package owasp_benchmark;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.core.Is.*;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import rasp_benchmark.*;
import rasp_benchmark.utils.*;
import rasp_benchmark.utils.RASPTestUtilities.SqliDirNumOutOfRangeException;

@RunWith(JUnitParamsRunner.class)
public class OwaspBenchmarkTests extends TestFramework
{
    static PollableProcess raspProcess;
    private static Collection<Object[]> testParams;
    private static List<String> inputFileEntries = null;
    private static List<String> benchmarkServlets = null;
    private static Set<Entry<Integer, String>> servletIntToSqliDirNumMap = null;

    private static final Path OWASP_BENCHMARK_TEST_DIR = Paths.get(BENCHMARK_AUTOMATION_DIR.toString(), "owasp_benchmark");
    private static final Path OWASP_BENCHMARK_INPUT_FILE_DIR = Paths.get(OWASP_BENCHMARK_TEST_DIR.toString(), "input_files");
    private static final Path OWASP_BENCHMARK_OUTPUT_FILE_DIR = Paths.get(OWASP_BENCHMARK_TEST_DIR.toString(), "output_files");
    private static final Path OWASP_BENCHMARK_BYTEMAN_DIR = Paths.get(OWASP_BENCHMARK_TEST_DIR.toString(), "byteman");
    private static final Path OWASP_BENCHMARK_SERVLETS = Paths.get(OWASP_BENCHMARK_INPUT_FILE_DIR.toString(), "servlets.txt");
    private static final Path OWASP_BENCHMARK_RAW_OUTPUT_FILE = Paths.get(OWASP_BENCHMARK_OUTPUT_FILE_DIR.toString(), "outputRASPBenchmark.log");
    private static final Path GLOBAL_BYTEMAN_RULES_FILE = Paths.get(OWASP_BENCHMARK_BYTEMAN_DIR.toString(), "bytemanRASPRules.btm");

    private static final String MAVEN = "maven";
    private static final String BENCHMARK_STARTUP_EXPECTED_OUTPUT = "INFO: Server startup in";
    private static final String BENCHMARK_WEBAPP_URL_BASE = "http://localhost:8080/benchmark/";
    private static final String BENCHMARK_WEBAPP_URL_SQLI = "sqli-0";
    private static final String BYTEMAN_RULES_HELPER_CLASS = "HELPER owasp_benchmark.BenchmarkResultRecorder";
    private static final String BYTEMAN_RULES_HELPER_CLASS_OUTPUT = "-------- BYTEMAN";

    private static final int SUCCESSFUL_HTTP_RESPONSE_CODE = 200;

    public OwaspBenchmarkTests() throws IOException
    {
        setUpParams();
    }

    private static void setUpParams() throws IOException
    {
        benchmarkServlets = FileUtils.readLines(new File(OWASP_BENCHMARK_SERVLETS.toString()), "UTF-8");
        testParams = new ArrayList<Object[]>();

        for(String servlet : benchmarkServlets)
        {
            Path servletInputFile = Paths.get(OWASP_BENCHMARK_INPUT_FILE_DIR.toString(), servlet);
            inputFileEntries = FileUtils.readLines(new File(servletInputFile.toString()), "UTF-8");

            for(String entry : inputFileEntries)
            {
                OwaspBenchmarkInput owaspBenchmarkInput = new OwaspBenchmarkInput(entry, servletInputFile);
                testParams.add(new Object[] {owaspBenchmarkInput});
            }
        }
    }

    public Collection<Object[]> parametersForTest()
    {
        return testParams;
    }

    private static void cleanup() throws IOException, InterruptedException
    {
        SystemUtils.killNamedProcess(MAVEN);
        new File(GLOBAL_BYTEMAN_RULES_FILE.toString()).delete();
    }

    @BeforeClass
    public static void setUpClass() throws Exception
    {
        cleanup();
        new File(OWASP_BENCHMARK_RAW_OUTPUT_FILE.toString()).delete();
        new File(BENCHMARK_REPO_UNZIP_DIR.toString() + OWASP_BENCHMARK_ZIP_FILE).delete();
        RASPTestUtilities.removeRepoIfExists(BENCHMARK_REPO);
        RASPTestUtilities.downloadAndSetupBenchmarkRepo();
        populateGlobalBytemanRulesFile();
        servletIntToSqliDirNumMap = getServletIntToSqliDirNumMapping();
        launchBenchmarkWithRasp();
    }

    @AfterClass
    public static void tearDownClass() throws Exception
    {
        cleanup();
    }

    @Test
    @Parameters
    @TestCaseName("{method}_{0}")
    public void test(OwaspBenchmarkInput owaspBenchmarkInput) throws Exception
    {
        int bytemanPrintToConsoleTimeout = 60;

        String currentTestcaseName = "test_" + owaspBenchmarkInput.toString();
        System.out.println("TestcaseName: " + currentTestcaseName);

        String servletName = owaspBenchmarkInput.getServletName();
        String inputPayload = owaspBenchmarkInput.getPayload();
        String expectedResult = owaspBenchmarkInput.getExpectedResult();
        String injectionType = owaspBenchmarkInput.getInjectionType();
        String sqliDirNum = getSqliDirNum(servletName);
        String servletUrl = BENCHMARK_WEBAPP_URL_BASE + BENCHMARK_WEBAPP_URL_SQLI + sqliDirNum + "/" + servletName;

        CloseableHttpClient httpClient = HttpClients.createDefault();
        verifyBenchmarkIsRunning(BENCHMARK_WEBAPP_URL_BASE, httpClient);
        logInputParamsForTest(servletName, inputPayload, expectedResult, injectionType);

        HttpPost postRequest = null;
        switch(injectionType)
        {
            case "paramName":
                postRequest = injectPayloadAsParamName(servletUrl, inputPayload, servletName);
                break;
            case "paramValue":
                postRequest = injectPayloadAsParamValue(servletUrl, inputPayload, servletName);
                break;
        }

        executePostRequest(httpClient, postRequest);
        raspProcess.waitForOutput(BYTEMAN_RULES_HELPER_CLASS_OUTPUT, bytemanPrintToConsoleTimeout);
    }

    private static void launchBenchmarkWithRasp() throws Exception
    {
        int benchmarkStartupTimeout = 900;
        Map<String, String> envVariables = new HashMap<String, String>();
        envVariables.put("PWD", BENCHMARK_REPO.toString());
        envVariables.put("BENCHMARK_JDK", BENCHMARK_JDK);
        envVariables.put("RASP_ARGS", RASP_ARGS);

        raspProcess = PollableProcess.build(RUN_BENCHMARK_RASP_SCRIPT_DEST.toString(), envVariables);
        await().atMost(benchmarkStartupTimeout, SECONDS).until(raspProcess.output(), containsString(BENCHMARK_STARTUP_EXPECTED_OUTPUT));
    }

    private HttpPost injectPayloadAsParamName(String url, String payload, String servletName) throws IOException, UnsupportedEncodingException
    {
        HttpPost httpPost = injectParamNameValuePair(url, payload, servletName);
        return httpPost;
    }

    private HttpPost injectPayloadAsParamValue(String url, String payload, String servletName) throws IOException, UnsupportedEncodingException
    {
        HttpPost httpPost = injectParamNameValuePair(url, servletName, payload);
        return httpPost;
    }

    private HttpPost injectParamNameValuePair(String url, String paramName, String paramValue) throws IOException, UnsupportedEncodingException
    {
        HttpPost httpPost = new HttpPost(url);
        ArrayList<NameValuePair> postParameters = new ArrayList<NameValuePair>();
        postParameters.add(new BasicNameValuePair(paramName, paramValue));
        log("Post parameter name:value pair equals: " + postParameters);
        httpPost.setEntity(new UrlEncodedFormEntity(postParameters));
        log("httpPost equals: " + httpPost);
        return httpPost;
    }

    private void executePostRequest(CloseableHttpClient httpClient, HttpPost postRequest) throws IOException
    {
        CloseableHttpResponse postRequestResponse = httpClient.execute(postRequest);
        getResponseCode(postRequestResponse);
        printResponseEntity(postRequestResponse);
        postRequestResponse.close();
    }

    private void verifyBenchmarkIsRunning(String benchmarkUrl, CloseableHttpClient httpClient) throws IOException
    {
        log("Verifying Benchmark webapp is up and running...");
        HttpGet verifyHttpGet = new HttpGet(benchmarkUrl);
        HttpResponse response = httpClient.execute(verifyHttpGet);
        int currentHttpResponseCode = getResponseCode(response);
        assertThat("Get request failed on Benchmark repo home page.", currentHttpResponseCode, is(SUCCESSFUL_HTTP_RESPONSE_CODE));
    }

    private int getResponseCode(HttpResponse response) throws IOException
    {
        int httpResponseCode = response.getStatusLine().getStatusCode();
        log("HTTP response code is: " + httpResponseCode);
        return httpResponseCode;
    }

    private void logInputParamsForTest(String servletName, String inputPayload, String expectedResult, String injectionType)
    {
        log("servletName: " + servletName);
        log("inputPayload: " + inputPayload);
        log("expectedResult: " + expectedResult);
        log("injectionType: " + injectionType);
    }

    private void printResponseEntity(HttpResponse response) throws IOException
    {
        String responseEntity = EntityUtils.toString(response.getEntity());
        log("Response entity: " + LINE_SEPARATOR + responseEntity);
    }

    private String getSqliDirNum(String servletName) throws IOException, SqliDirNumOutOfRangeException
    {
        String sqlUrlNumber = "";
        String servletNumberAsString = servletName.split("Test")[1];
        String servletNumberAsStringNoZeros = servletNumberAsString.replaceFirst("^0+(?!$)", "");
        int servletInt = Integer.parseInt(servletNumberAsStringNoZeros);

        for(Entry<Integer, String> servletToSqli : servletIntToSqliDirNumMap)
        {
            int servletIntBoundary = servletToSqli.getKey();
            String sqliUrlId = servletToSqli.getValue();

            if(servletInt <= servletIntBoundary)
            {
                sqlUrlNumber = sqliUrlId;
                return sqlUrlNumber;
            }
        }

        if(sqlUrlNumber.isEmpty())
        {
            throw new SqliDirNumOutOfRangeException("SQLi directory number not in range.");
        }
        return sqlUrlNumber;
    }

    private static Set<Entry<Integer, String>> getServletIntToSqliDirNumMapping() throws IOException, SqliDirNumOutOfRangeException
    {
        TreeMap<Integer, String> servletIntToSqliDirNum = new TreeMap<Integer, String>();

        int benchmark00510 = 510;
        int benchmark00935 = 935;
        int benchmark01389 = 1389;
        int benchmark01813 = 1813;
        int benchmark02275 = 2275;
        int benchmark02647 = 2647;
        int benchmark02740 = 2740;

        servletIntToSqliDirNum.put(benchmark00510, "0");
        servletIntToSqliDirNum.put(benchmark00935, "1");
        servletIntToSqliDirNum.put(benchmark01389, "2");
        servletIntToSqliDirNum.put(benchmark01813, "3");
        servletIntToSqliDirNum.put(benchmark02275, "4");
        servletIntToSqliDirNum.put(benchmark02647, "5");
        servletIntToSqliDirNum.put(benchmark02740, "6");

        return servletIntToSqliDirNum.entrySet();
    }

    /*
     * We launch the app-server just once for testsuite execution. Only
     * one Byteman rules file can be passed in. So we must concatenate
     * the individual Byteman rules files into a single file and pass this
     * file to the app-server launch command.
     */
    private static void populateGlobalBytemanRulesFile() throws IOException
    {
        BufferedWriter outGlobal = new BufferedWriter(new FileWriter(GLOBAL_BYTEMAN_RULES_FILE.toString(), true));
        outGlobal.write(BYTEMAN_RULES_HELPER_CLASS);
        outGlobal.newLine();
        outGlobal.newLine();
        outGlobal.close();

        benchmarkServlets = FileUtils.readLines(new File(OWASP_BENCHMARK_SERVLETS.toString()), "UTF-8");

        for(String servlet : benchmarkServlets)
        {
            String servletBytemanRulesFile = OWASP_BENCHMARK_BYTEMAN_DIR.toString() + FILE_SEPARATOR + servlet.replace("txt", "btm");
            File fin = new File(servletBytemanRulesFile);
            BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(fin)));
            BufferedWriter out = new BufferedWriter(new FileWriter(GLOBAL_BYTEMAN_RULES_FILE.toString(), true));

            String aLine = null;
            while((aLine = in.readLine()) != null)
            {
                out.write(aLine);
                out.newLine();
            }
            out.newLine();
            in.close();
            out.close();
        }
    }
}

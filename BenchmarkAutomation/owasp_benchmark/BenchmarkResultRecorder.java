package owasp_benchmark;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jboss.byteman.rule.Rule;
import org.jboss.byteman.rule.helper.Helper;

public class BenchmarkResultRecorder extends Helper
{

    private PrintStream rawDataOutput;

    private static final String USER_DIR = System.getProperty("user.dir");
    private static final Path RASP_BENCHMARK_REPO_BASE_DIR = Paths.get(USER_DIR, "..", "..", "..", "..", "..", "rasp-benchmark");
    private static final Path OUT_FILES_DIR = Paths.get(RASP_BENCHMARK_REPO_BASE_DIR.toString(), "BenchmarkAutomation", "owasp_benchmark", "output_files");
    private static final Path FILE_PATH = Paths.get(OUT_FILES_DIR.toString(), "outputRASPBenchmark.log");

    protected BenchmarkResultRecorder(Rule rule) throws IOException
    {
        super(rule);

        rawDataOutput = new PrintStream(new FileOutputStream(FILE_PATH.toString(), true));
    }

    public void payloadExecuted(String servletName, String payload)
    {
        System.out.println("-------- BYTEMAN : EXECUTED --------");
        rawDataOutput.println(servletName + "<split>" + payload + "<split>executed");
    }

    public void payloadNotExecuted(String servletName, String payload)
    {
        System.out.println("-------- BYTEMAN : NOT EXECUTED --------");
        rawDataOutput.println(servletName + "<split>" + payload + "<split>NOTexecuted");
    }
}

package rasp_benchmark;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang3.SystemUtils;

public abstract class TestFramework
{
    static String scriptDir = System.getProperty("user.dir");
    protected static final boolean OS_IS_WINDOWS = SystemUtils.IS_OS_WINDOWS;
    protected static final String SCRIPT_EXTENSION = OS_IS_WINDOWS ? ".bat" : ".sh";
    protected static final String LINE_SEPARATOR = System.lineSeparator();
    protected static final String FILE_SEPARATOR = File.separator;
    protected static final String OWASP_BENCHMARK_GITHUB_REPO_SHA = "a7f8ae663dd39ca0935e9a88ce8879ed56547c2f";
    protected static final String OWASP_BENCHMARK_GITHUB_REPO_URL = "https://github.com/OWASP/Benchmark/archive/" + OWASP_BENCHMARK_GITHUB_REPO_SHA + ".zip";
    protected static final String REPO_BASE_DIR = scriptDir.split("BenchmarkAutomation")[0];
    protected static final String POM_XML = "pom.xml";
    protected static final String RUN_BENCHMARK_RASP_SCRIPT = "runBenchmarkRASP" + SCRIPT_EXTENSION;
    protected static final String SERVER_RASP_XML = "serverRASP.xml";
    protected static final String OWASP_BENCHMARK_ZIP_FILE = "benchmark.zip";
    protected static final String BENCHMARK_JDK = System.getProperty("benchmark.jdk");
    protected static final String RASP_ARGS = System.getProperty("rasp.args");

    protected static final Path BENCHMARK_AUTOMATION_DIR = Paths.get(REPO_BASE_DIR, "BenchmarkAutomation");
    protected static final Path BENCHMARK_REPO_UNZIP_DIR = Paths.get(BENCHMARK_AUTOMATION_DIR.toString(), "..", "..");
    protected static final Path BENCHMARK_REPO = Paths.get(BENCHMARK_REPO_UNZIP_DIR.toString(), "Benchmark-" + OWASP_BENCHMARK_GITHUB_REPO_SHA);
    protected static final Path TEMP_BENCHMARK_REPO_RASP_FILES = Paths.get(BENCHMARK_AUTOMATION_DIR.toString(), "benchmarkRASPFiles");
    protected static final Path RUN_BENCHMARK_RASP_SCRIPT_SRC = Paths.get(TEMP_BENCHMARK_REPO_RASP_FILES.toString(), RUN_BENCHMARK_RASP_SCRIPT);
    protected static final Path RUN_BENCHMARK_RASP_SCRIPT_DEST = Paths.get(BENCHMARK_REPO.toString(), RUN_BENCHMARK_RASP_SCRIPT);
    protected static final Path POM_XML_WITH_DEPLOY_RASP_SRC = Paths.get(TEMP_BENCHMARK_REPO_RASP_FILES.toString(), POM_XML);
    protected static final Path POM_XML_WITH_DEPLOY_RASP_DEST = Paths.get(BENCHMARK_REPO.toString(), POM_XML);
    protected static final Path SERVER_RASP_XML_SRC = Paths.get(TEMP_BENCHMARK_REPO_RASP_FILES.toString(), SERVER_RASP_XML);
    protected static final Path SERVER_RASP_XML_DEST = Paths.get(BENCHMARK_REPO.toString(), "src", "config", "local", SERVER_RASP_XML);

    public static void log(String message)
    {
        DateFormat dateFormat = new SimpleDateFormat("[HH:mm:ss.S]");
        Date date = new Date();
        System.out.println(dateFormat.format(date) + " " + message);
    }
}

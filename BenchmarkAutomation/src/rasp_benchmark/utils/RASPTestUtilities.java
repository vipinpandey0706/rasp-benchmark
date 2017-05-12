package rasp_benchmark.utils;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;

import org.apache.commons.io.FileUtils;

import rasp_benchmark.TestFramework;

public final class RASPTestUtilities extends TestFramework
{
    private RASPTestUtilities()
    {}

    public static void downloadAndSetupBenchmarkRepo() throws IOException
    {
        downloadFromUrl("OWASP Benchmark repo", OWASP_BENCHMARK_GITHUB_REPO_URL, OWASP_BENCHMARK_ZIP_FILE);
        unzipFile(OWASP_BENCHMARK_ZIP_FILE, BENCHMARK_REPO_UNZIP_DIR.toString());
        copyTempRASPFilesToBenchmarkRepo();
        grantFileExecPermissions(RUN_BENCHMARK_RASP_SCRIPT_DEST.toString());
    }

    public static void removeRepoIfExists(Path repo) throws IOException
    {
        if(new File(repo.toString()).exists())
        {
            log("Removing the following repo: " + repo);
            FileUtils.deleteDirectory(new File(repo.toString()));
        }
    }

    public static void downloadFromUrl(String name, String repoUrl, String zipFileName) throws IOException
    {
        log("Downloading " + name);
        URL benchmarkGitHub = new URL(repoUrl);
        ReadableByteChannel rbc = Channels.newChannel(benchmarkGitHub.openStream());
        FileOutputStream fos = new FileOutputStream(zipFileName);
        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
        fos.close();
    }

    public static void unzipFile(String zipFileName, String unzipTargetDir) throws IOException
    {
        log("Unzipping " + zipFileName);
        PollableProcess unzipProcess = PollableProcess.build("unzip", "-u " + zipFileName + " -d " + unzipTargetDir);
        await().atMost(60, SECONDS).until(unzipProcess.hasExited());
    }

    public static void copyTempRASPFilesToBenchmarkRepo() throws IOException
    {
        String[][] raspFiles = new String[][] { {POM_XML, POM_XML_WITH_DEPLOY_RASP_SRC.toString(), POM_XML_WITH_DEPLOY_RASP_DEST.toString()},
                                                {RUN_BENCHMARK_RASP_SCRIPT, RUN_BENCHMARK_RASP_SCRIPT_SRC.toString(), RUN_BENCHMARK_RASP_SCRIPT_DEST.toString()},
                                                {SERVER_RASP_XML, SERVER_RASP_XML_SRC.toString(), SERVER_RASP_XML_DEST.toString()}};
        for(String[] raspFileEntry : raspFiles)
        {
            copyRASPFileToBenchmarkRepo(raspFileEntry[0], raspFileEntry[1], raspFileEntry[2]);
        }
    }

    public static void copyRASPFileToBenchmarkRepo(String fileName, String fileSrc, String fileDest) throws IOException
    {
        int checkFileExistsTimeout = 60;
        log("Copying RASP file '" + fileName + "' to Benchmark repo.");
        File source = new File(fileSrc);
        File destination = new File(fileDest);
        FileUtils.copyFile(source, destination);
        FileUtils.waitFor(destination, checkFileExistsTimeout);
    }

    public static void grantFileExecPermissions(String fileName) throws IOException
    {
        Runtime.getRuntime().exec("chmod +x " + fileName);
    }

    public static class UnexpectedJavaHomeException extends Exception
    {
        public UnexpectedJavaHomeException(String msg)
        {
            super(msg);
        }
    }

    public static class SqliDirNumOutOfRangeException extends Exception
    {
        public SqliDirNumOutOfRangeException(String msg)
        {
            super(msg);
        }
    }
}

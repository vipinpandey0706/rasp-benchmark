package rasp_benchmark.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.apache.commons.lang3.ArrayUtils;

import rasp_benchmark.TestFramework;

public final class SystemUtils extends TestFramework
{
    private SystemUtils()
    {}

    public static int executeShellCommand(String[] cmdAndArgs) throws java.io.IOException, java.lang.InterruptedException
    {
        if(OS_IS_WINDOWS)
        {
            String[] cmdPrefixArray = {"cmd.exe", "/C"};
            cmdAndArgs = ArrayUtils.addAll(cmdPrefixArray, cmdAndArgs);
        }

        Process cmdProc = new ProcessBuilder(cmdAndArgs).start();
        BufferedReader stdoutReader = new BufferedReader(new InputStreamReader(cmdProc.getInputStream()));
        String line;
        while((line = stdoutReader.readLine()) != null)
        {
            log(line);
        }

        BufferedReader stderrReader = new BufferedReader(new InputStreamReader(cmdProc.getErrorStream()));
        while((line = stderrReader.readLine()) != null)
        {
            log(line);
        }

        stdoutReader.close();
        stderrReader.close();

        cmdProc.waitFor();
        cmdProc.destroy();
        cmdProc.waitFor();
        int returnCode = cmdProc.exitValue();
        return returnCode;
    }

    public static int killNamedProcess(String processName) throws java.io.IOException, java.lang.InterruptedException
    {
        String cmdAndArgs = "";

        if(OS_IS_WINDOWS)
        {
            cmdAndArgs = "taskkill /F /IM " + processName;
        }
        else
        {
            cmdAndArgs = "pkill -f " + processName;
        }

        int returnCode = executeShellCommand(cmdAndArgs.split(" "));
        return returnCode;
    }
}

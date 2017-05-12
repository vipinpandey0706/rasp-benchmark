package rasp_benchmark.utils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Map;
import java.util.concurrent.Callable;

import static com.jayway.awaitility.Awaitility.await;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.containsString;

public class PollableProcess
{
    private final Process process;
    private final StringBuilder bufferedOutput = new StringBuilder();

    public PollableProcess(ProcessBuilder processBuilder) throws IOException
    {
        process = processBuilder.start();
    }

    private String outputString() throws IOException
    {
        int previousOutputLength = bufferedOutput.length();
        InputStreamReader inputStreamReader = new InputStreamReader(process.getInputStream());
        char[] buffer = new char[1024];
        while(inputStreamReader.ready())
        {
            int readCount = inputStreamReader.read(buffer);
            bufferedOutput.append(buffer, 0, readCount);
        }

        inputStreamReader = new InputStreamReader(process.getErrorStream());
        while(inputStreamReader.ready())
        {
            int readCount = inputStreamReader.read(buffer);
            bufferedOutput.append(buffer, 0, readCount);
        }
        String output = bufferedOutput.toString();
        String newOutput = output.substring(previousOutputLength);
        System.out.print(newOutput);
        return output;
    }

    public void sendInput(String input) throws IOException
    {
        OutputStreamWriter outputStreamWriter = new OutputStreamWriter(process.getOutputStream());
        outputStreamWriter.write(input);
        System.out.print("Sending input: " + input + "\n");
        outputStreamWriter.flush();
        outputStreamWriter.close();
    }

    public static PollableProcess build(String cmd, String args, Map<String, String> env, SetDir setDir) throws IOException
    {
        PollableProcessBuilder builder = new PollableProcessBuilder(cmd, args);
        if(env != null)
        {
            builder.setEnv(env);
            builder.setDir(setDir);
        }
        return builder.build();
    }

    public static PollableProcess build(String cmd, String args, Map<String, String> env) throws IOException
    {
        return build(cmd, args, env, SetDir.YES);
    }

    public static PollableProcess build(String cmd, String args) throws IOException
    {
        return build(cmd, args, null);
    }

    public static PollableProcess build(String cmd, Map<String, String> env, SetDir setDir) throws IOException
    {
        return build(cmd, "", env, setDir);
    }

    public static PollableProcess build(String cmd, Map<String, String> env) throws IOException
    {
        return build(cmd, "", env);
    }

    public static PollableProcess build(String cmd) throws IOException
    {
        return build(cmd, "");
    }

    public Callable<String> output()
    {
        return new Callable<String>()
        {
            @Override
            public String call() throws Exception
            {
                return outputString();
            }
        };
    }

    public void stop() throws InterruptedException
    {
        process.destroy();
        process.waitFor();
    }

    public Callable<Boolean> hasExited()
    {
        return new Callable<Boolean>()
        {
            @Override
            public Boolean call() throws Exception
            {
                try
                {
                    process.exitValue();
                    return true;
                }
                catch(IllegalThreadStateException e)
                {
                    outputString();
                    return false;
                }
            }
        };
    }

    public int exitValue()
    {
        return process.exitValue();
    }

    public void waitForOutput(String expectedOutput, int limitInSeconds)
    {
        await().atMost(limitInSeconds, SECONDS).until(output(), containsString(expectedOutput));
    }
}

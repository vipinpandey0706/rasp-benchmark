package rasp_benchmark.utils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;

public class PollableProcessBuilder
{
    private final String[] args;
    private SetDir setDir = SetDir.YES;
    private File dir;
    private Map<String, String> env = new HashMap<String, String>();

    public PollableProcessBuilder(String cmd, String args)
    {
        if(args.isEmpty())
        {
            this.args = new String[] {cmd};
        }
        else
        {
            this.args = ArrayUtils.add(args.split("\\s+"), 0, cmd);
        }
    }

    public PollableProcessBuilder(String cmd, String args, Map<String, String> env)
    {
        this(cmd, args);
        setEnv(env);
    }

    public void setEnv(Map<String, String> env)
    {
        this.env = env;
    }

    public void setDir(SetDir setDir)
    {
        this.setDir = setDir;
    }

    private void setDirFromEnv()
    {
        String path = env.get("PWD");
        if(path != null)
        {
            dir = new File(path);
        }
    }

    public PollableProcess build() throws IOException
    {
        ProcessBuilder processBuilder = new ProcessBuilder(args);
        processBuilder.environment().putAll(env);
        if(setDir == SetDir.YES)
        {
            setDirFromEnv();
        }
        processBuilder.directory(dir);
        return new PollableProcess(processBuilder);
    }
}

package rasp_benchmark;

import java.io.UnsupportedEncodingException;
import java.nio.file.Path;

import org.apache.commons.io.FilenameUtils;

public class OwaspBenchmarkInput
{
    private String payload;
    private String expectedResult;
    private String servletName;
    private String injectionType;

    public OwaspBenchmarkInput(String entry, Path inputFile) throws UnsupportedEncodingException
    {
        String delimiter = "<split>";
        String[] parts = entry.split(delimiter);
        this.payload = parts[0];
        this.expectedResult = parts[1];
        this.injectionType = parts[2];
        this.servletName = FilenameUtils.removeExtension(inputFile.getFileName().toString());
    }

    public String getPayload()
    {
        return payload;
    }

    public String getExpectedResult()
    {
        return expectedResult;
    }

    public String getServletName()
    {
        return servletName;
    }

    public String getInjectionType()
    {
        return injectionType;
    }

    @Override
    public String toString()
    {
        return servletName + "_" + payload.replace(" ", "_") + "_" + expectedResult;
    }
}

package jp.igapyon.mikutextbundle.model;

import java.util.List;

public class CliOptions {
    public String inputDirectory;
    public String outputDirectory;
    public int maxChars = 120000;
    public int maxInputFileBytes = 1000000;
    public EncodingOptions encoding = new EncodingOptions();
    public List<String> excludeExtensions;
    public List<String> excludeDirectories;
    public boolean verbose;
}

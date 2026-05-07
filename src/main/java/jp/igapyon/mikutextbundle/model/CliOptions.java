package jp.igapyon.mikutextbundle.model;

import java.util.ArrayList;
import java.util.List;

public class CliOptions {
    public String inputDirectory;
    public String outputDirectory;
    public int maxChars = 120000;
    public int maxInputFileBytes = 1000000;
    public EncodingOptions encoding = new EncodingOptions();
    public List<String> includePatterns = new ArrayList<String>();
    public List<String> excludePatterns = new ArrayList<String>();
    public boolean verbose;
}

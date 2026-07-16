package jp.igapyon.mikutextbundle.coreapi;

import java.util.ArrayList;
import java.util.List;

import jp.igapyon.mikutextbundle.model.BundleMode;

public class BundleResult {
    public BundleMode mode;
    public String outputDirectory;
    public String indexPath;
    public String promptPath;
    public List<String> partPaths = new ArrayList<String>();
    public List<String> knowledgeSourcePaths = new ArrayList<String>();
    public String managementIndexPath;
    public int filesCollected;
    public int filesSkipped;
    public int directoriesIgnored;
    public int filesIgnored;
    public int ignoredByDirectory;
    public int ignoredByExtension;
    public int ignoredByGitignore;
    public int ignoredByOutputDirectory;
    public int partsGenerated;
    public List<String> warnings = new ArrayList<String>();
    public boolean dryRun;
}

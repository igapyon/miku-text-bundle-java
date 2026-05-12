package jp.igapyon.mikutextbundle.discovery;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import jp.igapyon.mikutextbundle.model.IgnoreStats;

public class DiscoveryResult {
    public final List<Path> files = new ArrayList<Path>();
    public final IgnoreStats ignored = new IgnoreStats();
}

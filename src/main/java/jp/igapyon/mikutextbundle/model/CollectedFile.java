package jp.igapyon.mikutextbundle.model;

import java.util.ArrayList;
import java.util.List;

public class CollectedFile {
    public String absolutePath;
    public String relativePath;
    public String extension;
    public String content;
    public int charCount;
    public int lineCount;
    public List<Marker> markers = new ArrayList<Marker>();
}

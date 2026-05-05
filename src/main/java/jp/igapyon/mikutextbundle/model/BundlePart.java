package jp.igapyon.mikutextbundle.model;

import java.util.ArrayList;
import java.util.List;

public class BundlePart {
    public String fileName;
    public int partNumber;
    public List<BundleChunk> chunks = new ArrayList<BundleChunk>();
    public int charCount;
}

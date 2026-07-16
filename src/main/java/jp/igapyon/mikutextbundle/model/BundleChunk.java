package jp.igapyon.mikutextbundle.model;

public class BundleChunk {
    public String relativePath;
    public String extension;
    public String content;
    public int originalCharCount;
    public int originalLineCount;
    public int chunkIndex;
    public int chunkCount;
    public int sourceStartLine;
    public int sourceEndLine;
    public int sourceStartChar;
    public int sourceEndChar;
    public String splitReason;
}

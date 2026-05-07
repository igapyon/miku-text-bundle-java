package jp.igapyon.mikutextbundle.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class EncodingOptions {
    public SupportedEncoding defaultEncoding = SupportedEncoding.UTF_8;
    public Map<String, SupportedEncoding> extensions = new LinkedHashMap<String, SupportedEncoding>();
}

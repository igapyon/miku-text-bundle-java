package jp.igapyon.mikutextbundle.model;

public enum SupportedEncoding {
    UTF_8("utf-8", "UTF-8", "UTF-8"),
    SHIFT_JIS("shift_jis", "Shift_JIS", "Shift_JIS");

    public final String optionValue;
    public final String displayName;
    public final String charsetName;

    SupportedEncoding(String optionValue, String displayName, String charsetName) {
        this.optionValue = optionValue;
        this.displayName = displayName;
        this.charsetName = charsetName;
    }

    public static SupportedEncoding parse(String value, String optionName) {
        for (SupportedEncoding encoding : values()) {
            if (encoding.optionValue.equals(value)) {
                return encoding;
            }
        }
        throw new IllegalArgumentException(optionName + " must be one of: utf-8, shift_jis.");
    }
}

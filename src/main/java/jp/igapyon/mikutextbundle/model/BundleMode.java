package jp.igapyon.mikutextbundle.model;

public enum BundleMode {
    HANDOFF("handoff"),
    KNOWLEDGE_SOURCE("knowledge-source");

    public final String cliValue;

    BundleMode(String cliValue) {
        this.cliValue = cliValue;
    }

    public static BundleMode parse(String value) {
        for (BundleMode mode : values()) {
            if (mode.cliValue.equals(value)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("--mode must be one of: handoff, knowledge-source.");
    }
}

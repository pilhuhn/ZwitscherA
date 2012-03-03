package de.bsd.zwitscher.helper;

/**
 * Helper that just holds the source url and the
 * respective target e.g. after url expansion (from t.co to ... )
 */
public class UrlPair {
    private String src;
    private String target;

    public UrlPair(String src, String target) {
        this.src = src;
        this.target = target;
    }

    public String getSrc() {
        return src;
    }

    public String getTarget() {
        return target;
    }

    @Override
    public String toString() {
        return "UrlPair{" +
                "src='" + src + '\'' +
                ", target='" + target + '\'' +
                '}';
    }
}
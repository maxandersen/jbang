package dev.jbang.poor;

public class PoorStyle {

    String styleName;

    /**
     * Normalize a style definition so that styles with the same effect have the same string
     *         representation.
     * @return normal form of a style definition
     */
    static String normalize(String style) {
        return style.strip().toLowerCase();
    }
}

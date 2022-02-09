package dev.jbang.poor;

import java.util.List;

public class PoorText {
    private StringBuffer text;
    private List<Span> spans;

    public PoorText(String markup) {
        this.text = new StringBuffer(markup.toString());
    }

    public PoorText() {

    }

    public List<Span> getSpans() {
        return spans;
    }

    @Override
    public String toString() {
        return text.toString();
    }

    public PoorText append(String extratext) {
        text.append(extratext);
        return this;
    }
}

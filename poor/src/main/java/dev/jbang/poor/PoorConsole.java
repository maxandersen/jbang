package dev.jbang.poor;

import java.util.Arrays;
import java.util.stream.Collectors;

public class PoorConsole {
    private final int width;
    private final ColorSystem colorSystem;
    private StringBuffer capture;

    public PoorConsole(Builder builder) {
        this.width=builder.width;
        this.colorSystem=builder.colorSystem;
    }

    public void beginCapture() {
        capture = new StringBuffer();
    }

    public void print(Object... objects) {
        if(capture!=null) {
           capture.append(Arrays.stream(objects)
                   .map(Object::toString)
                   .map(x -> x.replace("\\[", "[")
                                .replace("\\\\", "\\\\"))
                        .collect(Collectors.joining(" ")));
        }
    }

    public String endCapture() {
        String oldcapture= String.valueOf(capture);
        capture=null;
        return oldcapture;
    }

    static class Builder {

        private int width;
        private ColorSystem colorSystem;

        public Builder width(int i) {
            this.width=i;
            return this;
        }

        public Builder colorSystem(ColorSystem system) {
            this.colorSystem=system;
            return this;
        }

        public PoorConsole build() {
            return new PoorConsole(this);
        }
    }
    public static Builder builder() {
        return new Builder();
    }
}

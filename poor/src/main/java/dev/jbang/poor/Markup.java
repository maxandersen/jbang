package dev.jbang.poor;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class Markup {

    static Pattern RE_TAGS = Pattern.compile("((\\\\*)\\[([a-z#\\/@].*?)\\])");

    static Pattern RE_ESCAPE = Pattern.compile("(\\\\*)(\\[([a-z#\\/@].*?)\\])");
    /**
     * Escapes text so that it won't be interpreted as markup.
     *
     * @param markup Content to be inserted in to markup
     * @return Markup with square brackets escaped
     */
    public static String escape(String markup) {
        return RE_ESCAPE.matcher(markup).replaceAll((match -> {
            String backslashes = match.group(1);
            String text = match.group(2);
            return String.format("%s%s\\\\%s", backslashes, backslashes, text);
        }));
    }


    static public record Segment(int position, String text, Tag tag) {

        static Segment segment(int p, String tx, Tag t) {
            return new Segment(p,tx,t);
        }
    }

    /**
     * Parse markup in to a stream of Segments of (position, text, tag).
     *
     * @param markup
     * @return
     */
    public static Stream<Segment> _parse(String markup) {
        List<Segment> result = new ArrayList<>();
        int position = 0;
        var match = RE_TAGS.matcher(markup);
        while(match.find()) {
            String fullText = match.group(1);
            String escapes = match.group(2);
            String tagText = match.group(3);
            int start = match.start();
            int end = match.end();

            if(start>position) {
                result.add(new Segment(start, markup.substring(position,start), null));
            }
            if (escapes!=null) {
                int backslashes = escapes.length()/2;
                int escaped = escapes.length()%2;
                if(backslashes>0) {
                    result.add(new Segment(start, "\\".repeat(backslashes),null));
                    start += backslashes * 2;
                }
                if(escaped>0) {
                    result.add(new Segment(start, fullText.substring(escapes.length()), null));
                    position = end;
                    continue;
                }
            }
            var parts = tagText.split("=",2);
            var text = parts[0];
            var parameters = parts.length>1?parts[1]:null;
            result.add(new Segment(start,null, new Tag(text, parameters==null?null:new Parameters(parameters))));
            position = end;
        }
        if(position < markup.length()) {
            result.add(new Segment(position, markup.substring(position),null));
        }
        return result.stream();
    }

    /**
     * Render console markup in to a Text instance.
     *
     *     Args:
     *         markup (str): A string containing console markup.
     *         emoji (bool, optional): Also render emoji code. Defaults to True.
     *
     *     Raises:
     *         MarkupError: If there is a syntax error in the markup.
     *
     *     Returns:
     *         Text: A test instance.
     *
     * @param markup
     * @return
     */
    static PoorText render(String markup) {
        boolean emojiiReplace = true;

        if(!markup.contains("[")) {
            return new PoorText(markup);
        }
        var text = new PoorText();

        var styleStack = new ArrayList();

        var spans = new ArrayList<Span>();

        _parse(markup).forEach(s -> {
            if(s.text!=null) {
                text.append(s.text);
            } else if (s.tag!=null) {
                if(s.tag.name().startsWith("/")) { // Closing tag
                    String styleName = s.tag.name().substring(1).strip();

                    if(!styleName.isEmpty()) { // explicit close
                        styleName = PoorStyle.normalize(styleName);
                        

                    }
                }
            }
        });
        return new PoorText();
    }
}

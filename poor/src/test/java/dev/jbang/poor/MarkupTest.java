package dev.jbang.poor;

import dev.jbang.poor.Markup.Segment;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;


import java.util.Arrays;
import java.util.List;

import static dev.jbang.poor.Markup.Segment.segment;
import static dev.jbang.poor.Markup.escape;
import static junit.framework.TestCase.assertFalse;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MarkupTest {

    @Test
    public void test_re_no_match() {
        assertFalse(Markup.RE_TAGS.matcher("[True]").matches());
        assertFalse(Markup.RE_TAGS.matcher("[None]").matches());
        assertFalse(Markup.RE_TAGS.matcher("[1]").matches());
        assertFalse(Markup.RE_TAGS.matcher("[2]").matches());
        assertFalse(Markup.RE_TAGS.matcher("[1]").matches());
    }

    @Test
    public void test_re_match() {
        assertTrue(Markup.RE_TAGS.matcher("[true]").matches());
        assertTrue(Markup.RE_TAGS.matcher("[false]").matches());
        assertTrue(Markup.RE_TAGS.matcher("[none]").matches());
        assertTrue(Markup.RE_TAGS.matcher("[color(1)]").matches());
        assertTrue(Markup.RE_TAGS.matcher("[/]").matches());
        assertTrue(Markup.RE_TAGS.matcher("[@]").matches());
        assertTrue(Markup.RE_TAGS.matcher("[@foo]").matches());
        assertTrue(Markup.RE_TAGS.matcher("[@foo=bar]").matches());
    }

    @Test
    public void testEscape() {
        // Potential tags
        assertThat(escape("foo[bar]"), IsEqual.equalTo("foo\\[bar]"));
        assertThat(escape("foo\\[bar]"), IsEqual.equalTo("foo\\\\[bar]"));

        // Not tags (escape not required)
        assertThat(escape("[5]"), IsEqual.equalTo("[5]"));
        assertThat(escape("\\[5]"), IsEqual.equalTo("\\[5]"));

        // Test @ escape
        assertThat(escape("[@foo]"), IsEqual.equalTo("\\[@foo]"));
        assertThat(escape("[@]"), IsEqual.equalTo("\\[@]"));

        // Not tags (escape not required)
        assertThat(escape("[red]"), IsEqual.equalTo("\\[red]"));
    }

    @Test
    public void testRenderEscape() {
        PoorConsole console = PoorConsole.builder().width(80).colorSystem(null).build();
        console.beginCapture();
        console.print(escape("[red]"), escape("\\[red]"), escape("\\\\[red]"), escape("\\\\\\[red]"));
        String result = console.endCapture();
        MatcherAssert.assertThat(result, IsEqual.equalTo("[red] \\[red] \\\\[red] \\\\\\[red]"));
    }

    @Test
    public void testParse() {
        var result = Markup._parse("[foo]hello[/foo][bar]world[/]\\[escaped]").toList();
        var expected = List.of(
                segment(0, null, new Tag("foo", null)),
                segment(10, "hello", null),
                segment(10, null, new Tag("/foo",null)),
                segment(16, null, new Tag("bar",null)),
                segment(26, "world", null),
                segment(26, null, new Tag("/", null)),
                segment(29, "[escaped]", null));
        assertThat(result, contains(expected.toArray()));
    }

    @Test
    public void testParseLink() {
        var result = Markup._parse("[link=foo]bar[/link]").toList();
        var expected = List.of(
                segment(0, null, new Tag("link", new Parameters("foo"))),
                segment(13, "bar", null),
                segment(13, null, new Tag("/link",null))
                );
        assertThat(result, contains(expected.toArray()));
    }

    @Test
    public void testRender() {
        var result = Markup.render("[bold]FOO[/bold]");
        assertThat(result.toString(),equalTo("FOO"));
        assertThat(result.getSpans(),contains(new Span[] { new Span(0,3,"bold") }));

    }
}

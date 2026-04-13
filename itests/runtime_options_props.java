///usr/bin/env jbang "$0" "$@" ; exit $?
//RUNTIME_OPTIONS -Dtest.runtime.property=from-runtime-options
//RUNTIME_OPTIONS -Danother.prop=another-value

public class runtime_options_props {
    public static void main(String[] args) {
        System.out.println("Hello World");
    }
}

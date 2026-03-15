package sh.skills.providers;

import java.util.Arrays;
import java.util.List;

/**
 * Registry of all skill providers.
 * Mirrors src/providers/registry.ts.
 */
public class ProviderRegistry {

    private static final List<HostProvider> PROVIDERS = Arrays.asList(
        new GitHubProvider(),
        new GitLabProvider(),
        new GitProvider(),
        new LocalProvider(),
        new WellKnownProvider()
    );

    /**
     * Find the first provider that can handle the given source.
     * Returns null if no provider matches.
     */
    public static HostProvider findProvider(String source) {
        for (HostProvider provider : PROVIDERS) {
            if (provider.matches(source)) {
                return provider;
            }
        }
        return null;
    }

    public static List<HostProvider> getProviders() {
        return PROVIDERS;
    }
}

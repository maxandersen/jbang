package dev.jbang.catalog;

import static dev.jbang.util.TestUtil.clearSettingsCaches;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.jbang.BaseTest;

public class TestAliasPinning extends BaseTest {

	static final String testCatalog = "{\n" +
			"  \"aliases\": {\n" +
			"    \"myapp\": {\n" +
			"      \"script-ref\": \"org.example:myapp:1.0.0\"\n" +
			"    },\n" +
			"    \"ghapp\": {\n" +
			"      \"script-ref\": \"https://github.com/example/repo/blob/main/ghapp.java\"\n" +
			"    },\n" +
			"    \"rawapp\": {\n" +
			"      \"script-ref\": \"https://raw.githubusercontent.com/example/repo/main/rawapp.java\"\n" +
			"    },\n" +
			"    \"othapp\": {\n" +
			"      \"script-ref\": \"https://example.com/othapp.java\"\n" +
			"    }\n" +
			"  }\n" +
			"}";

	@BeforeEach
	void initCatalog() throws IOException {
		Path catsFile = jbangTempDir.resolve(Catalog.JBANG_CATALOG_JSON);
		Path testCatalogFile = cwdDir.resolve("test-catalog.json");
		Files.write(testCatalogFile, testCatalog.getBytes());
		clearSettingsCaches();
		CatalogUtil.addCatalogRef(catsFile, "testcat",
				testCatalogFile.toAbsolutePath().toString(), "Test catalog", null);
	}

	// --- parseVersion unit tests ---

	@Test
	void testParseVersionWithVersionAndCatalog() {
		assertThat(Alias.parseVersion("myapp:1.0.3@testcat"), is("1.0.3"));
	}

	@Test
	void testParseVersionWithoutVersion() {
		assertThat(Alias.parseVersion("myapp@testcat"), is(nullValue()));
	}

	@Test
	void testParseVersionWithoutCatalog() {
		// Version-only without @catalog is not supported (ambiguous with GAV G:A)
		assertThat(Alias.parseVersion("myapp:1.0.3"), is(nullValue()));
	}

	@Test
	void testParseVersionGavNotExtracted() {
		// G:A:V@type has two colons before @, should not be treated as alias:version
		assertThat(Alias.parseVersion("com.example:artifact:1.0.0@fatjar"), is(nullValue()));
	}

	@Test
	void testParseVersionUnqualifiedAlias() {
		assertThat(Alias.parseVersion("myapp"), is(nullValue()));
	}

	// --- stripVersion unit tests ---

	@Test
	void testStripVersionWithVersionAndCatalog() {
		assertThat(Alias.stripVersion("myapp:1.0.3@testcat"), is("myapp@testcat"));
	}

	@Test
	void testStripVersionWithoutVersion() {
		assertThat(Alias.stripVersion("myapp@testcat"), is("myapp@testcat"));
	}

	@Test
	void testStripVersionWithoutCatalog() {
		assertThat(Alias.stripVersion("myapp:1.0.3"), is("myapp:1.0.3"));
	}

	@Test
	void testStripVersionGavNotModified() {
		assertThat(Alias.stripVersion("com.example:artifact:1.0.0@fatjar"),
				is("com.example:artifact:1.0.0@fatjar"));
	}

	// --- applyVersion unit tests ---

	@Test
	void testApplyVersionMavenGav() {
		assertThat(Alias.applyVersion("org.example:myapp:1.0.0", "2.0.0"),
				is("org.example:myapp:2.0.0"));
	}

	@Test
	void testApplyVersionMavenGavPreservesClassifier() {
		assertThat(Alias.applyVersion("org.example:myapp:1.0.0:jdk8", "2.0.0"),
				is("org.example:myapp:2.0.0:jdk8"));
	}

	@Test
	void testApplyVersionMavenGavPreservesType() {
		assertThat(Alias.applyVersion("org.example:myapp:1.0.0@fatjar", "2.0.0"),
				is("org.example:myapp:2.0.0@fatjar"));
	}

	@Test
	void testApplyVersionGitHubBlobUrl() {
		assertThat(
				Alias.applyVersion("https://github.com/example/repo/blob/main/ghapp.java", "v2.0.0"),
				is("https://github.com/example/repo/blob/v2.0.0/ghapp.java"));
	}

	@Test
	void testApplyVersionGitHubBlobUrlWithSubPath() {
		assertThat(
				Alias.applyVersion("https://github.com/example/repo/blob/main/dir/ghapp.java", "0.0.3"),
				is("https://github.com/example/repo/blob/0.0.3/dir/ghapp.java"));
	}

	@Test
	void testApplyVersionRawGitHubUrl() {
		assertThat(
				Alias.applyVersion("https://raw.githubusercontent.com/example/repo/main/rawapp.java",
						"v1.2.3"),
				is("https://raw.githubusercontent.com/example/repo/v1.2.3/rawapp.java"));
	}

	@Test
	void testApplyVersionOtherUrlUnchanged() {
		String url = "https://example.com/othapp.java";
		assertThat(Alias.applyVersion(url, "1.0.0"), is(url));
	}

	// --- Alias.get integration tests with version pinning ---

	@Test
	void testGetAliasWithoutVersionIsUnchanged() {
		Alias alias = Alias.get("myapp@testcat");
		assertThat(alias, notNullValue());
		assertThat(alias.scriptRef, is("org.example:myapp:1.0.0"));
	}

	@Test
	void testGetAliasWithVersionPinningMaven() {
		Alias pinned = Alias.get("myapp:2.0.0@testcat");
		assertThat(pinned, notNullValue());
		assertThat(pinned.scriptRef, is("org.example:myapp:2.0.0"));
	}

	@Test
	void testGetAliasWithVersionPinningGitHub() {
		Alias pinned = Alias.get("ghapp:v2.0.0@testcat");
		assertThat(pinned, notNullValue());
		assertThat(pinned.scriptRef,
				is("https://github.com/example/repo/blob/v2.0.0/ghapp.java"));
	}

	@Test
	void testGetAliasWithVersionPinningRawGitHub() {
		Alias pinned = Alias.get("rawapp:v1.5.0@testcat");
		assertThat(pinned, notNullValue());
		assertThat(pinned.scriptRef,
				is("https://raw.githubusercontent.com/example/repo/v1.5.0/rawapp.java"));
	}

	@Test
	void testGetAliasWithVersionPinningOtherUrlUnchanged() {
		Alias pinned = Alias.get("othapp:1.0.0@testcat");
		assertThat(pinned, notNullValue());
		// Non-GitHub/non-GAV URLs are returned unchanged
		assertThat(pinned.scriptRef, is("https://example.com/othapp.java"));
	}
}

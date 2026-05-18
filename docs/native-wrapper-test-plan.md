# Native Wrapper Test Plan

This document outlines the recommended unit and integration tests for PR #8 (native-aware wrapper support).

## Unit Tests

### Test Class: `src/test/java/dev/jbang/cli/AppInstallTest.java`

```java
package dev.jbang.cli;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

import static org.junit.jupiter.api.Assertions.*;

class AppInstallTest {

	@Test
	void testGetNativeBundleOs_Mac() {
		// Mock System.getProperty("os.name") to return "Mac OS X"
		// Verify getNativeBundleOs() returns "mac"
		// Similar for "darwin"
	}

	@Test
	void testGetNativeBundleOs_Windows() {
		// Mock System.getProperty("os.name") to return "Windows 10"
		// Verify getNativeBundleOs() returns "windows"
	}

	@Test
	void testGetNativeBundleOs_Linux() {
		// Mock System.getProperty("os.name") to return "Linux"
		// Verify getNativeBundleOs() returns "linux"
	}

	@Test
	void testGetNativeBundleArch_X64Variants() {
		// Mock System.getProperty("os.arch") for "x86_64", "amd64"
		// Verify getNativeBundleArch() returns "x64" for both
	}

	@Test
	void testGetNativeBundleArch_Aarch64Variants() {
		// Mock System.getProperty("os.arch") for "aarch64", "arm64"
		// Verify getNativeBundleArch() returns "aarch64" for both
	}

	@Test
	void testGetNativeBundleArch_UnknownArch() {
		// Mock System.getProperty("os.arch") to return "sparc-v9"
		// Verify getNativeBundleArch() sanitizes to "sparc-v9"
	}

	@Test
	void testGetGenericBundleUrl_Latest() {
		// Call getGenericBundleUrl(null)
		// Verify URL is: https://github.com/jbangdev/jbang/releases/latest/download/jbang.{tar|zip}
	}

	@Test
	void testGetGenericBundleUrl_SpecificVersion() {
		// Call getGenericBundleUrl("0.138.0")
		// Verify URL is: https://github.com/jbangdev/jbang/releases/download/v0.138.0/jbang.{tar|zip}
	}

	@Test
	void testGetNativeBundleUrl_Latest() {
		// Call getNativeBundleUrl(null)
		// Verify URL includes platform/arch, e.g.:
		// https://github.com/jbangdev/jbang/releases/latest/download/jbang-linux-x64.tar
	}

	@Test
	void testGetNativeBundleUrl_SpecificVersion() {
		// Call getNativeBundleUrl("0.138.0")
		// Verify URL is:
		// https://github.com/jbangdev/jbang/releases/download/v0.138.0/jbang-{os}-{arch}.{ext}
	}

	@Test
	void testGetNativeBundleUrl_WithCustomBaseUrl() {
		// Set env var JBANG_DOWNLOAD_BASEURL=http://localhost:18080
		// Call getNativeBundleUrl(null)
		// Verify URL starts with http://localhost:18080/latest/download/...
	}

	@Test
	void testGetDownloadBaseUrl_Default() {
		// Unset JBANG_DOWNLOAD_BASEURL
		// Verify getDownloadBaseUrl() returns default GitHub URL
	}

	@Test
	void testGetDownloadBaseUrl_CustomWithTrailingSlash() {
		// Set JBANG_DOWNLOAD_BASEURL=http://example.com/releases/
		// Verify getDownloadBaseUrl() returns http://example.com/releases (no trailing slash)
	}

	@Test
	void testGetBundleExtension_Windows() {
		// On Windows, verify getBundleExtension() returns "zip"
	}

	@Test
	@EnabledOnOs({OS.LINUX, OS.MAC})
	void testGetBundleExtension_UnixLike() {
		// On Linux/Mac, verify getBundleExtension() returns "tar"
	}

	@Test
	void testIsNativeRequested_True() {
		// Set JBANG_USE_NATIVE=true
		// Verify isNativeRequested() returns true
	}

	@Test
	void testIsNativeRequested_False() {
		// Unset or set JBANG_USE_NATIVE=false
		// Verify isNativeRequested() returns false
	}

	@Test
	void testIsNativeRequested_CaseInsensitive() {
		// Set JBANG_USE_NATIVE=TRUE
		// Verify isNativeRequested() returns true
	}
}
```

**Note**: You may need to use a mocking framework (Mockito) or System stubs library to mock `System.getProperty()` and `System.getenv()` calls.

---

## Integration Tests

### Test Class: `src/it/java/dev/jbang/ITNativeWrapper.java`

These tests should use the `BaseIT` infrastructure with WireMock to simulate HTTP responses.

```java
package dev.jbang;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ITNativeWrapper extends BaseIT {

	@TempDir
	Path tempDir;

	@BeforeEach
	void setUp() {
		// Set up WireMock stubs for:
		// - /latest/download/version.txt
		// - /latest/download/jbang.tar (or jbang.zip on Windows)
		// - /latest/download/jbang-{os}-{arch}.tar
	}

	@Test
	void testWrapperBootstrap_GenericBundle() {
		// Run wrapper script with JBANG_DOWNLOAD_BASEURL pointing to WireMock
		// Verify jbang.jar is installed to ~/.jbang/bin/
		// Verify jbang version succeeds
	}

	@Test
	@EnabledOnOs({OS.LINUX, OS.MAC})
	void testWrapperBootstrap_NativeBundle_BashScript() {
		assumeTrue(isCommandAvailable("bash"));

		// Set JBANG_USE_NATIVE=true
		// Set JBANG_DOWNLOAD_BASEURL to WireMock
		// Run bash wrapper script
		// Verify jbang.bin is installed
		// Verify wrapper prefers jbang.bin over jbang.jar
	}

	@Test
	@EnabledOnOs(OS.WINDOWS)
	void testWrapperBootstrap_NativeBundle_PowerShell() {
		assumeTrue(isCommandAvailable("pwsh"));

		// Set JBANG_USE_NATIVE=true
		// Set JBANG_DOWNLOAD_BASEURL to WireMock
		// Run PowerShell wrapper script
		// Verify jbang.bin.exe is installed
		// Verify wrapper prefers jbang.bin.exe over jbang.jar
	}

	@Test
	void testNativeBundleFallback_BundleNotFound() {
		// Configure WireMock to return 404 for native bundle
		// Return 200 for generic bundle
		// Set JBANG_USE_NATIVE=true
		// Run wrapper
		// Verify fallback warning is printed
		// Verify generic bundle is used
		// Verify jbang.jar exists but jbang.bin does not
	}

	@Test
	void testNativeBundleFallback_NoBinaryInBundle() {
		// Configure WireMock to return a generic jbang.tar as the "native" bundle
		// (i.e., bundle has no jbang.bin file)
		// Set JBANG_USE_NATIVE=true
		// Run wrapper
		// Verify warning about missing native binary
		// Verify execution falls back to jbang.jar
	}

	@Test
	void testNewFilePromotion_JarUpdate() {
		// Install jbang with existing jbang.jar
		// Create jbang.jar.new
		// Run wrapper
		// Verify jbang.jar is replaced with jbang.jar.new
		// Verify jbang.jar.new is deleted
	}

	@Test
	void testNewFilePromotion_NativeBinaryUpdate() {
		// Install jbang with existing jbang.bin
		// Create jbang.bin.new
		// Set JBANG_USE_NATIVE=true
		// Run wrapper
		// Verify jbang.bin is replaced with jbang.bin.new
		// Verify jbang.bin.new is deleted
	}

	@Test
	void testVersionedDownload() {
		// Set JBANG_DOWNLOAD_VERSION=0.138.0
		// Configure WireMock to expect /download/v0.138.0/jbang.tar
		// Run wrapper
		// Verify correct versioned URL is requested
		// Verify installation succeeds
	}

	@Test
	void testCustomBaseUrl_FileProtocol() {
		// Create local file tree in tempDir
		// Set JBANG_DOWNLOAD_BASEURL=file:///path/to/tempDir
		// Run wrapper
		// Verify local files are used
	}

	@Test
	void testCustomBaseUrl_HttpProtocol() {
		// Set JBANG_DOWNLOAD_BASEURL to WireMock URL
		// Run wrapper
		// Verify WireMock receives requests
		// Verify installation succeeds
	}
}
```

**Implementation Tips**:
- Use `BaseIT.shell()` helper to run wrapper scripts
- Use WireMock `stubFor()` to mock HTTP endpoints
- Check `BaseIT.isCommandAvailable("bash")` before running bash tests
- Use `@EnabledOnOs` to run platform-specific tests
- Clean up `JBANG_DIR` in temp directory for each test

---

## Shell Script Tests (Optional)

If CI supports running shell scripts:

### Bash Tests: `src/it/bash/test-native-wrapper.sh`

```bash
#!/usr/bin/env bash
set -euo pipefail

# Test native_bundle_arch function
test_native_bundle_arch() {
	arch="x86_64"
	result=$(bash -c 'source ../../../src/main/scripts/jbang; native_bundle_arch')
	[[ "$result" == "x64" ]] || { echo "FAIL: expected x64, got $result"; exit 1; }
}

# Test resolve_download_url function
test_resolve_download_url() {
	# Test with version
	# Test without version
	# Test with custom base URL
}

# Add more tests for each function
```

Run with:
```bash
./gradlew test --tests "*ITNativeWrapper*"
```

---

## Test Coverage Goals

- **Unit tests**: Helper functions (OS detection, URL construction, environment reading)
- **Integration tests**: Full wrapper bootstrap flow with HTTP mocking
- **Edge cases**:
- Missing native bundles (fallback)
- Missing binaries in bundle (jar fallback)
- .new file promotion
- Version-specific downloads
- Custom base URLs (file://, http://)

---

## Running Tests

```bash
# Unit tests only
./gradlew test --tests "dev.jbang.cli.AppInstallTest"

# Integration tests
./gradlew integrationTest --tests "dev.jbang.ITNativeWrapper"

# All tests
./gradlew test integrationTest
```

---

## Notes for Implementer

1. **Mocking strategy**: Use system stubs or Mockito to mock `System.getProperty()` and `System.getenv()`
2. **WireMo*: Extend `BaseIT` which provides WireMock infrastructure
3. **Platform tests**: Use `@EnabledOnOs` and `assumeTrue(isCommandAvailable(...))` for platform-specific tests
4. **Cleanup**: Ensure temp directories are cleaned up in `@AfterEach`
5. **Fast tests**: Mock HTTP calls to avoid real network I/O

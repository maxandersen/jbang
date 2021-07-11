package dev.jbang.dependencies;

import org.eclipse.aether.artifact.Artifact;

import java.io.File;
import java.util.Objects;

/**
 * class describing artifact coordinates and its resolved physical location.
 */
public class ArtifactInfo {

	private final Artifact coordinate;
	private final File file;
	private final long timestamp;

	ArtifactInfo(Artifact coordinate, File file) {
		this.coordinate = coordinate;
		this.file = file;
		this.timestamp = file.exists() ? file.lastModified() : 0;
	}

	ArtifactInfo(Artifact coordinate, File file, long cachedTimestamp) {
		this.coordinate = coordinate;
		this.file = file;
		this.timestamp = cachedTimestamp;
	}

	public Artifact getCoordinate() {
		return coordinate;
	}

	public File getFile() {
		return file;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public boolean isUpToDate() {
		return file.canRead() && timestamp == file.lastModified();
	}

	public String toString() {
		String path = getFile().getAbsolutePath();
		return getCoordinate() == null ? "<null>" : toCanonicalForm(getCoordinate()) + "=" + path;
	}

	public static String toCanonicalForm(Artifact a) {
		String SEPARATOR_COORDINATE = ":";
		String version = a.getVersion();
		String classifier = a.getClassifier();
		String packaging = a.getExtension();
		String groupId = a.getGroupId();
		String artifactId = a.getArtifactId();

		final StringBuilder sb =new StringBuilder(groupId).append(SEPARATOR_COORDINATE).append(artifactId);


		if (version == null || version.length() == 0) {
			return sb.toString();
		}
		if (classifier != null && classifier.length() > 0 && packaging != null) {
			sb.append(SEPARATOR_COORDINATE).append(packaging).append(SEPARATOR_COORDINATE)
					.append(classifier).append(SEPARATOR_COORDINATE).append(version);
		}
		if ((classifier == null || classifier.length() == 0) && packaging != null) {
			sb.append(SEPARATOR_COORDINATE).append(packaging).append(SEPARATOR_COORDINATE).append(version);
		}

		return sb.toString();
	}
	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		ArtifactInfo that = (ArtifactInfo) o;
		return Objects.equals(coordinate, that.coordinate) &&
				Objects.equals(file, that.file);
	}

	@Override
	public int hashCode() {
		return Objects.hash(coordinate, file);
	}
}

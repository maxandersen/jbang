package dev.jbang.dependencies;

import com.sun.tools.javac.util.List;
import io.quarkus.bootstrap.resolver.maven.MavenArtifactResolver;
import org.eclipse.aether.repository.RemoteRepository;

public class MavenRepo {

	private String id;
	private String url;

	public MavenRepo(String id, String url) {
		this.setId(id);
		this.setUrl(url);
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public void apply(MavenArtifactResolver resolver) {

		resolver.addRemoteRepositories(List.of(new RemoteRepository.Builder(getId() == null ? getUrl() : getId(), "default", getUrl()).build()));
	}
}

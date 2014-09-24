package ca.mestevens.ios;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

import ca.mestevens.ios.utils.ProcessRunner;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Goal which generates your framework dependencies in the target directory.
 *
 * @goal framework-dependencies
 * 
 * @phase initialize
 */
public class FrameworkDependenciesMojo extends AbstractMojo {

	/**
	 * @parameter property="project"
	 * @readonly
	 * @required
	 */
	public MavenProject project;

	/**
	 * The project's remote repositories to use for the resolution of project
	 * dependencies.
	 * 
	 * @parameter default-value="${project.remoteProjectRepositories}"
	 * @readonly
	 */
	protected List<RemoteRepository> projectRepos;

	/**
	 * The entry point to Aether, i.e. the component doing all the work.
	 * 
	 * @component
	 */
	protected RepositorySystem repoSystem;

	/**
	 * The current repository/network configuration of Maven.
	 * 
	 * @parameter default-value="${repositorySystemSession}"
	 * @readonly
	 */
	protected RepositorySystemSession repoSession;

	public void execute() throws MojoExecutionException, MojoFailureException {
		getLog().info("Starting execution");
		Set<Artifact> artifacts = project.getDependencyArtifacts();

		for (Artifact artifact : artifacts) {
			if (artifact.getType().equals("xcode-framework")) {
				getLog().info("Attempting to resolve artifact: " + artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getVersion());
				org.eclipse.aether.artifact.Artifact defaultArtifact = new DefaultArtifact(artifact.getGroupId(), artifact.getArtifactId(), "xcode-framework", artifact.getVersion());

				ArtifactRequest artifactRequest = new ArtifactRequest();
				artifactRequest.setRepositories(projectRepos);
				artifactRequest.setArtifact(defaultArtifact);

				try {
					ArtifactResult artifactResult = repoSystem.resolveArtifact(repoSession, artifactRequest);
					org.eclipse.aether.artifact.Artifact resultArtifact = artifactResult.getArtifact();
					// Get File from result artifact
					File file = resultArtifact.getFile();
					File resultFile = new File(project.getBuild().getDirectory() + "/xcode-dependencies/frameworks/" + resultArtifact.getGroupId() + "/" + resultArtifact.getArtifactId());
					
					if (resultFile.exists()) {
						FileUtils.deleteDirectory(resultFile);
					}
					
					FileUtils.mkdir(resultFile.getAbsolutePath());
					
					ProcessRunner processRunner = new ProcessRunner(getLog());
					int returnValue = processRunner.runProcess(null, "unzip", file.getAbsolutePath(), "-d", resultFile.getAbsolutePath());

					if (returnValue != 0) {
						getLog().error("Could not unzip file: " + artifact.getArtifactId());
						throw new MojoFailureException("Could not unzip file: " + artifact.getArtifactId());
					}

				} catch (ArtifactResolutionException e) {
					getLog().error("Could not resolve artifact: " + artifact.getArtifactId());
					getLog().error(e.getMessage());
					throw new MojoFailureException("Could not resolve artifact: " + artifact.getArtifactId());
				} catch (IOException e) {
					getLog().error("Problem creating/deleting framework file: " + artifact.getArtifactId());
					getLog().error(e.getMessage());
					throw new MojoFailureException("Problem creating/deleting framework file: " + artifact.getArtifactId());
				}

			}
		}

	}
}

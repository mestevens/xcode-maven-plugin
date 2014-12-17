package ca.mestevens.ios;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.util.artifact.JavaScopes;

import ca.mestevens.ios.utils.ProcessRunner;
import ca.mestevens.ios.utils.XCodeProjectUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
	
	/**
	 * @parameter property="xcode.add.dependencies" default-value="false"
	 * @readonly
	 * @required
	 */
	public boolean addDependencies;
	
	/**
	 * @parameter property="xcode.project.name" default-value="${project.artifactId}.xcodeproj"
	 * @readonly
	 * @required
	 */
	public String xcodeProjectName;

	public void execute() throws MojoExecutionException, MojoFailureException {
		getLog().info("Starting execution");
		
		CollectRequest collectRequest = new CollectRequest();
		final Artifact mainArtifact = new DefaultArtifact(project.getArtifact().getId());
		collectRequest.setRoot(new Dependency(mainArtifact, JavaScopes.COMPILE));
		collectRequest.setRepositories(projectRepos);
		DependencyRequest dependencyRequest = new DependencyRequest().setCollectRequest(collectRequest);
		dependencyRequest.setFilter(new DependencyFilter() {

			public boolean accept(DependencyNode node,
					List<DependencyNode> parents) {
				Artifact nodeArtifact = node.getArtifact();
				
				if (nodeArtifact.getGroupId().equals(mainArtifact.getGroupId()) &&
						nodeArtifact.getArtifactId().equals(mainArtifact.getArtifactId())) {
					return false;
				}
				return true;
			}
			
		});
		List<ArtifactResult> resolvedArtifacts;
		try {
			
			resolvedArtifacts = repoSystem.resolveDependencies(repoSession, dependencyRequest).getArtifactResults();
		} catch (DependencyResolutionException e) {
			getLog().error("Could not resolve dependencies");
			getLog().error(e.getMessage());
			throw new MojoFailureException("Could not resolve dependencies");
		}

		List<File> dependencyFiles = new ArrayList<File>();
		for (ArtifactResult resolvedArtifact : resolvedArtifacts) {
			Artifact artifact = resolvedArtifact.getArtifact();
			String type = artifact.getProperty("type", "");
			if (type.equals("xcode-framework") || type.equals("xcode-library")) {
				try {
					// Get File from result artifact
					File file = artifact.getFile();
					String resultFileName = project.getBuild().getDirectory() + "/xcode-dependencies/";
					if (type.equals("xcode-framework")) {
						resultFileName += "frameworks";
					} else if (type.equals("xcode-library")) {
						resultFileName += "libraries";
					}
					resultFileName += "/" + artifact.getGroupId() + "/" + artifact.getArtifactId();
					File resultFile = new File(resultFileName);
					
					if (resultFile.exists()) {
						FileUtils.deleteDirectory(resultFile);
					}
					
					FileUtils.mkdir(resultFile.getAbsolutePath());
					
					ProcessRunner processRunner = new ProcessRunner(getLog());
					int returnValue = processRunner.runProcess(null, "unzip", file.getAbsolutePath(), "-d", resultFile.getAbsolutePath());
					
					if (type.equals("xcode-framework")) {
						dependencyFiles.add(new File(resultFile.getAbsolutePath() + "/" + artifact.getArtifactId() + ".framework"));
					} else if (type.equals("xcode-library")) {
						dependencyFiles.add(new File(resultFile.getAbsolutePath() + "/lib" + artifact.getArtifactId() + ".a"));
					}

					if (returnValue != 0) {
						getLog().error("Could not unzip file: " + artifact.getArtifactId());
						throw new MojoFailureException("Could not unzip file: " + artifact.getArtifactId());
					}
					
				} catch (IOException e) {
					getLog().error("Problem creating/deleting framework file: " + artifact.getArtifactId());
					getLog().error(e.getMessage());
					throw new MojoFailureException("Problem creating/deleting framework file: " + artifact.getArtifactId());
				}

			}
		}
		if (addDependencies) {
			XCodeProjectUtil projectUtil = new XCodeProjectUtil(project.getBasedir().getAbsolutePath() + "/" + xcodeProjectName + "/project.pbxproj");
			projectUtil.addDependencies(dependencyFiles);
		}
	}
}

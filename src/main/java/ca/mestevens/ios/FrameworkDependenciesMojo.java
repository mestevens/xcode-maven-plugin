package ca.mestevens.ios;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
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
import ca.mestevens.ios.utils.XcodeProjectUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Goal which generates your framework dependencies in the target directory.
 */
@Mojo(name = "framework-dependencies", defaultPhase = LifecyclePhase.INITIALIZE)
public class FrameworkDependenciesMojo extends AbstractMojo {

	@Parameter(property = "project", readonly = true, required = true)
	public MavenProject project;

	@Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true, required = true)
    protected List<RemoteRepository> projectRepos;

	/**
	 * The entry point to Aether, i.e. the component doing all the work.
	 */
	@Component
	protected RepositorySystem repoSystem;

	/**
	 * The current repository/network configuration of Maven.
	 */
	@Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
	protected RepositorySystemSession repoSession;
	
	/**
	 * The property to determine whether or not to add the dependencies to the xcodeproj/project.pbxproj file. Defaults to false.
	 */
	@Parameter(property = "xcode.add.dependencies", defaultValue = "false", readonly = true, required = true)
	public boolean addDependencies;
	
	/**
	 * The name of the xcodeproj file. Defaults to ${project.artifactId}.xcodeproj.
	 */
	@Parameter(property = "xcode.project.name", defaultValue = "${project.artifactId}.xcodeproj", readonly = true, required = true)
	public String xcodeProjectName;

	public void execute() throws MojoExecutionException, MojoFailureException {
		getLog().info("Starting execution");
		
		CollectRequest collectRequest = new CollectRequest();
		System.out.println(project.getArtifact().getId());
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
			try {
				XcodeProjectUtil projectUtil = new XcodeProjectUtil(project.getBasedir().getAbsolutePath() + "/" + xcodeProjectName + "/project.pbxproj");
				projectUtil.addDependencies(dependencyFiles);
				projectUtil.writeProject();
			} catch (Exception ex) {
				throw new MojoFailureException(ex.getMessage());
			}
		}
	}
}

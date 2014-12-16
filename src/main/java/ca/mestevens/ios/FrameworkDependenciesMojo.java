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
import ca.mestevens.ios.xcode.parser.models.CommentedIdentifier;
import ca.mestevens.ios.xcode.parser.models.PBXBuildFile;
import ca.mestevens.ios.xcode.parser.models.PBXBuildPhase;
import ca.mestevens.ios.xcode.parser.models.PBXFileElement;
import ca.mestevens.ios.xcode.parser.models.PBXTarget;
import ca.mestevens.ios.xcode.parser.models.XCConfigurationList;
import ca.mestevens.ios.xcode.parser.models.XCodeProject;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
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
	 * @parameter property="xcode.add.frameworks" default-value="false"
	 * @readonly
	 * @required
	 */
	public boolean addFrameworks;
	
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

		List<File> frameworkFiles = new ArrayList<File>();
		for (ArtifactResult resolvedArtifact : resolvedArtifacts) {
			Artifact artifact = resolvedArtifact.getArtifact();
			if (artifact.getProperty("type", "").equals("xcode-framework")) {
				try {
					// Get File from result artifact
					File file = artifact.getFile();
					File resultFile = new File(project.getBuild().getDirectory() + "/xcode-dependencies/frameworks/" + artifact.getGroupId() + "/" + artifact.getArtifactId());
					
					if (resultFile.exists()) {
						FileUtils.deleteDirectory(resultFile);
					}
					
					FileUtils.mkdir(resultFile.getAbsolutePath());
					
					ProcessRunner processRunner = new ProcessRunner(getLog());
					int returnValue = processRunner.runProcess(null, "unzip", file.getAbsolutePath(), "-d", resultFile.getAbsolutePath());
					
					frameworkFiles.add(new File(resultFile.getAbsolutePath() + "/" + artifact.getArtifactId() + ".framework"));

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
		if (addFrameworks) {
			try {
				XCodeProject xcodeProject = new XCodeProject(project.getBasedir().getAbsolutePath() + "/" + xcodeProjectName + "/project.pbxproj");
				List<CommentedIdentifier> embedPhaseIdentifiers = new ArrayList<CommentedIdentifier>();
				List<CommentedIdentifier> fileReferenceIdentifiers = new ArrayList<CommentedIdentifier>();
				//TODO Doesn't seem to be doing anything atm, take a look at it
				List<CommentedIdentifier> copyIdentifiers = new ArrayList<CommentedIdentifier>();
				//Add the framework files as file references and build files
				for (File frameworkFile : frameworkFiles) {
					String frameworkPath = frameworkFile.getAbsolutePath().substring(frameworkFile.getAbsolutePath().lastIndexOf("target"));
					PBXFileElement fileReference = xcodeProject.getFileReferenceWithPath(frameworkPath);
					if (fileReference == null) {
						fileReference = xcodeProject.createFileReference(frameworkPath, "SOURCE_ROOT");
					}
					List<PBXBuildFile> buildFiles = xcodeProject.getBuildFileWithFileRefPath(frameworkPath);
					PBXBuildFile buildFile = null;
					PBXBuildFile copyBuildFile = null;
					for (PBXBuildFile existingFile : buildFiles) {
						if (existingFile.getReference().getComment().equals(frameworkFile.getName() + " in Frameworks")) {
							buildFile = existingFile;
						} else if (existingFile.getReference().getComment().equals(frameworkFile.getName() + " in Embed Frameworks")) {
							copyBuildFile = existingFile;
						}
					}
					if (buildFile == null) {
						buildFile = xcodeProject.createBuildFileFromFileReferencePath(frameworkPath, frameworkFile.getName() + " in Frameworks");
					}
					if (copyBuildFile == null) {
						copyBuildFile = xcodeProject.createBuildFileFromFileReferencePath(frameworkPath, frameworkFile.getName() + " in Embed Frameworks");
						copyBuildFile.getSettings().put("ATTRIBUTES", "(CodeSignOnCopy, )");
					}
					
					embedPhaseIdentifiers.add(buildFile.getReference());
					fileReferenceIdentifiers.add(fileReference.getReference());
					copyIdentifiers.add(copyBuildFile.getReference());
				}
				PBXBuildPhase copyFrameworksBuildPhase = new PBXBuildPhase("PBXCopyFilesBuildPhase", "\"Embed Frameworks\"", copyIdentifiers, "\"\"", 10);
				//If the Embed Frameworks phase doesn't exist, create it
				boolean foundExistingPhase = false;
				for(PBXBuildPhase copyFilesBuildPhase : xcodeProject.getCopyFilesBuildPhases()) {
					if (copyFilesBuildPhase.getReference().getComment().equals("\"Embed Frameworks\"")) {
						for(CommentedIdentifier identifier : copyIdentifiers) {
							boolean foundFile = false;
							for (CommentedIdentifier fileIdentifier : copyFilesBuildPhase.getFiles()) {
								if (fileIdentifier.getComment().equals(identifier.getComment())) {
									foundFile = true;
								}
							}
							if (!foundFile) {
								copyFilesBuildPhase.getFiles().add(identifier);
							}
						}
						foundExistingPhase = true;
					}
				}
				//Grab the first target
				String firstTargetIdentifier = xcodeProject.getProject().getTargets().get(0).getIdentifier();
				if (!foundExistingPhase) {
					xcodeProject.addCopyFilesBuildPhase(firstTargetIdentifier, copyFrameworksBuildPhase);
				}
				//Get the configuration list identifier for the first target
				String existingFrameworksPhaseId = null;
				String buildConfigurationList = null;
				PBXTarget nativeTarget = xcodeProject.getNativeTargetWithIdentifier(firstTargetIdentifier);
				for(CommentedIdentifier buildPhaseIdentifier : nativeTarget.getBuildPhases()) {
					if (buildPhaseIdentifier.getComment().equals("Frameworks")) {
						existingFrameworksPhaseId = buildPhaseIdentifier.getIdentifier();
						buildConfigurationList = nativeTarget.getBuildConfigurationList().getIdentifier();
						break;
					}
				}
				//Add the framework build files to the frameworks build phase
				PBXBuildPhase frameworksBuildPhase = xcodeProject.getFrameworksBuildPhaseWithIdentifier(existingFrameworksPhaseId);
				if (frameworksBuildPhase.getReference().getIdentifier().equals(existingFrameworksPhaseId)) {
					for(CommentedIdentifier identifier : embedPhaseIdentifiers) {
						if (!frameworksBuildPhase.getFiles().contains(identifier)) {
							frameworksBuildPhase.getFiles().add(identifier);
						}
					}
				}
				//Add the properties to the build configuration
				XCConfigurationList configuration = xcodeProject.getConfigurationListWithIdentifier(buildConfigurationList);
				for(CommentedIdentifier identifier : configuration.getBuildConfigurations()) {
					//FRAMEWORK_SEARCH_PATHS
					List<String> frameworkSearchPaths = xcodeProject.getBuildConfigurationPropertyAsList(identifier.getIdentifier(), "FRAMEWORK_SEARCH_PATHS");
					if (frameworkSearchPaths != null) {
						if (!frameworkSearchPaths.contains("\"${PROJECT_DIR}/target/xcode-dependencies/frameworks/**\"")) {
							frameworkSearchPaths.add("\"${PROJECT_DIR}/target/xcode-dependencies/frameworks/**\"");
							xcodeProject.setBuildConfigurationProperty(identifier.getIdentifier(), "FRAMEWORK_SEARCH_PATHS", frameworkSearchPaths);
						}
					} else {
						frameworkSearchPaths = new ArrayList<String>();
						frameworkSearchPaths.add("\"${PROJECT_DIR}/target/xcode-dependencies/frameworks/**\"");
						xcodeProject.setBuildConfigurationProperty(identifier.getIdentifier(), "FRAMEWORK_SEARCH_PATHS", frameworkSearchPaths);
					}
					//LD_RUNPATH_SEARCH_PATHS
					String ldRunpathSearchPaths = xcodeProject.getBuildConfigurationProperty(identifier.getIdentifier(), "LD_RUNPATH_SEARCH_PATHS");
					if (ldRunpathSearchPaths != null) {
						if (ldRunpathSearchPaths.startsWith("\"")) {
							ldRunpathSearchPaths = ldRunpathSearchPaths.substring(1);
						}
						if (ldRunpathSearchPaths.endsWith("\"")) {
							ldRunpathSearchPaths = ldRunpathSearchPaths.substring(0, ldRunpathSearchPaths.length() - 1);
						}
						ldRunpathSearchPaths = ldRunpathSearchPaths.trim();
						if (!ldRunpathSearchPaths.contains("@loader_path/Frameworks")) {
							ldRunpathSearchPaths = ldRunpathSearchPaths.concat(" @loader_path/Frameworks");
						}
						if (!ldRunpathSearchPaths.contains("@executable_path/Frameworks")) {
							ldRunpathSearchPaths = ldRunpathSearchPaths.concat(" @executable_path/Frameworks");
						}
						ldRunpathSearchPaths = "\"" + ldRunpathSearchPaths + "\"";
						xcodeProject.setBuildConfigurationProperty(identifier.getIdentifier(), "LD_RUNPATH_SEARCH_PATHS", ldRunpathSearchPaths);
					} else {
						xcodeProject.setBuildConfigurationProperty(identifier.getIdentifier(), "LD_RUNPATH_SEARCH_PATHS", "\"@loader_path/Frameworks @executable_path/Frameworks\"");
					}
				}
				//Add/Edit the Frameworks group
				String mainGroupIdentifier = xcodeProject.getProject().getMainGroup().getIdentifier();
				String groupIdentifier = xcodeProject.getGroupWithIdentifier(mainGroupIdentifier).getChildren().get(0).getIdentifier();
				PBXFileElement group = xcodeProject.getGroupWithIdentifier(groupIdentifier);
				String frameworkGroupIdentifier = null;
				for(CommentedIdentifier child : group.getChildren()) {
					if (child.getComment().equals("Frameworks")) {
						frameworkGroupIdentifier = child.getIdentifier();
					}
				}
				PBXFileElement frameworkGroup = null;
				if (frameworkGroupIdentifier != null) {
					frameworkGroup = xcodeProject.getGroupWithIdentifier(frameworkGroupIdentifier);
				} else {
					frameworkGroup = xcodeProject.createGroup("Frameworks", groupIdentifier);
				}
				for (CommentedIdentifier fileReference : fileReferenceIdentifiers) {
					boolean found = false;
					for (CommentedIdentifier child : frameworkGroup.getChildren()) {
						if (child.getComment().equals(fileReference.getComment())) {
							found = true;
						}
					}
					if (!found) {
						frameworkGroup.addChild(fileReference);
					}
				}
				//Write out the updated xcode project
				Files.write(Paths.get(project.getBasedir().getAbsolutePath() + "/" + xcodeProjectName + "/project.pbxproj"), xcodeProject.toString().getBytes());
			} catch (Exception ex) {
				ex.printStackTrace();
				throw new MojoExecutionException(ex.getMessage());
			}
		}
	}
}

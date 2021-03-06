package ca.mestevens.ios;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.util.artifact.JavaScopes;

import ca.mestevens.ios.utils.CopyDependenciesUtil;
import ca.mestevens.ios.utils.ProcessRunner;
import ca.mestevens.ios.utils.XcodeProjectUtil;

/**
 * Goal which generates your framework dependencies in the target directory.
 */
@Mojo(name = "xcode-process-test-sources", defaultPhase = LifecyclePhase.PROCESS_TEST_SOURCES, requiresDependencyResolution = ResolutionScope.TEST)
public class XcodeProcessTestSourcesMojo extends AbstractMojo {

	@Parameter(property = "project", readonly = true, required = true)
	public MavenProject project;

	@Parameter(defaultValue = "${project.remoteProjectRepositories}", readonly = true, required = true)
    public List<RemoteRepository> projectRepos;

	/**
	 * The entry point to Aether, i.e. the component doing all the work.
	 */
	@Component
	public RepositorySystem repoSystem;

	/**
	 * The current repository/network configuration of Maven.
	 */
	@Parameter(defaultValue = "${repositorySystemSession}", readonly = true)
	public RepositorySystemSession repoSession;
	
	/**
	 * The property to determine whether or not to add the dependencies to the xcodeproj/project.pbxproj file. Defaults to false.
	 */
	@Parameter(alias = "addDependencies", property = "xcode.add.dependencies", defaultValue = "false", required = true)
	public boolean addDependencies;
	
	/**
	 * The property to determine whether or not to add the dependencies to the xcodeproj/project.pbxproj file. Defaults to true but only if xcode.add.dependencies is true.
	 */
	@Parameter(alias = "addTestDependencies", property = "xcode.add.test.dependencies", defaultValue = "true", required = true)
	public boolean addTestDependencies;
	
	/**
	 * The path to your xcodeproj file. Defaults to ${basedir}/${project.artifactId}.xcodeproj.
	 */
	@Parameter(alias = "project", property = "xcode.project.path", defaultValue = "${basedir}/${project.artifactId}.xcodeproj", required = true)
	public String xcodeProject;
	
	/**
	 * The targets to add test dependencies to. Defaults to only ${project.artifactId}Tests.
	 */
	@Parameter
	public List<String> dependencyTestTargets;
	
	public ProcessRunner processRunner;
	public CopyDependenciesUtil copyDependenciesUtil;
	
	public XcodeProcessTestSourcesMojo() {
		this.processRunner = new ProcessRunner(getLog());
	}

	public void execute() throws MojoExecutionException, MojoFailureException {
		getLog().info("Starting execution");
		
		copyDependenciesUtil = new CopyDependenciesUtil(project, getLog(), processRunner);
		
		Map<String, List<File>> dependencyMap = copyDependenciesUtil.copyDependencies(JavaScopes.TEST);
		if (addDependencies) {
			try {
				XcodeProjectUtil projectUtil = new XcodeProjectUtil(xcodeProject + "/project.pbxproj");
				if (!projectUtil.containsTestTarget()) {
					getLog().info("No test target found.");
					return;
				}
				if (addTestDependencies) {
					if (dependencyTestTargets == null) {
						dependencyTestTargets = new ArrayList<String>();
					}
					if (dependencyTestTargets.isEmpty()) {
						dependencyTestTargets.add(project.getArtifactId() + "Tests");
					}
					for (String target : dependencyTestTargets) {
						projectUtil.addDependenciesToTarget(target, dependencyMap.get("dynamic-frameworks"), dependencyMap.get("static-frameworks"), dependencyMap.get("libraries"), null);
					}
				}
				projectUtil.writeProject();
			} catch (Exception ex) {
				throw new MojoFailureException(ex.getMessage());
			}
		}
	}
	
}

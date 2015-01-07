package ca.mestevens.ios;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import ca.mestevens.ios.utils.ProcessRunner;

/**
 * Goal to test your artifact(s).
 */
@Mojo(name = "xcode-test", defaultPhase = LifecyclePhase.TEST)
public class XcodeTestMojo extends AbstractMojo {

	/**
	 * The location of the xcodebuild executable. Defaults to /usr/bin/xcodebuild.
	 */
	@Parameter(alias = "xcodebuildPath", property = "xcodebuild.path", defaultValue = "/usr/bin/xcodebuild", required = true)
	public String xcodebuild;
	
	/**
	 * The path to your xcodeproj file. Defaults to ${basedir}/${project.artifactId}.xcodeproj.
	 */
	@Parameter(alias = "xcodeProjectPath", property = "xcode.project.path", defaultValue = "${basedir}/${project.artifactId}.xcodeproj", required = true)
	public String xcodeProject;
	
	/**
	 * The name of the scheme to build. Defaults to ${project.artifactId}.
	 */
	@Parameter(alias = "xcodeProjectScheme", property = "xcode.project.scheme.name", defaultValue = "${project.artifactId}", required = true)
	public String xcodeScheme;
	
	@Parameter(property = "project.build.directory", readonly = true, required = true)
	public String targetDirectory;
	
	/**
	 * Specifies whether or not to skip tests. Default value is false.
	 */
	@Parameter(alias = "skipTests", property = "skipTests", defaultValue = "false", required = true)
	public boolean skipTests;
	
	/**
	 * Specifies whether or not to ignore test failures. Default value is false (it won't ignore failures).
	 */
	@Parameter(alias = "ignoreTestFailures", property = "xcode.ignore.test.failures", defaultValue = "false", required = true)
	public boolean ignoreFailures;
	
	@Parameter
	public List<String> testSimulators;
	
	public ProcessRunner processRunner;
	
	public XcodeTestMojo() {
		this.processRunner = new ProcessRunner(getLog(), false);
	}
	
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (skipTests) {
			return;
		}
		List<String> command = new ArrayList<String>();
		command.add(xcodebuild);
		command.add("-project");
		command.add(xcodeProject);
		command.add("-scheme");
		command.add(xcodeScheme);
		if (testSimulators == null) {
			testSimulators = new ArrayList<String>();
		}
		if (testSimulators.isEmpty()) {
			testSimulators.add("iPhone 6");
		}
		for (String simulator : testSimulators) {
			command.add("-destination");
			command.add("platform=iOS Simulator,name=" + simulator + "");
		}
		command.add("test");
		
		int resultCode = processRunner.runProcess(null, command.toArray(new String[command.size()]));
		if (resultCode != 0 && !ignoreFailures) {
			throw new MojoFailureException("There were test failures.");
		}
	}
	
}

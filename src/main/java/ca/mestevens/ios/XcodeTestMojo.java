package ca.mestevens.ios;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import ca.mestevens.ios.utils.ProcessRunner;

/**
 * Goal which generates your framework dependencies in the target directory.
 *
 * @goal xcode-test
 * 
 * @phase test
 */
public class XcodeTestMojo extends AbstractMojo {

	/**
	 * @parameter property="xcodebuild.path" default-value="/usr/bin/xcodebuild"
	 * @readonly
	 * @required
	 */
	public String xcodebuild;
	
	/**
	 * @parameter property="xcodeproj.path" default-value="${basedir}/${project.artifactId}.xcodeproj"
	 * @readonly
	 * @required
	 */
	public String xcodeProject;
	
	/**
	 * @parameter property="xcodeproj.scheme.name" default-value="${project.artifactId}"
	 * @readonly
	 * @required
	 */
	public String xcodeScheme;
	
	/**
	 * @parameter property="project.build.directory"
	 * @readonly
	 * @required
	 */
	public String targetDirectory;
	
	/**
	 * @parameter property="skipTests" default-value="false"
	 * @readonly
	 * @required
	 */
	public boolean skipTests;
	
	/**
	 * @parameter property="xcode.ignore.test.failures" default-value="false"
	 * @readonly
	 * @required
	 */
	public boolean ignoreFailures;
	
	/**
	 * @parameter
	 */
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

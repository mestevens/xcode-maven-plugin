package ca.mestevens.xcode_maven_plugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

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
	
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (skipTests) {
			return;
		}
		try {
			ProcessBuilder processBuilder = new ProcessBuilder(xcodebuild, "-project", xcodeProject, "-scheme", xcodeScheme,
					"-sdk", "iphonesimulator", "test");
			processBuilder.redirectErrorStream(true);
			Process process = processBuilder.start();
			BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line = null;
			while ((line = input.readLine()) != null) {
				getLog().info(line);
			}
			int processCode = process.waitFor();
			if (processCode != 0 && !ignoreFailures) {
				throw new MojoFailureException("There were test failures.");
			}
		} catch (IOException e) {
			getLog().error(e.getMessage());
			throw new MojoFailureException(e.getMessage());
		} catch (InterruptedException e) {
			getLog().error(e.getMessage());
			throw new MojoFailureException(e.getMessage());
		}
	}
	
}

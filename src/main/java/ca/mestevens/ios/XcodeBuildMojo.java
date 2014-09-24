package ca.mestevens.ios;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

/**
 * Goal which generates your framework dependencies in the target directory.
 *
 * @goal xcode-build
 * 
 * @phase compile
 */
public class XcodeBuildMojo extends AbstractMojo {
	
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

	public void execute() throws MojoExecutionException, MojoFailureException {
		try {
			ProcessBuilder processBuilder = new ProcessBuilder(xcodebuild, "-project", xcodeProject, "-scheme", xcodeScheme,
					"-destination", "generic/platform=iOS", "CONFIGURATION_BUILD_DIR=" + targetDirectory, "archive");
			processBuilder.redirectErrorStream(true);
			Process process = processBuilder.start();
			BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line = null;
			while ((line = input.readLine()) != null) {
				getLog().info(line);
			}
			process.waitFor();
		} catch (IOException e) {
			getLog().error(e.getMessage());
			throw new MojoFailureException(e.getMessage());
		} catch (InterruptedException e) {
			getLog().error(e.getMessage());
			throw new MojoFailureException(e.getMessage());
		}
	}

}

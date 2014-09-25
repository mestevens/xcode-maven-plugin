package ca.mestevens.ios;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import ca.mestevens.ios.utils.ProcessRunner;

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
	
	/**
	 * @parameter property="xcode.framework.name" default-value="${project.artifactId}"
	 * @readonly
	 * @required
	 */
	public String frameworkName;

	public void execute() throws MojoExecutionException, MojoFailureException {
		ProcessRunner processRunner = new ProcessRunner(getLog());
		int returnValue = processRunner.runProcess(null, xcodebuild, "-project", xcodeProject, "-scheme", xcodeScheme,
				"-sdk", "iphonesimulator", "CONFIGURATION_BUILD_DIR=" + targetDirectory + "/iphonesimulator", "build");
		checkReturnValue(returnValue);
		returnValue = processRunner.runProcess(null, xcodebuild, "-project", xcodeProject, "-scheme", xcodeScheme,
				"-sdk", "iphonesimulator", "-arch", "x86_64", "CONFIGURATION_BUILD_DIR=" + targetDirectory + "/iphonesimulator6", "build");
		checkReturnValue(returnValue);
		returnValue = processRunner.runProcess(null, xcodebuild, "-project", xcodeProject, "-scheme", xcodeScheme,
				"-sdk", "iphoneos", "CONFIGURATION_BUILD_DIR=" + targetDirectory + "/iphoneos", "build");
		checkReturnValue(returnValue);
		returnValue = processRunner.runProcess(targetDirectory, "lipo", "-create", "-output", frameworkName, 
				"iphonesimulator/" + frameworkName + ".framework/" + frameworkName, "iphonesimulator6/" + frameworkName + ".framework/" + frameworkName,
				"iphoneos/" + frameworkName + ".framework/" + frameworkName);
		checkReturnValue(returnValue);
		processRunner.runProcess(targetDirectory, "cp", "-r", "iphoneos/" + frameworkName + ".framework", ".");
		processRunner.runProcess(targetDirectory, "cp", frameworkName, frameworkName + ".framework/.");
	}
	
	protected void checkReturnValue(int returnValue) throws MojoFailureException {
		if (returnValue != 0) {
			throw new MojoFailureException("Failed to build project.");
		}
	}

}

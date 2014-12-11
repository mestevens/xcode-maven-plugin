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
	
	/**
	 * @parameter property="xcode.build.64bit" default-value=true
	 * @readonly
	 * @required
	 */
	public boolean build64bit;

	public void execute() throws MojoExecutionException, MojoFailureException {
		ProcessRunner processRunner = new ProcessRunner(getLog());
		int returnValue = processRunner.runProcess(null, xcodebuild, "-project", xcodeProject, "-scheme", xcodeScheme,
				"-sdk", "iphonesimulator", "CONFIGURATION_BUILD_DIR=" + targetDirectory + "/iphonesimulator", "build");
		checkReturnValue(returnValue);
		if (build64bit) {
			returnValue = processRunner.runProcess(null, xcodebuild, "-project", xcodeProject, "-scheme", xcodeScheme,
					"-sdk", "iphonesimulator", "-arch", "x86_64", "CONFIGURATION_BUILD_DIR=" + targetDirectory + "/iphonesimulator6", "build");
			checkReturnValue(returnValue);
		}
		returnValue = processRunner.runProcess(null, xcodebuild, "-project", xcodeProject, "-scheme", xcodeScheme,
				"-sdk", "iphoneos", "CONFIGURATION_BUILD_DIR=" + targetDirectory + "/iphoneos", "build");
		checkReturnValue(returnValue);
		List<String> lipoCommand = new ArrayList<String>();
		lipoCommand.add("lipo");
		lipoCommand.add("-create");
		lipoCommand.add("-output");
		lipoCommand.add(frameworkName);
		lipoCommand.add("iphonesimulator/" + frameworkName + ".framework/" + frameworkName);
		if (build64bit) {
			lipoCommand.add("iphonesimulator6/" + frameworkName + ".framework/" + frameworkName);
		}
		lipoCommand.add("iphoneos/" + frameworkName + ".framework/" + frameworkName);
		returnValue = processRunner.runProcess(targetDirectory, lipoCommand.toArray(new String[lipoCommand.size()]));
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

package ca.mestevens.ios;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import ca.mestevens.ios.utils.ProcessRunner;
import ca.mestevens.ios.xcode.parser.exceptions.InvalidObjectFormatException;
import ca.mestevens.ios.xcode.parser.models.CommentedIdentifier;
import ca.mestevens.ios.xcode.parser.models.XCBuildConfiguration;
import ca.mestevens.ios.xcode.parser.models.XCConfigurationList;
import ca.mestevens.ios.xcode.parser.models.XCodeProject;

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
	 * @parameter property="xcode.simulator.archs"
	 * @readonly
	 */
	public List<String> simulatorArchs;
	
	/**
	 * @parameter property="xcode.device.archs"
	 * @readonly
	 */
	public List<String> deviceArchs;

	public void execute() throws MojoExecutionException, MojoFailureException {
		ProcessRunner processRunner = new ProcessRunner(getLog());
		if (simulatorArchs == null || simulatorArchs.size() == 0) {
			simulatorArchs = new ArrayList<String>();
			simulatorArchs.add("i386");
			simulatorArchs.add("x86_64");
		}
		if (deviceArchs == null || deviceArchs.size() == 0) {
			deviceArchs = new ArrayList<String>();
			try {
				XCodeProject project = new XCodeProject(xcodeProject + "/project.pbxproj");
				XCConfigurationList mainConfiguration = project.getConfigurationListWithIdentifier(project.getProject().getBuildConfigurationList().getIdentifier());
				for (CommentedIdentifier configuration : mainConfiguration.getBuildConfigurations()) {
					XCBuildConfiguration buildConfiguration = project.getBuildConfigurationWithIdentifier(configuration.getIdentifier());
					if (buildConfiguration != null) {
						List<String> archs = buildConfiguration.getBuildSettingAsList("ARCHS");
						if (archs != null) {
							deviceArchs = archs;
							break;
						}
					}
				}
			} catch (InvalidObjectFormatException e) {
				throw new MojoExecutionException(e.getMessage());
			}
			if (deviceArchs == null || deviceArchs.size() == 0) {
				deviceArchs.add("armv7");
				deviceArchs.add("arm64");
			}
		}
		int returnValue = 0;
		for (String simulatorArch : simulatorArchs) {
			returnValue = processRunner.runProcess(null, xcodebuild, "-project", xcodeProject, "-scheme", xcodeScheme,
					"-sdk", "iphonesimulator", "-arch", simulatorArch, "CONFIGURATION_BUILD_DIR=" + targetDirectory + "/iphonesimulator-" + simulatorArch, "build");
			checkReturnValue(returnValue);
		}
		for (String deviceArch : deviceArchs) {
			returnValue = processRunner.runProcess(null, xcodebuild, "-project", xcodeProject, "-scheme", xcodeScheme,
					"-sdk", "iphoneos", "-arch", deviceArch, "CONFIGURATION_BUILD_DIR=" + targetDirectory + "/iphoneos-" + deviceArch, "build");
			checkReturnValue(returnValue);
		}
		List<String> lipoCommand = new ArrayList<String>();
		lipoCommand.add("lipo");
		lipoCommand.add("-create");
		lipoCommand.add("-output");
		lipoCommand.add(frameworkName);
		for (String simulatorArch : simulatorArchs) {
			lipoCommand.add("iphonesimulator-" + simulatorArch + "/" + frameworkName + ".framework/" + frameworkName);
		}
		for (String deviceArch : deviceArchs) {
			lipoCommand.add("iphoneos-" + deviceArch + "/" + frameworkName + ".framework/" + frameworkName);
		}
		returnValue = processRunner.runProcess(targetDirectory, lipoCommand.toArray(new String[lipoCommand.size()]));
		checkReturnValue(returnValue);
		processRunner.runProcess(targetDirectory, "cp", "-r", "iphoneos-" + deviceArchs.get(0) + "/" + frameworkName + ".framework", ".");
		processRunner.runProcess(targetDirectory, "cp", frameworkName, frameworkName + ".framework/.");
	}
	
	protected void checkReturnValue(int returnValue) throws MojoFailureException {
		if (returnValue != 0) {
			throw new MojoFailureException("Failed to build project.");
		}
	}

}

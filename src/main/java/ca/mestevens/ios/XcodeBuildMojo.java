package ca.mestevens.ios;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;

import ca.mestevens.ios.utils.ProcessRunner;
import ca.mestevens.ios.xcode.parser.exceptions.InvalidObjectFormatException;
import ca.mestevens.ios.xcode.parser.models.CommentedIdentifier;
import ca.mestevens.ios.xcode.parser.models.XCBuildConfiguration;
import ca.mestevens.ios.xcode.parser.models.XCConfigurationList;
import ca.mestevens.ios.xcode.parser.models.XCodeProject;

/**
 * Goal to build your artifact.
 */
@Mojo(name = "xcode-build", defaultPhase = LifecyclePhase.COMPILE)
public class XcodeBuildMojo extends AbstractMojo {
	
	@Parameter(property = "project", readonly = true, required = true)
	public MavenProject project;
	
	/**
	 * The location of the xcodebuild executable. Defaults to /usr/bin/xcodebuild.
	 */
	@Parameter(alias = "xcodebuildPath", property = "xcodebuild.path", defaultValue = "/usr/bin/xcodebuild", required = true)
	public String xcodebuild;
	
	/**
	 * The path to your xcodeproj file. Defaults to ${basedir}/${project.artifactId}.xcodeproj.
	 */
	@Parameter(alias = "project", property = "xcode.project.path", defaultValue = "${basedir}/${project.artifactId}.xcodeproj", required = true)
	public String xcodeProject;
	
	/**
	 * Builds the specified target.
	 */
	@Parameter(alias = "target", property = "xcode.project.target", required = false)
	public String xcodeTarget;
	
	/**
	 * Build all the targets in the specified project.
	 */
	@Parameter(alias = "allTargets", property = "xcode.project.all.targets", required = false)
	public boolean xcodeAllTargets;
	
	/**
	 * Build the specified workspace.
	 */
	@Parameter(alias = "workspace", property = "xcode.project.workspace", required = false)
	public String xcodeWorkspace;
	
	/**
	 * The name of the scheme to build. Defaults to ${project.artifactId}.
	 */
	@Parameter(alias = "scheme", property = "xcode.project.scheme", defaultValue = "${project.artifactId}", required = true)
	public String xcodeScheme;
	
	/**
	 * Use the specified timeout when searching for a destination device. The default is 30 seconds.
	 */
	@Parameter(alias = "destinationTimeout", property = "xcode.project.destination.timeout", required = false)
	public Integer xcodeDestinationTimeout;
	
	/**
	 * Use the specified build configuration when building each target.
	 */
	@Parameter(alias = "configuration", property = "xcode.project.configuration.name", required = false)
	public String xcodeConfigurationName;
	
	/**
	 * Overrides the folder that should be used for derived data when performing a build action on a scheme in a workspace.
	 */
	@Parameter(alias = "derivedDataPath", property = "xcode.project.derived.data.path", required = false)
	public String xcodeDerivedDataPath;
	
	/**
	 * Writes a bundle to the specified path with results from performing a build action on a scheme in a workspace.
	 */
	@Parameter(alias = "resultBundlePath", property = "xcode.project.result.bundle.path", required = false)
	public String xcodeResultBundlePath;
	
	/**
	 * Load the build settings defined in the specified file when building all targets. These settings will over-ride all other settings, including settings passed individually on the command line.
	 */
	@Parameter(alias = "xcconfig", property = "xcode.project.xcconfig.file.name", required = false)
	public String xcodeXcconfig;
	
	/**
	 * Skip build actions that cannot be performed instead of failing.
	 */
	@Parameter(alias = "skipUnavailableActions", property = "xcode.project.skip.unavailable.actions", defaultValue = "false", required = false)
	public boolean xcodeSkipUnavailableActions;
	
	@Parameter(property = "project.build.directory", readonly = true, required = true)
	public String targetDirectory;
	
	/**
	 * The name of the artifact. Defaults to ${project.artifactId}
	 */
	@Parameter(alias = "xcodeProjectArtifactName", property = "xcode.artifact.name", defaultValue = "${project.artifactId}", required = true)
	public String artifactName;
	
	/**
	 * The list of simulator architectures to build.
	 */
	@Parameter
	public List<String> simulatorArchs;
	
	/**
	 * The list of device architectures to build.
	 */
	@Parameter
	public List<String> deviceArchs;
	
	/**
	 * A map of build settings to add.
	 */
	@Parameter
	public Map<String, String> buildSettings;
	
	/**
	 * A map of user defaults to add.
	 */
	@Parameter
	public Map<String, String> userDefaults;
	
	/**
	 * Property to determine whether or not to build for the simulator.
	 */
	@Parameter(alias = "buildSimulator", property = "xcode.project.build.simulator", defaultValue = "true", required = false)
	public boolean buildSimulator;
	
	/**
	 * Property to determine whether or not to build for device.
	 */
	@Parameter(alias = "buildDevice", property = "xcode.project.build.device", defaultValue = "true", required = false)
	public boolean buildDevice;
	
	@Parameter(alias = "forceCodeSigning", property = "xcode.project.force.code.signing", defaultValue = "false", required = false)
	public boolean forceCodeSigning;
	
	public ProcessRunner processRunner;
	
	public XcodeBuildMojo() {
		this.processRunner = new ProcessRunner(getLog());
	}
	
	public void execute() throws MojoExecutionException, MojoFailureException {
		String packaging = project.getPackaging();
		if (packaging.equals("xcode-dynamic-framework") || packaging.equals("xcode-static-framework") || packaging.equals("xcode-library")) {
			if (simulatorArchs == null) {
				simulatorArchs = new ArrayList<String>();
				simulatorArchs.add("i386");
				simulatorArchs.add("x86_64");
			}
			if (deviceArchs == null) {
				deviceArchs = new ArrayList<String>();
				File pbxprojFile = new File(xcodeProject + "/project.pbxproj");
				if (pbxprojFile.exists()) {
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
				}
				if (deviceArchs.isEmpty()) {
					deviceArchs.add("armv7");
					deviceArchs.add("arm64");
				}
			}
			if (simulatorArchs == null || simulatorArchs.isEmpty()) {
				buildSimulator = false;
			}
			if (deviceArchs == null || deviceArchs.isEmpty()) {
				buildDevice = false;
			}
			int returnValue = 0;
			List<String> buildCommands = createCommonBuildCommands();
			if (!forceCodeSigning) {
				buildCommands.add("CODE_SIGNING_REQUIRED=NO");
			}
			if (buildSimulator) {
				for (String simulatorArch : simulatorArchs) {
					List<String> simulatorBuildCommands = new ArrayList<String>(buildCommands);
					simulatorBuildCommands.add("-sdk");
					simulatorBuildCommands.add("iphonesimulator");
					simulatorBuildCommands.add("-arch");
					simulatorBuildCommands.add(simulatorArch);
					simulatorBuildCommands.add("CONFIGURATION_BUILD_DIR=" + targetDirectory + "/iphonesimulator-" + simulatorArch);
					simulatorBuildCommands.add("build");
					returnValue = processRunner.runProcess(null, simulatorBuildCommands.toArray(new String[simulatorBuildCommands.size()]));
					checkReturnValue(returnValue);
				}
			}
			if (buildDevice) {
				for (String deviceArch : deviceArchs) {
					List<String> deviceBuildCommands = new ArrayList<String>(buildCommands);
					deviceBuildCommands.add("-sdk");
					deviceBuildCommands.add("iphoneos");
					deviceBuildCommands.add("-arch");
					deviceBuildCommands.add(deviceArch);
					deviceBuildCommands.add("CONFIGURATION_BUILD_DIR=" + targetDirectory + "/iphoneos-" + deviceArch);
					deviceBuildCommands.add("build");
					returnValue = processRunner.runProcess(null, deviceBuildCommands.toArray(new String[deviceBuildCommands.size()]));
					checkReturnValue(returnValue);
				}
			}
			if (buildSimulator || buildDevice) {
				List<String> lipoCommand = new ArrayList<String>();
				lipoCommand.add("lipo");
				lipoCommand.add("-create");
				lipoCommand.add("-output");

				String libraryLocation = "";
				if (packaging.equals("xcode-dynamic-framework")) {
					lipoCommand.add(artifactName);
					libraryLocation = artifactName + ".framework/" + artifactName;
				} else if (packaging.equals("xcode-library") || packaging.equals("xcode-static-framework")) {
					lipoCommand.add("lib" + artifactName + ".a");
					libraryLocation =  "lib" + artifactName + ".a";
				}
				for (String simulatorArch : simulatorArchs) {
					lipoCommand.add("iphonesimulator-" + simulatorArch + "/" + libraryLocation);
				}
				for (String deviceArch : deviceArchs) {
					lipoCommand.add("iphoneos-" + deviceArch + "/" + libraryLocation);
				}

				returnValue = processRunner.runProcess(targetDirectory, lipoCommand.toArray(new String[lipoCommand.size()]));
				checkReturnValue(returnValue);
				if (packaging.equals("xcode-dynamic-framework")) {
					if (buildDevice) {
						processRunner.runProcess(targetDirectory, "cp", "-r", "iphoneos-" + deviceArchs.get(0) + "/" + artifactName + ".framework", ".");
					} else {
						processRunner.runProcess(targetDirectory, "cp", "-r", "iphonesimulator-" + simulatorArchs.get(0) + "/" + artifactName + ".framework", ".");
					}
					processRunner.runProcess(targetDirectory, "cp", artifactName, artifactName + ".framework/.");
				} else if (packaging.equals("xcode-static-framework")) {
					processRunner.runProcess(targetDirectory, "cp", "-r", "iphoneos-" + deviceArchs.get(0) + "/include", "Headers");
					processRunner.runProcess(targetDirectory, "mv", libraryLocation, artifactName);
					File frameworkFile = new File(targetDirectory + "/" + artifactName + ".framework");
					if (frameworkFile.exists()) {
						try {
							FileUtils.deleteDirectory(frameworkFile);
						} catch (IOException e) {
							getLog().error("Problem creating/deleting framework file: " + artifactName);
							getLog().error(e.getMessage());
							throw new MojoFailureException("Problem creating/deleting framework file: " + artifactName);
						}
					}
					processRunner.runProcess(targetDirectory, "mkdir", artifactName + ".framework");
					processRunner.runProcess(targetDirectory, "cp", "-R", "Headers", artifactName, artifactName + ".framework");
				} else if (packaging.equals("xcode-library")) {
					processRunner.runProcess(targetDirectory, "cp", "-r", "iphoneos-" + deviceArchs.get(0) + "/include", "headers");
				}
			}
		} else if (packaging.equals("xcode-application")) {
			List<String> applicationBuildCommands = createCommonBuildCommands();
			applicationBuildCommands.add("build");
			int returnValue = 0;
			if (buildSimulator) {
				List<String> simulatorBuildCommands = new ArrayList<String>(applicationBuildCommands);
				simulatorBuildCommands.add("-sdk");
				simulatorBuildCommands.add("iphonesimulator");
				simulatorBuildCommands.add("CONFIGURATION_BUILD_DIR=" + targetDirectory + "/build/iPhone-simulator");
				returnValue = processRunner.runProcess(null, simulatorBuildCommands.toArray(new String[simulatorBuildCommands.size()]));
				checkReturnValue(returnValue);
			}
			if (buildDevice) {
				List<String> deviceBuildCommands = new ArrayList<String>(applicationBuildCommands);
				deviceBuildCommands.add("-sdk");
				deviceBuildCommands.add("iphoneos");
				deviceBuildCommands.add("CONFIGURATION_BUILD_DIR=" + targetDirectory + "/build/iPhone");
				returnValue = processRunner.runProcess(null, deviceBuildCommands.toArray(new String[deviceBuildCommands.size()]));
				checkReturnValue(returnValue);
			}
		}
	}
	
	protected void checkReturnValue(int returnValue) throws MojoFailureException {
		if (returnValue != 0) {
			throw new MojoFailureException("Failed to build project.");
		}
	}
	
	protected List<String> createCommonBuildCommands() {
		List<String> buildCommands = new ArrayList<String>();
		buildCommands.add(xcodebuild);
		buildCommands.add("-project");
		buildCommands.add(xcodeProject);
		buildCommands.add("-scheme");
		buildCommands.add(xcodeScheme);
		if (xcodeTarget != null && !xcodeTarget.isEmpty()) {
			buildCommands.add("-target");
			buildCommands.add(xcodeTarget);
		}
		if (xcodeAllTargets) {
			buildCommands.add("-alltargets");
		}
		if (xcodeWorkspace != null && !xcodeWorkspace.isEmpty()) {
			buildCommands.add("-workspace");
			buildCommands.add(xcodeWorkspace);
		}
		if (xcodeDestinationTimeout != null && xcodeDestinationTimeout > 0) {
			buildCommands.add("-destination-timeout");
			buildCommands.add(xcodeDestinationTimeout.toString());
		}
		if (xcodeConfigurationName != null && !xcodeConfigurationName.isEmpty()) {
			buildCommands.add("-configuration");
			buildCommands.add(xcodeConfigurationName);
		}
		if (xcodeDerivedDataPath != null && !xcodeDerivedDataPath.isEmpty()) {
			buildCommands.add("-derivedDataPath");
			buildCommands.add(xcodeDerivedDataPath);
		}
		if (xcodeResultBundlePath != null && !xcodeResultBundlePath.isEmpty()) {
			buildCommands.add("-resultBundlePath");
			buildCommands.add(xcodeResultBundlePath);
		}
		if (xcodeXcconfig != null && !xcodeXcconfig.isEmpty()) {
			buildCommands.add("-xcconfig");
			buildCommands.add(xcodeXcconfig);
		}
		if (xcodeSkipUnavailableActions) {
			buildCommands.add("-skipUnavailableActions");
		}
		if (buildSettings != null) {
			for (String key : buildSettings.keySet()) {
				buildCommands.add(key + "=" + buildSettings.get(key));
			}
		}
		if (userDefaults != null) {
			for (String key : userDefaults.keySet()) {
				buildCommands.add("-" + key + "=" + userDefaults.get(key));
			}
		}
		return buildCommands;
	}

}

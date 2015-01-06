package ca.mestevens.ios;

import java.io.File;
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

import ca.mestevens.ios.utils.ProcessRunner;
import ca.mestevens.ios.xcode.parser.exceptions.InvalidObjectFormatException;
import ca.mestevens.ios.xcode.parser.models.CommentedIdentifier;
import ca.mestevens.ios.xcode.parser.models.XCBuildConfiguration;
import ca.mestevens.ios.xcode.parser.models.XCConfigurationList;
import ca.mestevens.ios.xcode.parser.models.XCodeProject;

/**
 * Goal which generates your framework dependencies in the target directory.
 */
@Mojo(name = "xcode-build", defaultPhase = LifecyclePhase.COMPILE)
public class XcodeBuildMojo extends AbstractMojo {
	
	@Parameter(property = "project", readonly = true, required = true)
	public MavenProject project;
	
	/**
	 * The location of the xcodebuild executable. Defaults to /usr/bin/xcodebuild.
	 */
	@Parameter(alias = "xcodebuildPath", property = "xcodebuild.path", defaultValue = "/usr/bin/xcodebuild", readonly = true, required = true)
	public String xcodebuild;
	
	/**
	 * The path to your xcodeproj file. Defaults to ${basedir}/${project.artifactId}.xcodeproj.
	 */
	@Parameter(alias = "xcodeProjectPath", property = "xcode.project.path", defaultValue = "${basedir}/${project.artifactId}.xcodeproj", readonly = true, required = true)
	public String xcodeProject;
	
	/**
	 * The name of the scheme to build. Defaults to ${project.artifactId}.
	 */
	@Parameter(alias = "xcodeProjectScheme", property = "xcode.project.scheme.name", defaultValue = "${project.artifactId}", readonly = true, required = true)
	public String xcodeScheme;
	
	@Parameter(property = "project.build.directory", readonly = true, required = true)
	public String targetDirectory;
	
	/**
	 * The name of the artifact. Defaults to ${project.artifactId}
	 */
	@Parameter(alias = "xcodeProjectArtifactName", property = "xcode.artifact.name", defaultValue = "${project.artifactId}", readonly = true, required = true)
	public String artifactName;
	
	/**
	 * The list of simulator architectures to build.
	 */
	@Parameter(readonly = true)
	public List<String> simulatorArchs;
	
	/**
	 * The list of device architectures to build.
	 */
	@Parameter(readonly = true)
	public List<String> deviceArchs;
	
	/**
	 * A map of build options to add.
	 */
	@Parameter(readonly = true)
	public Map<String, String> buildOptions;
	
	public ProcessRunner processRunner;
	
	public XcodeBuildMojo() {
		this.processRunner = new ProcessRunner(getLog());
	}
	
	public void execute() throws MojoExecutionException, MojoFailureException {
		String packaging = project.getPackaging();
		if (simulatorArchs == null || simulatorArchs.size() == 0) {
			simulatorArchs = new ArrayList<String>();
			simulatorArchs.add("i386");
			simulatorArchs.add("x86_64");
		}
		if (deviceArchs == null || deviceArchs.size() == 0) {
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
			if (deviceArchs == null || deviceArchs.size() == 0) {
				deviceArchs.add("armv7");
				deviceArchs.add("arm64");
			}
		}
		int returnValue = 0;
		List<String> buildCommands = new ArrayList<String>();
		buildCommands.add(xcodebuild);
		if (buildOptions != null) {
			for (String key : buildOptions.keySet()) {
				buildCommands.add("-" + key);
				buildCommands.add(buildOptions.get(key));
			}
		}
		if (!buildCommands.contains("-project")) {
			buildCommands.add("-project");
			buildCommands.add(xcodeProject);
		}
		if (!buildCommands.contains("-scheme")) {
			buildCommands.add("-scheme");
			buildCommands.add(xcodeScheme);
		}
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
		List<String> lipoCommand = new ArrayList<String>();
		lipoCommand.add("lipo");
		lipoCommand.add("-create");
		lipoCommand.add("-output");
		
		String libraryLocation = "";
		if (packaging.equals("xcode-framework")) {
			lipoCommand.add(artifactName);
			libraryLocation = artifactName + ".framework/" + artifactName;
		} else if (packaging.equals("xcode-library")) {
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
		if (packaging.equals("xcode-framework")) {
			processRunner.runProcess(targetDirectory, "cp", "-r", "iphoneos-" + deviceArchs.get(0) + "/" + artifactName + ".framework", ".");
			processRunner.runProcess(targetDirectory, "cp", artifactName, artifactName + ".framework/.");
		} else if (packaging.equals("xcode-library")) {
			processRunner.runProcess(targetDirectory, "cp", "-r", "iphoneos-" + deviceArchs.get(0) + "/include", "headers");
		}
	}
	
	protected void checkReturnValue(int returnValue) throws MojoFailureException {
		if (returnValue != 0) {
			throw new MojoFailureException("Failed to build project.");
		}
	}

}

package ca.mestevens.ios;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import ca.mestevens.ios.utils.ProcessRunner;

/**
 * Goal to package your .app file and optionally deploy it to the simulator.
 */
@Mojo(name = "xcode-package-app", defaultPhase = LifecyclePhase.PACKAGE)
public class XcodePackageApplicationMojo extends AbstractMojo {
	
	/**
	 * The path to your xcrun command. This defaults to /usr/bin/xcrun."
	 */
	@Parameter(alias = "xcrun", property = "xcode.project.xcrun.path", defaultValue = "/usr/bin/xcrun", required = true)
	public String xcrun;
	
	/**
	 * A flag to determine whether or not you want to deploy your .app to the simulator and run it.
	 */
	@Parameter(alias = "deployApp", property = "xcode.project.deploy", defaultValue = "false", required = false)
	public boolean deploy;
	
	/**
	 * The simulator to deploy to, usually takes the form of something like "iPhone <#> (<iOS version> Simulator)". Defaults to "iPhone 6 (8.1 Simulator)".
	 */
	@Parameter(alias = "iPhoneSimulatorName", property = "xcode.project.simulator.name", defaultValue = "iPhone 6 (8.1 Simulator)", required = false)
	public String deployDevice;
	
	/**
	 * The bundle id of your application. This is required.
	 */
	@Parameter(alias = "bundleId", property = "xcode.project.bundle.id", required = true)
	public String bundleId;
	
	/**
	 * The path to your .app file created by the build mojo. Defaults to "${project.build.directory}/build/${project.artifactId}.app".
	 */
	@Parameter(alias = "simulatorAppPath", property = "xcode.project.simulator.app.path", defaultValue = "${project.build.directory}/build/iPhone-simulator/${project.artifactId}.app", required = true)
	public String simulatorAppPath;
	
	/**
	 * The path to your .app file created by the build mojo. Defaults to "${project.build.directory}/build/${project.artifactId}.app".
	 */
	@Parameter(alias = "deviceAppPath", property = "xcode.project.device.app.path", defaultValue = "${project.build.directory}/build/iPhone/${project.artifactId}.app", required = true)
	public String deviceAppPath;
	
	/**
	 * The name of the ipa file you want to create. Defaults to ${project.artifactId}.ipa.
	 */
	@Parameter(alias = "ipaName", property = "xcode.project.ipa.name", defaultValue = "${project.artifactId}.ipa", required = false)
	public String ipaName;
	
	@Parameter(property = "project.build.directory", readonly = true, required = true)
	public String targetDirectory;
	
	public ProcessRunner processRunner;
	
	public XcodePackageApplicationMojo() {
		processRunner = new ProcessRunner(getLog(), false);
	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		List<String> packageCommand = new ArrayList<String>();
		packageCommand.add(xcrun);
		packageCommand.add("-sdk");
		packageCommand.add("iphoneos");
		packageCommand.add("PackageApplication");
		packageCommand.add("-v");
		packageCommand.add(simulatorAppPath);
		packageCommand.add("-o");
		packageCommand.add(targetDirectory + "/" + ipaName);
		processRunner.runProcess(targetDirectory, packageCommand.toArray(new String[packageCommand.size()]));
		if (deploy) {
			int returnCode = processRunner.runProcess(targetDirectory, "ios-deploy", "-c");
			if (returnCode == 127) {
				returnCode = processRunner.runProcess(targetDirectory, "npm", "install", "-g", "ios-deploy");
				if (returnCode == 127) {
					returnCode = processRunner.runProcess(targetDirectory, "brew", "install", "node");
					if (returnCode == 127) {
						processRunner.runProcess(targetDirectory, "ruby", "-e", "\"$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/master/install)\"");
						processRunner.runProcess(targetDirectory, "brew", "install", "node");
					}
					processRunner.runProcess(targetDirectory, "npm", "install", "-g", "ios-deploy");
				}
				returnCode = processRunner.runProcess(targetDirectory, "ios-deploy", "-c");
			}
			if (returnCode == 0) {
				processRunner.runProcess(targetDirectory, "ios-deploy", "-b", deviceAppPath);
			} else {
				List<String> instrumentsCommand = new ArrayList<String>();
				instrumentsCommand.add(xcrun);
				instrumentsCommand.add("instruments");
				instrumentsCommand.add("-w");
				instrumentsCommand.add(deployDevice);
				processRunner.runProcess(targetDirectory, instrumentsCommand.toArray(new String[instrumentsCommand.size()]));
				List<String> simctlInstallCommand = new ArrayList<String>();
				simctlInstallCommand.add(xcrun);
				simctlInstallCommand.add("simctl");
				simctlInstallCommand.add("install");
				simctlInstallCommand.add("booted");
				simctlInstallCommand.add(simulatorAppPath);
				processRunner.runProcess(targetDirectory, simctlInstallCommand.toArray(new String[simctlInstallCommand.size()]));
				List<String> simctlLaunchCommand = new ArrayList<String>();
				simctlLaunchCommand.add(xcrun);
				simctlLaunchCommand.add("simctl");
				simctlLaunchCommand.add("launch");
				simctlLaunchCommand.add("booted");
				simctlLaunchCommand.add(bundleId);
				processRunner.runProcess(targetDirectory, simctlLaunchCommand.toArray(new String[simctlLaunchCommand.size()]));
			}
		}
	}

}
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
	@Parameter(alias = "deployToSimulator", property = "xcode.project.deploy", defaultValue = "false", required = false)
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
	@Parameter(alias = "appPath", property = "xcode.project.app.path", defaultValue = "${project.build.directory}/build/${project.artifactId}.app", required = true)
	public String appPath;
	
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
		packageCommand.add(appPath);
		packageCommand.add("-o");
		packageCommand.add(targetDirectory + "/" + ipaName);
		processRunner.runProcess(targetDirectory, packageCommand.toArray(new String[packageCommand.size()]));
		if (deploy) {
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
			simctlInstallCommand.add(appPath);
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
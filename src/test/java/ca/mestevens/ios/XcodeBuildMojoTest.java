package ca.mestevens.ios;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import ca.mestevens.ios.utils.ProcessRunner;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

@Test(groups = "automated")
public class XcodeBuildMojoTest {
	
	private final String projectString = "test-project";
	private final String schemeString = "test-scheme";
	private final String xcodebuild = "xcodebuild";
	private final String artifactName = "test-artifact-name";
	private final String target = "target";
	private final String pbxprojPathString = Thread.currentThread().getContextClassLoader().getResource("project.pbxproj").getPath().toString().replace("/project.pbxproj",  "");
	private MavenProject mockProject;
	private Log mockLog;
	private ProcessRunner mockProcessRunner;
	private XcodeBuildMojo buildMojo;

	@BeforeMethod
	public void setUp() {
		mockProject = mock(MavenProject.class);
		mockLog = mock(Log.class);
		mockProcessRunner = mock(ProcessRunner.class);
		mockProcessRunner.log = mockLog;
		
		buildMojo = new XcodeBuildMojo();
		buildMojo.xcodeProject = projectString;
		buildMojo.xcodeScheme = schemeString;
		buildMojo.artifactName = artifactName;
		buildMojo.project = mockProject;
		buildMojo.processRunner = mockProcessRunner;
		buildMojo.xcodebuild = xcodebuild;
		buildMojo.targetDirectory = target;
	}
	
	@AfterMethod
	public void tearDown() {
		buildMojo = null;
		mockProcessRunner = null;
		mockLog = null;
		mockProject = null;
	}
	
	@Test
	public void testExecuteFrameworkWithDeviceList() throws MojoExecutionException, MojoFailureException {
		when(mockProject.getPackaging()).thenReturn("xcode-framework");
		List<String> simulatorArchs = new ArrayList<String>();
		simulatorArchs.add("i386");
		simulatorArchs.add("x86_64");
		for (String simulatorArch : simulatorArchs) {
			when(mockProcessRunner.runProcess(null, xcodebuild, "-project", projectString, "-scheme", schemeString, "-sdk", "iphonesimulator",
					"-arch", simulatorArch, "CONFIGURATION_BUILD_DIR=target/iphonesimulator-" + simulatorArch, "build")).thenReturn(0);
		}
		List<String> deviceArchs = new ArrayList<String>();
		deviceArchs.add("armv7");
		for (String deviceArch : deviceArchs) {
			when(mockProcessRunner.runProcess(null, xcodebuild, "-project", projectString, "-scheme", schemeString, "-sdk", "iphoneos",
					"-arch", deviceArch, "CONFIGURATION_BUILD_DIR=target/iphoneos-" + deviceArch, "build")).thenReturn(0);
		}
		List<String> lipoCommand = new ArrayList<String>();
		lipoCommand.add("lipo");
		lipoCommand.add("-create");
		lipoCommand.add("-output");
		lipoCommand.add(artifactName);
		for (String simulatorArch : simulatorArchs) {
			lipoCommand.add("iphonesimulator-" + simulatorArch + "/" + artifactName + ".framework/" + artifactName);
		}
		for (String deviceArch : deviceArchs) {
			lipoCommand.add("iphoneos-" + deviceArch + "/" + artifactName + ".framework/" + artifactName);
		}
		when(mockProcessRunner.runProcess(target, lipoCommand.toArray(new String[lipoCommand.size()]))).thenReturn(0);
		when(mockProcessRunner.runProcess(any(String.class), any(String.class))).thenReturn(0);
		buildMojo.deviceArchs = deviceArchs;

		buildMojo.execute();
		verify(mockProcessRunner, times(1)).runProcess(target, lipoCommand.toArray(new String[lipoCommand.size()]));
		for (String simulatorArch : simulatorArchs) {
			verify(mockProcessRunner, times(1)).runProcess(null, xcodebuild, "-project", projectString, "-scheme", schemeString, "-sdk", "iphonesimulator",
					"-arch", simulatorArch, "CONFIGURATION_BUILD_DIR=" + target + "/iphonesimulator-" + simulatorArch, "build");
		}
		for (String deviceArch : deviceArchs) {
			verify(mockProcessRunner, times(1)).runProcess(null, xcodebuild, "-project", projectString, "-scheme", schemeString, "-sdk", "iphoneos",
					"-arch", deviceArch, "CONFIGURATION_BUILD_DIR=" + target + "/iphoneos-" + deviceArch, "build");
		}
	}
	
	@Test
	public void testExecuteFrameworkWithNullDeviceList() throws MojoExecutionException, MojoFailureException {
		when(mockProject.getPackaging()).thenReturn("xcode-framework");
		List<String> simulatorArchs = new ArrayList<String>();
		simulatorArchs.add("i386");
		simulatorArchs.add("x86_64");
		for (String simulatorArch : simulatorArchs) {
			when(mockProcessRunner.runProcess(null, xcodebuild, "-project", projectString, "-scheme", schemeString, "-sdk", "iphonesimulator",
					"-arch", simulatorArch, "CONFIGURATION_BUILD_DIR=target/iphonesimulator-" + simulatorArch, "build")).thenReturn(0);
		}
		List<String> deviceArchs = new ArrayList<String>();
		deviceArchs.add("armv7");
		deviceArchs.add("arm64");
		for (String deviceArch : deviceArchs) {
			when(mockProcessRunner.runProcess(null, xcodebuild, "-project", projectString, "-scheme", schemeString, "-sdk", "iphoneos",
					"-arch", deviceArch, "CONFIGURATION_BUILD_DIR=target/iphoneos-" + deviceArch, "build")).thenReturn(0);
		}

		List<String> lipoCommand = new ArrayList<String>();
		lipoCommand.add("lipo");
		lipoCommand.add("-create");
		lipoCommand.add("-output");
		lipoCommand.add(artifactName);
		for (String simulatorArch : simulatorArchs) {
			lipoCommand.add("iphonesimulator-" + simulatorArch + "/" + artifactName + ".framework/" + artifactName);
		}
		for (String deviceArch : deviceArchs) {
			lipoCommand.add("iphoneos-" + deviceArch + "/" + artifactName + ".framework/" + artifactName);
		}
		when(mockProcessRunner.runProcess(target, lipoCommand.toArray(new String[lipoCommand.size()]))).thenReturn(0);
		when(mockProcessRunner.runProcess(any(String.class), any(String.class))).thenReturn(0);
		buildMojo.deviceArchs = null;

		buildMojo.execute();
		verify(mockProcessRunner, times(1)).runProcess(target, lipoCommand.toArray(new String[lipoCommand.size()]));
		for (String simulatorArch : simulatorArchs) {
			verify(mockProcessRunner, times(1)).runProcess(null, xcodebuild, "-project", projectString, "-scheme", schemeString, "-sdk", "iphonesimulator",
					"-arch", simulatorArch, "CONFIGURATION_BUILD_DIR=" + target + "/iphonesimulator-" + simulatorArch, "build");
		}
		for (String deviceArch : deviceArchs) {
			verify(mockProcessRunner, times(1)).runProcess(null, xcodebuild, "-project", projectString, "-scheme", schemeString, "-sdk", "iphoneos",
					"-arch", deviceArch, "CONFIGURATION_BUILD_DIR=" + target + "/iphoneos-" + deviceArch, "build");
		}
	}
	
	@Test
	public void testExecuteFrameworkWithEmptyDeviceList() throws MojoExecutionException, MojoFailureException {
		when(mockProject.getPackaging()).thenReturn("xcode-framework");
		List<String> simulatorArchs = new ArrayList<String>();
		simulatorArchs.add("i386");
		simulatorArchs.add("x86_64");
		for (String simulatorArch : simulatorArchs) {
			when(mockProcessRunner.runProcess(null, xcodebuild, "-project", projectString, "-scheme", schemeString, "-sdk", "iphonesimulator",
					"-arch", simulatorArch, "CONFIGURATION_BUILD_DIR=target/iphonesimulator-" + simulatorArch, "build")).thenReturn(0);
		}
		List<String> deviceArchs = new ArrayList<String>();
		deviceArchs.add("armv7");
		deviceArchs.add("arm64");
		for (String deviceArch : deviceArchs) {
			when(mockProcessRunner.runProcess(null, xcodebuild, "-project", projectString, "-scheme", schemeString, "-sdk", "iphoneos",
					"-arch", deviceArch, "CONFIGURATION_BUILD_DIR=target/iphoneos-" + deviceArch, "build")).thenReturn(0);
		}

		List<String> lipoCommand = new ArrayList<String>();
		lipoCommand.add("lipo");
		lipoCommand.add("-create");
		lipoCommand.add("-output");
		lipoCommand.add(artifactName);
		for (String simulatorArch : simulatorArchs) {
			lipoCommand.add("iphonesimulator-" + simulatorArch + "/" + artifactName + ".framework/" + artifactName);
		}
		for (String deviceArch : deviceArchs) {
			lipoCommand.add("iphoneos-" + deviceArch + "/" + artifactName + ".framework/" + artifactName);
		}
		when(mockProcessRunner.runProcess(target, lipoCommand.toArray(new String[lipoCommand.size()]))).thenReturn(0);
		when(mockProcessRunner.runProcess(any(String.class), any(String.class))).thenReturn(0);
		buildMojo.deviceArchs = new ArrayList<String>();

		buildMojo.execute();
		verify(mockProcessRunner, times(1)).runProcess(target, lipoCommand.toArray(new String[lipoCommand.size()]));
		for (String simulatorArch : simulatorArchs) {
			verify(mockProcessRunner, times(1)).runProcess(null, xcodebuild, "-project", projectString, "-scheme", schemeString, "-sdk", "iphonesimulator",
					"-arch", simulatorArch, "CONFIGURATION_BUILD_DIR=" + target + "/iphonesimulator-" + simulatorArch, "build");
		}
		for (String deviceArch : deviceArchs) {
			verify(mockProcessRunner, times(1)).runProcess(null, xcodebuild, "-project", projectString, "-scheme", schemeString, "-sdk", "iphoneos",
					"-arch", deviceArch, "CONFIGURATION_BUILD_DIR=" + target + "/iphoneos-" + deviceArch, "build");
		}
	}
	
	@Test(expectedExceptions = MojoFailureException.class)
	public void testExecuteFrameworkErrorBuildingSimulatorFramework() throws MojoFailureException, MojoExecutionException {
		when(mockProject.getPackaging()).thenReturn("xcode-framework");
		List<String> simulatorArchs = new ArrayList<String>();
		simulatorArchs.add("i386");
		simulatorArchs.add("x86_64");
		for (String simulatorArch : simulatorArchs) {
			when(mockProcessRunner.runProcess(null, xcodebuild, "-project", projectString, "-scheme", schemeString, "-sdk", "iphonesimulator",
					"-arch", simulatorArch, "CONFIGURATION_BUILD_DIR=target/iphonesimulator-" + simulatorArch, "build")).thenReturn(1);
		}
		
		buildMojo.execute();
	}
	
	@Test(expectedExceptions = MojoFailureException.class)
	public void testExecuteFrameworkErrorBuildingDeviceFramework() throws MojoExecutionException, MojoFailureException {
		when(mockProject.getPackaging()).thenReturn("xcode-framework");
		List<String> simulatorArchs = new ArrayList<String>();
		simulatorArchs.add("i386");
		simulatorArchs.add("x86_64");
		for (String simulatorArch : simulatorArchs) {
			when(mockProcessRunner.runProcess(null, xcodebuild, "-project", projectString, "-scheme", schemeString, "-sdk", "iphonesimulator",
					"-arch", simulatorArch, "CONFIGURATION_BUILD_DIR=target/iphonesimulator-" + simulatorArch, "build")).thenReturn(0);
		}
		List<String> deviceArchs = new ArrayList<String>();
		deviceArchs.add("armv7");
		deviceArchs.add("arm64");
		for (String deviceArch : deviceArchs) {
			when(mockProcessRunner.runProcess(null, xcodebuild, "-project", projectString, "-scheme", schemeString, "-sdk", "iphoneos",
					"-arch", deviceArch, "CONFIGURATION_BUILD_DIR=target/iphoneos-" + deviceArch, "build")).thenReturn(1);
		}
		buildMojo.deviceArchs = new ArrayList<String>();

		buildMojo.execute();
	}
	
	@Test(expectedExceptions = MojoFailureException.class)
	public void testExecuteFrameworkErrorLinkingFramework() throws MojoExecutionException, MojoFailureException {
		when(mockProject.getPackaging()).thenReturn("xcode-framework");
		List<String> simulatorArchs = new ArrayList<String>();
		simulatorArchs.add("i386");
		simulatorArchs.add("x86_64");
		for (String simulatorArch : simulatorArchs) {
			when(mockProcessRunner.runProcess(null, xcodebuild, "-project", projectString, "-scheme", schemeString, "-sdk", "iphonesimulator",
					"-arch", simulatorArch, "CONFIGURATION_BUILD_DIR=target/iphonesimulator-" + simulatorArch, "build")).thenReturn(0);
		}
		List<String> deviceArchs = new ArrayList<String>();
		deviceArchs.add("armv7");
		for (String deviceArch : deviceArchs) {
			when(mockProcessRunner.runProcess(null, xcodebuild, "-project", projectString, "-scheme", schemeString, "-sdk", "iphoneos",
					"-arch", deviceArch, "CONFIGURATION_BUILD_DIR=target/iphoneos-" + deviceArch, "build")).thenReturn(0);
		}
		List<String> lipoCommand = new ArrayList<String>();
		lipoCommand.add("lipo");
		lipoCommand.add("-create");
		lipoCommand.add("-output");
		lipoCommand.add(artifactName);
		for (String simulatorArch : simulatorArchs) {
			lipoCommand.add("iphonesimulator-" + simulatorArch + "/" + artifactName + ".framework/" + artifactName);
		}
		for (String deviceArch : deviceArchs) {
			lipoCommand.add("iphoneos-" + deviceArch + "/" + artifactName + ".framework/" + artifactName);
		}
		when(mockProcessRunner.runProcess(target, lipoCommand.toArray(new String[lipoCommand.size()]))).thenReturn(1);
		when(mockProcessRunner.runProcess(any(String.class), any(String.class))).thenReturn(0);
		buildMojo.deviceArchs = deviceArchs;

		buildMojo.execute();
	}
	
	@Test
	public void testExecuteFrameworkWithDeviceListFromXcodeProj() throws MojoExecutionException, MojoFailureException {
		when(mockProject.getPackaging()).thenReturn("xcode-framework");
		buildMojo.xcodeProject = pbxprojPathString;
		List<String> simulatorArchs = new ArrayList<String>();
		simulatorArchs.add("i386");
		simulatorArchs.add("x86_64");
		for (String simulatorArch : simulatorArchs) {
			when(mockProcessRunner.runProcess(null, xcodebuild, "-project", pbxprojPathString, "-scheme", schemeString, "-sdk", "iphonesimulator",
					"-arch", simulatorArch, "CONFIGURATION_BUILD_DIR=target/iphonesimulator-" + simulatorArch, "build")).thenReturn(0);
		}
		List<String> deviceArchs = new ArrayList<String>();
		deviceArchs.add("arm64");
		for (String deviceArch : deviceArchs) {
			when(mockProcessRunner.runProcess(null, xcodebuild, "-project", pbxprojPathString, "-scheme", schemeString, "-sdk", "iphoneos",
					"-arch", deviceArch, "CONFIGURATION_BUILD_DIR=target/iphoneos-" + deviceArch, "build")).thenReturn(0);
		}
		List<String> lipoCommand = new ArrayList<String>();
		lipoCommand.add("lipo");
		lipoCommand.add("-create");
		lipoCommand.add("-output");
		lipoCommand.add(artifactName);
		for (String simulatorArch : simulatorArchs) {
			lipoCommand.add("iphonesimulator-" + simulatorArch + "/" + artifactName + ".framework/" + artifactName);
		}
		for (String deviceArch : deviceArchs) {
			lipoCommand.add("iphoneos-" + deviceArch + "/" + artifactName + ".framework/" + artifactName);
		}
		when(mockProcessRunner.runProcess(target, lipoCommand.toArray(new String[lipoCommand.size()]))).thenReturn(0);
		when(mockProcessRunner.runProcess(any(String.class), any(String.class))).thenReturn(0);
		buildMojo.deviceArchs = null;

		buildMojo.execute();
		verify(mockProcessRunner, times(1)).runProcess(target, lipoCommand.toArray(new String[lipoCommand.size()]));
		for (String simulatorArch : simulatorArchs) {
			verify(mockProcessRunner, times(1)).runProcess(null, xcodebuild, "-project", pbxprojPathString, "-scheme", schemeString, "-sdk", "iphonesimulator",
					"-arch", simulatorArch, "CONFIGURATION_BUILD_DIR=" + target + "/iphonesimulator-" + simulatorArch, "build");
		}
		for (String deviceArch : deviceArchs) {
			verify(mockProcessRunner, times(1)).runProcess(null, xcodebuild, "-project", pbxprojPathString, "-scheme", schemeString, "-sdk", "iphoneos",
					"-arch", deviceArch, "CONFIGURATION_BUILD_DIR=" + target + "/iphoneos-" + deviceArch, "build");
		}
	}
	
	@Test
	public void testExecuteLibraryWithDeviceList() throws MojoExecutionException, MojoFailureException {
		when(mockProject.getPackaging()).thenReturn("xcode-library");
		List<String> simulatorArchs = new ArrayList<String>();
		simulatorArchs.add("i386");
		simulatorArchs.add("x86_64");
		for (String simulatorArch : simulatorArchs) {
			when(mockProcessRunner.runProcess(null, xcodebuild, "-project", projectString, "-scheme", schemeString, "-sdk", "iphonesimulator",
					"-arch", simulatorArch, "CONFIGURATION_BUILD_DIR=target/iphonesimulator-" + simulatorArch, "build")).thenReturn(0);
		}
		List<String> deviceArchs = new ArrayList<String>();
		deviceArchs.add("armv7");
		for (String deviceArch : deviceArchs) {
			when(mockProcessRunner.runProcess(null, xcodebuild, "-project", projectString, "-scheme", schemeString, "-sdk", "iphoneos",
					"-arch", deviceArch, "CONFIGURATION_BUILD_DIR=target/iphoneos-" + deviceArch, "build")).thenReturn(0);
		}
		List<String> lipoCommand = new ArrayList<String>();
		lipoCommand.add("lipo");
		lipoCommand.add("-create");
		lipoCommand.add("-output");
		lipoCommand.add("lib" + artifactName + ".a");
		for (String simulatorArch : simulatorArchs) {
			lipoCommand.add("iphonesimulator-" + simulatorArch + "/lib" + artifactName + ".a");
		}
		for (String deviceArch : deviceArchs) {
			lipoCommand.add("iphoneos-" + deviceArch + "/lib" + artifactName + ".a");
		}
		when(mockProcessRunner.runProcess(target, lipoCommand.toArray(new String[lipoCommand.size()]))).thenReturn(0);
		when(mockProcessRunner.runProcess(any(String.class), any(String.class))).thenReturn(0);
		buildMojo.deviceArchs = deviceArchs;

		buildMojo.execute();
		verify(mockProcessRunner, times(1)).runProcess(target, lipoCommand.toArray(new String[lipoCommand.size()]));
		for (String simulatorArch : simulatorArchs) {
			verify(mockProcessRunner, times(1)).runProcess(null, xcodebuild, "-project", projectString, "-scheme", schemeString, "-sdk", "iphonesimulator",
					"-arch", simulatorArch, "CONFIGURATION_BUILD_DIR=" + target + "/iphonesimulator-" + simulatorArch, "build");
		}
		for (String deviceArch : deviceArchs) {
			verify(mockProcessRunner, times(1)).runProcess(null, xcodebuild, "-project", projectString, "-scheme", schemeString, "-sdk", "iphoneos",
					"-arch", deviceArch, "CONFIGURATION_BUILD_DIR=" + target + "/iphoneos-" + deviceArch, "build");
		}
	}
	
}

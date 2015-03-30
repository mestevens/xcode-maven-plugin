package ca.mestevens.ios;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import ca.mestevens.ios.utils.ProcessRunner;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@Test(groups = "automated")
public class XcodeTestMojoTest {
	
	private final String projectStringPath = Thread.currentThread().getContextClassLoader().getResource("test.xcodeproj").getPath().toString() + "/project.pbxproj";
	private final String projectString = projectStringPath.substring(0, projectStringPath.lastIndexOf('/'));
	private final String xcodebuild = "xcodebuild";
	private final String xcodeScheme = "test-scheme";
	private ProcessRunner mockProcessRunner;
	private Log mockLog;
	private XcodeTestMojo testMojo;

	@BeforeMethod
	public void setUp() {
		mockLog = mock(Log.class);
		mockProcessRunner = mock(ProcessRunner.class);
		mockProcessRunner.log = mockLog;
		testMojo = new XcodeTestMojo();
		testMojo.processRunner = mockProcessRunner;
		
		testMojo.xcodeProject = projectString;
		testMojo.xcodebuild = xcodebuild;
		testMojo.xcodeScheme = xcodeScheme;
	}
	
	@AfterMethod
	public void tearDown() {
		testMojo = null;
		mockProcessRunner = null;
		mockLog = null;
	}
	
	@Test
	public void testExecuteTestsSkipTests() throws MojoExecutionException, MojoFailureException {
		testMojo.skipTests = true;
		
		testMojo.execute();
		verify(mockProcessRunner, times(0)).runProcess(any(String.class), any(String.class));
	}
	
	@Test
	public void testExecuteTestsNoSimulators() throws MojoExecutionException, MojoFailureException {
		List<String> testCommands = new ArrayList<String>();
		testCommands.add(xcodebuild);
		testCommands.add("-project");
		testCommands.add(projectString);
		testCommands.add("-scheme");
		testCommands.add(xcodeScheme);
		testCommands.add("-destination");
		testCommands.add("platform=iOS Simulator,name=iPhone 6");
		testCommands.add("test");
		when(mockProcessRunner.runProcess(null, testCommands.toArray(new String[testCommands.size()]))).thenReturn(0);
		
		testMojo.execute();
		verify(mockProcessRunner, times(1)).runProcess(null, testCommands.toArray(new String[testCommands.size()]));
	}
	
	@Test
	public void testExecuteTestsSimulatorList() throws MojoExecutionException, MojoFailureException {
		List<String> simulatorList = new ArrayList<String>();
		simulatorList.add("iPhone 6");
		simulatorList.add("iPhone 5");
		testMojo.testSimulators = simulatorList;
		List<String> testCommands = new ArrayList<String>();
		testCommands.add(xcodebuild);
		testCommands.add("-project");
		testCommands.add(projectString);
		testCommands.add("-scheme");
		testCommands.add(xcodeScheme);
		for (String simulator : simulatorList) {
			testCommands.add("-destination");
			testCommands.add("platform=iOS Simulator,name=" + simulator);
		}
		testCommands.add("test");
		when(mockProcessRunner.runProcess(null, testCommands.toArray(new String[testCommands.size()]))).thenReturn(0);
		
		testMojo.execute();
		verify(mockProcessRunner, times(1)).runProcess(null, testCommands.toArray(new String[testCommands.size()]));
	}
	
	@Test
	public void testExecuteTestsSimulatorIgnoreFailures() throws MojoFailureException, MojoExecutionException {
		testMojo.ignoreFailures = true;
		List<String> simulatorList = new ArrayList<String>();
		simulatorList.add("iPhone 6");
		simulatorList.add("iPhone 5");
		testMojo.testSimulators = simulatorList;
		List<String> testCommands = new ArrayList<String>();
		testCommands.add(xcodebuild);
		testCommands.add("-project");
		testCommands.add(projectString);
		testCommands.add("-scheme");
		testCommands.add(xcodeScheme);
		for (String simulator : simulatorList) {
			testCommands.add("-destination");
			testCommands.add("platform=iOS Simulator,name=" + simulator);
		}
		testCommands.add("test");
		when(mockProcessRunner.runProcess(null, testCommands.toArray(new String[testCommands.size()]))).thenReturn(1);
		
		testMojo.execute();
		verify(mockProcessRunner, times(1)).runProcess(null, testCommands.toArray(new String[testCommands.size()]));
	}
	
	@Test(expectedExceptions = MojoFailureException.class)
	public void testExecuteTestsSimulatorDoNotIgnoreFailures() throws MojoFailureException, MojoExecutionException {
		List<String> simulatorList = new ArrayList<String>();
		simulatorList.add("iPhone 6");
		simulatorList.add("iPhone 5");
		testMojo.testSimulators = simulatorList;
		List<String> testCommands = new ArrayList<String>();
		testCommands.add(xcodebuild);
		testCommands.add("-project");
		testCommands.add(projectString);
		testCommands.add("-scheme");
		testCommands.add(xcodeScheme);
		for (String simulator : simulatorList) {
			testCommands.add("-destination");
			testCommands.add("platform=iOS Simulator,name=" + simulator);
		}
		testCommands.add("test");
		when(mockProcessRunner.runProcess(null, testCommands.toArray(new String[testCommands.size()]))).thenReturn(1);
		
		testMojo.execute();
	}
	
}
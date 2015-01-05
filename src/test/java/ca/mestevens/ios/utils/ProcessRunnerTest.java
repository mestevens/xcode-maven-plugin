package ca.mestevens.ios.utils;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import static org.testng.Assert.assertEquals;

@Test(groups = "automated")
public class ProcessRunnerTest {
	
	Log mockLog;
	ProcessRunner processRunner;

	@BeforeMethod
	public void setUp() {
		mockLog = mock(Log.class);
		processRunner = new ProcessRunner(mockLog);
	}
	
	@AfterMethod
	public void tearDown() {
		processRunner = null;
		mockLog = null;
	}
	
	@Test
	public void testHelloWorld() throws MojoFailureException {
		processRunner.runProcess(null, "echo", "hello world");
		verify(mockLog).info("hello world");
		verify(mockLog, times(1)).info(any(String.class));
	}
	
	@Test
	public void testDifferentDirectory() throws MojoFailureException {
		processRunner.runProcess("/", "pwd");
		verify(mockLog).info("/");
		verify(mockLog, times(1)).info(any(String.class));
		processRunner.runProcess("/usr", "pwd");
		verify(mockLog).info("/usr");
		verify(mockLog, times(2)).info(any(String.class));
	}
	
	@Test(expectedExceptions = MojoFailureException.class)
	public void testNonZeroReturn() throws MojoFailureException {
		//To return a non-zero exit code
		processRunner.runProcess(null, "kill", "kjdfajjkdfsajfds");
	}
	
	@Test
	public void testNonZeroReturnIgnore() throws MojoFailureException {
		processRunner = new ProcessRunner(mockLog, false);
		int returnCode = processRunner.runProcess(null, "kill", "kjdfajjkdfsajfds");
		assertEquals(returnCode, 1);
	}
	
}

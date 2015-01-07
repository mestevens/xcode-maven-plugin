package ca.mestevens.ios;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.mockito.Matchers;
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
public class KeychainMojoTest {
	
	private final String security = "security";
	private final String keychainPath = "test-path";
	private final String keychainPassword = "test-password";
	private ProcessRunner mockProcessRunner;
	private Log mockLog;
	private KeychainMojo keychainMojo;

	@BeforeMethod
	public void setUp() {
		mockProcessRunner = mock(ProcessRunner.class);
		mockLog = mock(Log.class);
		mockProcessRunner.log = mockLog;
		keychainMojo = new KeychainMojo();
		keychainMojo.processRunner = mockProcessRunner;
		keychainMojo.security = security;
		keychainMojo.keychain = keychainPath;
		keychainMojo.keychainPassword = keychainPassword;
	}
	
	@AfterMethod
	public void tearDown() {
		keychainMojo = null;
		mockProcessRunner = null;
		mockLog = null;
	}
	
	@Test
	public void testExecuteAllFine() throws MojoFailureException, MojoExecutionException {
		when(mockProcessRunner.runProcess(null, security, "list-keychains", "-s", keychainPath)).thenReturn(0);
		when(mockProcessRunner.runProcess(null, security, "default-keychain", "-d", "user", "-s", keychainPath)).thenReturn(0);
		when(mockProcessRunner.runProcess(null, security, "unlock-keychain", "-p", keychainPassword, keychainPath)).thenReturn(0);
		when(mockProcessRunner.runProcess(null, security, "show-keychain-info", keychainPath)).thenReturn(0);
		
		keychainMojo.execute();
		
		verify(mockProcessRunner, times(1)).runProcess(null, security, "list-keychains", "-s", keychainPath);
		verify(mockProcessRunner, times(1)).runProcess(null, security, "default-keychain", "-d", "user", "-s", keychainPath);
		verify(mockProcessRunner, times(1)).runProcess(null, security, "unlock-keychain", "-p", keychainPassword, keychainPath);
		verify(mockProcessRunner, times(1)).runProcess(null, security, "show-keychain-info", keychainPath);
	}
	
	@Test
	public void testExecuteNoPath() throws MojoExecutionException, MojoFailureException {
		keychainMojo.keychain = null;
		
		keychainMojo.execute();
		
		verify(mockProcessRunner, times(0)).runProcess(any(String.class), Matchers.<String>anyVararg());
	}
	
	@Test
	public void testExecuteEmptyPath() throws MojoExecutionException, MojoFailureException {
		keychainMojo.keychain = "";
		
		keychainMojo.execute();
		
		verify(mockProcessRunner, times(0)).runProcess(any(String.class), Matchers.<String>anyVararg());
	}
	
	@Test
	public void testExecuteNoPassword() throws MojoExecutionException, MojoFailureException {
		keychainMojo.keychainPassword = null;
		
		keychainMojo.execute();
		
		verify(mockProcessRunner, times(0)).runProcess(any(String.class), Matchers.<String>anyVararg());
	}
	
	@Test
	public void testExecuteEmptyPassword() throws MojoExecutionException, MojoFailureException {
		keychainMojo.keychainPassword = "";
		
		keychainMojo.execute();
		
		verify(mockProcessRunner, times(0)).runProcess(any(String.class), Matchers.<String>anyVararg());
	}
	
}
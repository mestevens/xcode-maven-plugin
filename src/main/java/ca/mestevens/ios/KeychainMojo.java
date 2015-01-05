package ca.mestevens.ios;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import ca.mestevens.ios.utils.ProcessRunner;

@Mojo(name = "set-keychain", defaultPhase = LifecyclePhase.INITIALIZE)
public class KeychainMojo extends AbstractMojo {

	@Parameter(property = "security.path", defaultValue = "/usr/bin/security", readonly = true, required = true)
	public String security;
	
	@Parameter(alias = "keychain.path", property = "keychain.path", readonly = true, required = false)
	public String keychain;
	
	@Parameter(alias = "keychain.password", property = "keychain.password", readonly = true, required = false)
	public String keychainPassword;
	
	public ProcessRunner processRunner;
	
	public KeychainMojo() {
		processRunner = new ProcessRunner(getLog());
	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		if (keychain == null || keychain.isEmpty() || keychainPassword == null || keychainPassword.isEmpty()) {
			return;
		}
		getLog().info(String.format("%s list-keychains -s %s", security, keychain));
		processRunner.runProcess(null, security, "list-keychains", "-s", keychain);
		getLog().info(String.format("%s default-keychain -d user -s %s", security, keychain));
		processRunner.runProcess(null, security, "default-keychain", "-d", "user", "-s", keychain);
		String obfuscatedPassword = "";
		for (int i = 0; i < keychainPassword.length(); i++) {
			obfuscatedPassword += "*";
		}
		getLog().info(String.format("%s unlock-keychain -p %s", security, obfuscatedPassword));
		processRunner.runProcess(null, security, "unlock-keychain", "-p", keychainPassword, keychain);
		getLog().info(String.format("%s show-keychain-info %s", security, keychain));
		processRunner.runProcess(null, security, "show-keychain-info", keychain);
	}
	
}
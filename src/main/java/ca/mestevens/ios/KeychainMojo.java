package ca.mestevens.ios;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import ca.mestevens.ios.utils.ProcessRunner;

/**
 * Goal to set your keychain if needed.
 */
@Mojo(name = "set-keychain", defaultPhase = LifecyclePhase.INITIALIZE)
public class KeychainMojo extends AbstractMojo {

	@Parameter(alias = "securityPath", property = "security.path", defaultValue = "/usr/bin/security", readonly = true, required = true)
	public String security;
	
	@Parameter(alias = "keychainPath", property = "keychain.path", readonly = true, required = false)
	public String keychain;
	
	@Parameter(alias = "keychainPassword", property = "keychain.password", readonly = true, required = false)
	public String keychainPassword;
	
	public ProcessRunner processRunner;
	
	public KeychainMojo() {
		processRunner = new ProcessRunner(getLog());
	}

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		//If a keychain path and password aren't specified, try and get them from the environment
		if (keychain == null || keychain.isEmpty()) {
			keychain = System.getenv("KEYCHAIN_PATH");
		}
		if (keychainPassword == null || keychainPassword.isEmpty()) {
			keychainPassword = System.getenv("KEYCHAIN_PASSWORD");
		}
		//We only need to check nulls here, because the empty cases are caught above.
		if (keychain == null || keychainPassword == null) {
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
package ca.mestevens.ios;

import java.io.File;
import java.io.IOException;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;

import ca.mestevens.ios.utils.ProcessRunner;

/**
 * Goal which generates your framework dependencies in the target directory.
 *
 * @goal xcode-package-framework
 * 
 * @phase package
 */
public class XcodePackageMojo extends AbstractMojo {
	
	/**
	 * @parameter property="project"
	 * @readonly
	 * @required
	 */
	public MavenProject project;
	
	/**
	 * @parameter property="project.build.directory"
	 * @readonly
	 * @required
	 */
	public String targetDirectory;
	
	/**
	 * @parameter property="xcode.framework.name" default-value="${project.artifactId}"
	 * @readonly
	 * @required
	 */
	public String frameworkName;

	public void execute() throws MojoExecutionException, MojoFailureException {		
		try {
			ProcessRunner processRunner = new ProcessRunner(getLog());
			File zippedFile = new File(targetDirectory + "/" + frameworkName + ".xcode-framework");
			File frameworkArtifact = new File(targetDirectory + "/" + frameworkName + ".framework");
			if (zippedFile.exists()) {
				FileUtils.deleteDirectory(zippedFile);
			}
			int returnValue = processRunner.runProcess("zip", "-r", zippedFile.getAbsolutePath(), frameworkArtifact.getAbsolutePath());
			
			if (returnValue != 0) {
				getLog().error("Could not zip file: " + frameworkName);
				throw new MojoFailureException("Could not zip file: " + frameworkName);
			}
		} catch (IOException e) {
			getLog().error("Error deleting directory");
			getLog().error(e.getMessage());
			throw new MojoFailureException("Error deleting directory");
		}
	}

}

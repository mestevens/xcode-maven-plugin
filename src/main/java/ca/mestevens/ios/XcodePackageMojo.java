package ca.mestevens.ios;

import java.io.File;
import java.io.IOException;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.ZipParameters;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;

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
			ZipFile zippedFile = new ZipFile(targetDirectory + "/" + frameworkName + ".xcode-framework");
			File frameworkArtifact = new File(targetDirectory + "/" + frameworkName + ".framework");
			if (zippedFile.getFile().exists()) {
				FileUtils.deleteDirectory(zippedFile.getFile());
			}
			zippedFile.createZipFile(frameworkArtifact, new ZipParameters());
			project.getArtifact().setFile(zippedFile.getFile());
		} catch (ZipException e) {
			getLog().error("Could not zip file: " + frameworkName);
			getLog().error(e.getMessage());
			throw new MojoFailureException("Could not zip file: " + frameworkName);
		} catch (IOException e) {
			getLog().error("Error deleting directory");
			getLog().error(e.getMessage());
			throw new MojoFailureException("Error deleting directory");
		}
	}

}

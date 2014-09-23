package ca.mestevens.xcode_maven_plugin;

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

	public void execute() throws MojoExecutionException, MojoFailureException {
		try {
			ZipFile zippedFile = new ZipFile(targetDirectory + "/xcode_maven_plugin_test.xcode-framework");
			File frameworkArtifact = new File(targetDirectory + "/xcode_maven_plugin_test.framework");
			if (zippedFile.getFile().exists()) {
				FileUtils.deleteDirectory(zippedFile.getFile());
			}
			zippedFile.createZipFile(frameworkArtifact, new ZipParameters());
			project.getArtifact().setFile(zippedFile.getFile());
		} catch (ZipException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}

}

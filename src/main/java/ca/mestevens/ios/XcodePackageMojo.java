package ca.mestevens.ios;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
			String packaging = project.getPackaging();
			String packagedFileName = frameworkName + "." + packaging;
			File zippedFile = new File(targetDirectory + "/" + packagedFileName);
			List<String> inputFiles = new ArrayList<String>();
			if (packaging.equals("xcode-framework")) {
				inputFiles.add(frameworkName + ".framework");
			} else {
				inputFiles.add("lib" + frameworkName + ".a");
				inputFiles.add("headers");
			}
			if (zippedFile.exists()) {
				FileUtils.deleteDirectory(zippedFile);
			}
			List<String> zipCommand = new ArrayList<String>();
			zipCommand.add("zip");
			zipCommand.add("-r");
			zipCommand.add(packagedFileName);
			for(String inputFile : inputFiles) {
				zipCommand.add(inputFile);
			}
			int returnValue = processRunner.runProcess(targetDirectory, zipCommand.toArray(new String[zipCommand.size()]));

			if (returnValue != 0) {
				getLog().error("Could not zip file: " + frameworkName);
				throw new MojoFailureException("Could not zip file: " + frameworkName);
			}
			
			project.getArtifact().setFile(zippedFile);
		} catch (IOException e) {
			getLog().error("Error deleting directory");
			getLog().error(e.getMessage());
			throw new MojoFailureException("Error deleting directory");
		}
	}

}

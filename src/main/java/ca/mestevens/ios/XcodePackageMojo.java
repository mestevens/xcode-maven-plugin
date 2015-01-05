package ca.mestevens.ios;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;

import ca.mestevens.ios.utils.ProcessRunner;

/**
 * Goal which generates your framework dependencies in the target directory.
 */
@Mojo(name = "xcode-package-framework", defaultPhase = LifecyclePhase.PACKAGE)
public class XcodePackageMojo extends AbstractMojo {
	
	@Parameter(property = "project", readonly = true, required = true)
	public MavenProject project;
	
	@Parameter(property = "project.build.directory", readonly = true, required = true)
	public String targetDirectory;
	
	/**
	 * The name of the framework/artifact
	 */
	@Parameter(property = "xcode.framework.name", defaultValue = "${project.artifactId}", readonly = true, required = true)
	public String frameworkName;
	
	public ProcessRunner processRunner;
	
	public XcodePackageMojo() {
		this.processRunner = new ProcessRunner(getLog());
	}

	public void execute() throws MojoExecutionException, MojoFailureException {		
		try {
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

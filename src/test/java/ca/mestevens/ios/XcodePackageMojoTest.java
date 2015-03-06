package ca.mestevens.ios;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import ca.mestevens.ios.utils.ProcessRunner;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertFalse;

@Test(groups = "automated")
public class XcodePackageMojoTest {
	
	private MavenProject mockProject;
	private Log mockLog;
	private ProcessRunner mockProcessRunner;
	private final String targetDirectory = "target";
	private final String artifactName = "test-name";
	private XcodePackageMojo packageMojo;

	@BeforeMethod
	public void setUp() {
		mockProject = mock(MavenProject.class);
		mockLog = mock(Log.class);
		mockProcessRunner = mock(ProcessRunner.class);
		packageMojo = new XcodePackageMojo();
		mockProcessRunner.log = mockLog;
		packageMojo.processRunner = mockProcessRunner;
		packageMojo.project = mockProject;
		packageMojo.targetDirectory = targetDirectory;
		packageMojo.artifactName = artifactName;
	}
	
	@AfterMethod
	public void tearDown() {
		packageMojo = null;
		mockProcessRunner = null;
		mockLog = null;
		mockProject = null;
	}
	
	@Test
	public void testExecuteFrameworkPackage() throws MojoFailureException, MojoExecutionException {
		when(mockProject.getPackaging()).thenReturn("xcode-dynamic-framework");
		List<String> zipCommand = new ArrayList<String>();
		zipCommand.add("zip");
		zipCommand.add("-r");
		zipCommand.add(artifactName + ".xcode-dynamic-framework");
		zipCommand.add(artifactName + ".framework");
		when(mockProcessRunner.runProcess(targetDirectory, zipCommand.toArray(new String[zipCommand.size()]))).thenReturn(0);
		Artifact mockArtifact = mock(Artifact.class);
		when(mockProject.getArtifact()).thenReturn(mockArtifact);
		
		packageMojo.execute();
		verify(mockProcessRunner, times(1)).runProcess(targetDirectory, zipCommand.toArray(new String[zipCommand.size()]));
		verify(mockArtifact, times(1)).setFile(new File(targetDirectory + "/" + artifactName + ".xcode-dynamic-framework"));
	}
	
	@Test(expectedExceptions = MojoFailureException.class)
	public void testExecuteFrameworkPackageThrowsErrorOnZip() throws MojoFailureException, MojoExecutionException {
		when(mockProject.getPackaging()).thenReturn("xcode-dynamic-framework");
		List<String> zipCommand = new ArrayList<String>();
		zipCommand.add("zip");
		zipCommand.add("-r");
		zipCommand.add(artifactName + ".xcode-dynamic-framework");
		zipCommand.add(artifactName + ".framework");
		when(mockProcessRunner.runProcess(targetDirectory, zipCommand.toArray(new String[zipCommand.size()]))).thenReturn(1);
		
		packageMojo.execute();
	}
	
	@Test
	public void testExecuteFrameworkPackageWithExistingZippedFile() throws MojoFailureException, MojoExecutionException {
		when(mockProject.getPackaging()).thenReturn("xcode-dynamic-framework");
		List<String> zipCommand = new ArrayList<String>();
		zipCommand.add("zip");
		zipCommand.add("-r");
		zipCommand.add(artifactName + ".xcode-dynamic-framework");
		zipCommand.add(artifactName + ".framework");
		when(mockProcessRunner.runProcess(targetDirectory, zipCommand.toArray(new String[zipCommand.size()]))).thenReturn(0);
		Artifact mockArtifact = mock(Artifact.class);
		when(mockProject.getArtifact()).thenReturn(mockArtifact);
		File zippedFile = new File(targetDirectory + "/" + artifactName + ".xcode-dynamic-framework");
		zippedFile.mkdir();
		assertTrue(zippedFile.exists());
		
		packageMojo.execute();
		verify(mockProcessRunner, times(1)).runProcess(targetDirectory, zipCommand.toArray(new String[zipCommand.size()]));
		verify(mockArtifact, times(1)).setFile(zippedFile);
		assertFalse(zippedFile.exists());
	}
	
	@Test
	public void testExecuteLibraryPackage() throws MojoFailureException, MojoExecutionException {
		when(mockProject.getPackaging()).thenReturn("xcode-library");
		List<String> zipCommand = new ArrayList<String>();
		zipCommand.add("zip");
		zipCommand.add("-r");
		zipCommand.add(artifactName + ".xcode-library");
		zipCommand.add("lib" + artifactName + ".a");
		zipCommand.add("headers");
		when(mockProcessRunner.runProcess(targetDirectory, zipCommand.toArray(new String[zipCommand.size()]))).thenReturn(0);
		Artifact mockArtifact = mock(Artifact.class);
		when(mockProject.getArtifact()).thenReturn(mockArtifact);
		
		packageMojo.execute();
		verify(mockProcessRunner, times(1)).runProcess(targetDirectory, zipCommand.toArray(new String[zipCommand.size()]));
		verify(mockArtifact, times(1)).setFile(new File(targetDirectory + "/" + artifactName + ".xcode-library"));
	}
	
}
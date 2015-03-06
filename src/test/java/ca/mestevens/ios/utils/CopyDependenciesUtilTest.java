package ca.mestevens.ios.utils;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.assertEquals;

@Test(groups = "automated")
public class CopyDependenciesUtilTest {
	
	public MavenProject mockMavenProject;
	public Log mockLog;
	public ProcessRunner mockProcessRunner;
	public CopyDependenciesUtil copyDependenciesUtil;
	
	@BeforeMethod
	public void setUp() throws MojoFailureException {
		mockMavenProject = mock(MavenProject.class);
		mockLog = mock(Log.class);
		mockProcessRunner = mock(ProcessRunner.class);
		copyDependenciesUtil = new CopyDependenciesUtil(mockMavenProject, mockLog, mockProcessRunner);
		
		Build mockBuild = mock(Build.class);
		when(mockMavenProject.getBuild()).thenReturn(mockBuild);
		when(mockBuild.getDirectory()).thenReturn("/test-directory");
		
		when(mockProcessRunner.runProcess(null, "unzip", "frameworkAbsPath", "-d", "/test-directory/xcode-dependencies/frameworks/com.test.framework/framework")).thenReturn(0);
		when(mockProcessRunner.runProcess(null, "unzip", "libraryAbsPath", "-d", "/test-directory/xcode-dependencies/libraries/com.test.library/library")).thenReturn(0);
	}
	
	@AfterMethod
	public void tearDown() {
		copyDependenciesUtil = null;
		mockProcessRunner = null;
		mockLog = null;
		mockMavenProject = null;
	}
	
	@Test
	public void testArtifactWithNullType() throws MojoFailureException {
		Set<Artifact> mockArtifacts = new HashSet<Artifact>();
		Artifact mockArtifact = mock(Artifact.class);
		mockArtifacts.add(mockArtifact);
		
		when(mockMavenProject.getArtifacts()).thenReturn(mockArtifacts);
		when(mockArtifact.getType()).thenReturn(null);
		
		List<File> dependencies = copyDependenciesUtil.copyDependencies(JavaScopes.COMPILE);
		assertTrue(dependencies.isEmpty());
		
		verify(mockArtifact, times(0)).getFile();
	}
	
	@Test
	public void testArtifactWithJarType() throws MojoFailureException {
		Set<Artifact> mockArtifacts = new HashSet<Artifact>();
		Artifact mockArtifact = mock(Artifact.class);
		mockArtifacts.add(mockArtifact);
		
		when(mockMavenProject.getArtifacts()).thenReturn(mockArtifacts);
		when(mockArtifact.getType()).thenReturn("jar");
		
		List<File> dependencies = copyDependenciesUtil.copyDependencies(JavaScopes.COMPILE);
		assertTrue(dependencies.isEmpty());
		
		verify(mockArtifact, times(0)).getFile();
	}
	
	@Test
	public void testCopyDependencies() throws MojoFailureException {
		Set<Artifact> mockArtifacts = new HashSet<Artifact>();
		Artifact mockFrameworkArtifact = mock(Artifact.class);
		Artifact mockLibraryArtifact = mock(Artifact.class);
		mockArtifacts.add(mockFrameworkArtifact);
		mockArtifacts.add(mockLibraryArtifact);
		File mockFrameworkFile = mock(File.class);
		File mockLibraryFile = mock(File.class);
		when(mockFrameworkArtifact.getType()).thenReturn("xcode-dynamic-framework");
		when(mockLibraryArtifact.getType()).thenReturn("xcode-library");
		when(mockFrameworkArtifact.getFile()).thenReturn(mockFrameworkFile);
		when(mockLibraryArtifact.getFile()).thenReturn(mockLibraryFile);
		when(mockFrameworkArtifact.getGroupId()).thenReturn("com.test.framework");
		when(mockLibraryArtifact.getGroupId()).thenReturn("com.test.library");
		when(mockFrameworkArtifact.getArtifactId()).thenReturn("framework");
		when(mockLibraryArtifact.getArtifactId()).thenReturn("library");
		when(mockFrameworkFile.exists()).thenReturn(true);
		when(mockFrameworkFile.delete()).thenReturn(true);
		when(mockFrameworkFile.getAbsolutePath()).thenReturn("frameworkAbsPath");
		when(mockLibraryFile.exists()).thenReturn(true);
		when(mockLibraryFile.delete()).thenReturn(true);
		when(mockLibraryFile.getAbsolutePath()).thenReturn("libraryAbsPath");
		
		when(mockMavenProject.getArtifacts()).thenReturn(mockArtifacts);
		List<File> dependencies = copyDependenciesUtil.copyDependencies(JavaScopes.COMPILE);
		
		assertEquals(dependencies.size(), 2);
		verify(mockProcessRunner, times(1)).runProcess(null, "unzip", "frameworkAbsPath", "-d", "/test-directory/xcode-dependencies/frameworks/com.test.framework/framework");
		verify(mockProcessRunner, times(1)).runProcess(null, "unzip", "libraryAbsPath", "-d", "/test-directory/xcode-dependencies/libraries/com.test.library/library");
	}

}
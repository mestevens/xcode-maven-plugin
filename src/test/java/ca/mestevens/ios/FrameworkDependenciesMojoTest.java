package ca.mestevens.ios;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Build;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import ca.mestevens.ios.utils.ProcessRunner;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@Test(groups = "automated")
public class FrameworkDependenciesMojoTest {
	
	public MavenProject mockProject;
    public List<RemoteRepository> mockProjectRepos;
	public RepositorySystem mockRepoSystem;
	public RepositorySystemSession mockRepoSession;
	public final boolean addDependencies = false;
	public final String xcodeProjectName = "test-project-name";
	public ProcessRunner mockProcessRunner;
	public Log mockLog;
	public FrameworkDependenciesMojo dependenciesMojo;

	@BeforeMethod
	public void setUp() throws DependencyResolutionException, MojoFailureException {
		mockProject = mock(MavenProject.class);
		mockProjectRepos = new ArrayList<RemoteRepository>();
		mockRepoSystem = mock(RepositorySystem.class);
		mockRepoSession = mock(RepositorySystemSession.class);
		mockProcessRunner = mock(ProcessRunner.class);
		mockLog = mock(Log.class);
		mockProcessRunner.log = mockLog;
		dependenciesMojo = new FrameworkDependenciesMojo();
		dependenciesMojo.processRunner = mockProcessRunner;
		dependenciesMojo.xcodeProject = xcodeProjectName;
		dependenciesMojo.addDependencies = addDependencies;
		dependenciesMojo.repoSession = mockRepoSession;
		dependenciesMojo.repoSystem = mockRepoSystem;
		dependenciesMojo.projectRepos = mockProjectRepos;
		dependenciesMojo.project = mockProject;
		
		ArtifactResult mockFrameworkArtifactResult = new ArtifactResult(new ArtifactRequest());
		ArtifactResult mockLibraryArtifactResult = new ArtifactResult(new ArtifactRequest());
		Artifact mockFrameworkArtifact = mock(Artifact.class);
		Artifact mockLibraryArtifact = mock(Artifact.class);
		mockFrameworkArtifactResult.setArtifact(mockFrameworkArtifact);
		mockLibraryArtifactResult.setArtifact(mockLibraryArtifact);
		List<ArtifactResult> mockArtifactResultList = new ArrayList<ArtifactResult>();
		mockArtifactResultList.add(mockFrameworkArtifactResult);
		mockArtifactResultList.add(mockLibraryArtifactResult);
		
		org.apache.maven.artifact.Artifact mockArtifact = mock(org.apache.maven.artifact.Artifact.class);
		when(mockProject.getArtifact()).thenReturn(mockArtifact);
		when(mockArtifact.getId()).thenReturn("com.test:application:1.0");
		DependencyRequest mockDependencyRequest = new DependencyRequest();
		DependencyResult mockDependencyResult = new DependencyResult(mockDependencyRequest);
		mockDependencyResult.setArtifactResults(mockArtifactResultList);
		when(mockRepoSystem.resolveDependencies(eq(mockRepoSession), any(DependencyRequest.class))).thenReturn(mockDependencyResult);
		
		File mockFrameworkFile = mock(File.class);
		File mockLibraryFile = mock(File.class);
		when(mockFrameworkArtifact.getProperty("type", "")).thenReturn("xcode-framework");
		when(mockLibraryArtifact.getProperty("type", "")).thenReturn("xcode-library");
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
		
		Build mockBuild = mock(Build.class);
		when(mockProject.getBuild()).thenReturn(mockBuild);
		when(mockBuild.getDirectory()).thenReturn("test-directory");
		
		when(mockProcessRunner.runProcess(null, "unzip", "frameworkAbsPath", "-d", "test-directory/xcode-dependencies/frameworks/com.test.framework/framework")).thenReturn(0);
		when(mockProcessRunner.runProcess(null, "unzip", "libraryAbsPath", "-d", "test-directory/xcode-dependencies/libraries/com.test.library/library")).thenReturn(0);
	}
	
	@AfterMethod
	public void tearDown() {
		dependenciesMojo = null;
		mockLog = null;
		mockProcessRunner = null;
		mockRepoSession = null;
		mockRepoSystem = null;
		mockProjectRepos = null;
		mockProject = null;
	}
	
	@Test
	public void testExecute() throws MojoExecutionException, MojoFailureException {
		dependenciesMojo.execute();
	}
	
}
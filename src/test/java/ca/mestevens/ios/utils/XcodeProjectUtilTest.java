package ca.mestevens.ios.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.FileUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import ca.mestevens.ios.xcode.parser.exceptions.InvalidObjectFormatException;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertEquals;

@Test(groups = "automated")
public class XcodeProjectUtilTest {
	
	private final String pbxprojString = Thread.currentThread().getContextClassLoader().getResource("test.xcodeproj").getPath().toString() + "/project.pbxproj";
	private XcodeProjectUtil projectUtil;

	@BeforeMethod()
	public void setUp() throws InvalidObjectFormatException, IOException {
		projectUtil = new XcodeProjectUtil(pbxprojString);
		File xcodeDependenciesDirectory = new File("target/test-classes/target/xcode-dependencies/com.test/test/test.framework");
		xcodeDependenciesDirectory.mkdirs();
		new File("target/test-classes/target/xcode-dependencies/com.test/test/test.framework/test").createNewFile();
		new File("target/test-classes/target/xcode-dependencies/com.test/test/libtest.a").createNewFile();
	}
	
	@AfterMethod
	public void tearDown() throws IOException {
		File xcodeDependenciesDirectory = new File("target/test-classes/target/xcode-dependencies");
		if (xcodeDependenciesDirectory.exists()) {
			FileUtils.deleteDirectory(xcodeDependenciesDirectory);
		}
		projectUtil = null;
	}
	
	@Test
	public void testAddFramework() throws MojoExecutionException {
		String path = "target/test-classes/target/xcode-dependencies/com.test/test/test.framework";
		File testFrameworkFile = new File(path);
		List<File> dependencies = new ArrayList<File>();
		dependencies.add(testFrameworkFile);
		
		path = path.substring(path.lastIndexOf("target"));
		
		int numberOfFileReferences = projectUtil.getXcodeProject().fileReferences.size();
		int numberOfBuildFiles = projectUtil.getXcodeProject().buildFiles.size();
		int numberOfGroups = projectUtil.getXcodeProject().groups.size();
		int numberOfCopyFilePhases = projectUtil.getXcodeProject().copyFilesBuildPhases.size();
		
		projectUtil.addDependenciesToTarget("testProject", dependencies, null, null, null);
		
		assertEquals(projectUtil.getXcodeProject().fileReferences.size(), numberOfFileReferences + 1);
		assertNotNull(projectUtil.getXcodeProject().getFileReferenceWithPath(path));
		assertEquals(projectUtil.getXcodeProject().buildFiles.size(), numberOfBuildFiles + 1);
		assertEquals(projectUtil.getXcodeProject().getBuildFileWithFileRefPath(path).size(), 1);
		assertEquals(projectUtil.getXcodeProject().groups.size(), numberOfGroups + 1);
		assertEquals(projectUtil.getXcodeProject().copyFilesBuildPhases.size(), numberOfCopyFilePhases + 1);
		
	}
	
	@Test
	public void testAddFrameworksThatAlreadyExist() throws MojoExecutionException {
		String path = "target/test-classes/target/xcode-dependencies/com.test/test/test.framework";
		File testFrameworkFile = new File(path);
		List<File> dependencies = new ArrayList<File>();
		dependencies.add(testFrameworkFile);
		
		path = path.substring(path.lastIndexOf("target"));
		
		int numberOfFileReferences = projectUtil.getXcodeProject().fileReferences.size();
		int numberOfBuildFiles = projectUtil.getXcodeProject().buildFiles.size();
		
		projectUtil.addDependenciesToTarget("testProject", dependencies, null, null, null);
		
		assertEquals(projectUtil.getXcodeProject().fileReferences.size(), numberOfFileReferences + 1);
		assertNotNull(projectUtil.getXcodeProject().getFileReferenceWithPath(path));
		assertEquals(projectUtil.getXcodeProject().buildFiles.size(), numberOfBuildFiles + 1);
		assertEquals(projectUtil.getXcodeProject().getBuildFileWithFileRefPath(path).size(), 1);
		
		projectUtil.addDependenciesToTarget("testProject", dependencies, null, null, null);
		
		assertEquals(projectUtil.getXcodeProject().fileReferences.size(), numberOfFileReferences + 1);
		assertNotNull(projectUtil.getXcodeProject().getFileReferenceWithPath(path));
		assertEquals(projectUtil.getXcodeProject().buildFiles.size(), numberOfBuildFiles + 1);
		assertEquals(projectUtil.getXcodeProject().getBuildFileWithFileRefPath(path).size(), 1);
	}
	
	@Test
	public void testAddLibrary() throws MojoExecutionException {
		String path = "target/test-classes/target/xcode-dependencies/com.test/test/libtest.a";
		File testFrameworkFile = new File(path);
		List<File> dependencies = new ArrayList<File>();
		dependencies.add(testFrameworkFile);
		
		path = path.substring(path.lastIndexOf("target"));
		
		int numberOfFileReferences = projectUtil.getXcodeProject().fileReferences.size();
		int numberOfBuildFiles = projectUtil.getXcodeProject().buildFiles.size();
		
		projectUtil.addDependenciesToTarget("testProject", null, null, dependencies, null);
		
		assertEquals(projectUtil.getXcodeProject().fileReferences.size(), numberOfFileReferences + 1);
		assertNotNull(projectUtil.getXcodeProject().getFileReferenceWithPath(path));
		assertEquals(projectUtil.getXcodeProject().buildFiles.size(), numberOfBuildFiles + 1);
		assertEquals(projectUtil.getXcodeProject().getBuildFileWithFileRefPath(path).size(), 1);
	}
	
	@Test
	public void testAddLibrariesThatAlreadyExist() throws MojoExecutionException {
		String path = "target/test-classes/target/xcode-dependencies/com.test/test/libtest.a";
		File testFrameworkFile = new File(path);
		List<File> dependencies = new ArrayList<File>();
		dependencies.add(testFrameworkFile);
		
		path = path.substring(path.lastIndexOf("target"));
		
		int numberOfFileReferences = projectUtil.getXcodeProject().fileReferences.size();
		int numberOfBuildFiles = projectUtil.getXcodeProject().buildFiles.size();
		
		projectUtil.addDependenciesToTarget("testProject", null, null, dependencies, null);
		
		assertEquals(projectUtil.getXcodeProject().fileReferences.size(), numberOfFileReferences + 1);
		assertNotNull(projectUtil.getXcodeProject().getFileReferenceWithPath(path));
		assertEquals(projectUtil.getXcodeProject().buildFiles.size(), numberOfBuildFiles + 1);
		assertEquals(projectUtil.getXcodeProject().getBuildFileWithFileRefPath(path).size(), 1);
		
		projectUtil.addDependenciesToTarget("testProject", null, null, dependencies, null);
		
		assertEquals(projectUtil.getXcodeProject().fileReferences.size(), numberOfFileReferences + 1);
		assertNotNull(projectUtil.getXcodeProject().getFileReferenceWithPath(path));
		assertEquals(projectUtil.getXcodeProject().buildFiles.size(), numberOfBuildFiles + 1);
		assertEquals(projectUtil.getXcodeProject().getBuildFileWithFileRefPath(path).size(), 1);
	}
	
}

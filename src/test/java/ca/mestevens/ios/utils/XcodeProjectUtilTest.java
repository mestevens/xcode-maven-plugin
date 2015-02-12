package ca.mestevens.ios.utils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import ca.mestevens.ios.xcode.parser.exceptions.InvalidObjectFormatException;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertEquals;

@Test(groups = "automated")
public class XcodeProjectUtilTest {
	
	private final String pbxprojString = Thread.currentThread().getContextClassLoader().getResource("project.pbxproj").getPath().toString();
	private XcodeProjectUtil projectUtil;

	@BeforeMethod()
	public void setUp() throws InvalidObjectFormatException {
		projectUtil = new XcodeProjectUtil(pbxprojString);
	}
	
	@AfterMethod
	public void tearDown() {
		projectUtil = null;
	}
	
	@Test
	public void testAddFramework() throws MojoExecutionException {
		String path = "target/xcode-dependencies/com.test/test/test.framework";
		File testFrameworkFile = new File(path);
		List<File> dependencies = new ArrayList<File>();
		dependencies.add(testFrameworkFile);
		
		int numberOfFileReferences = projectUtil.getXcodeProject().fileReferences.size();
		int numberOfBuildFiles = projectUtil.getXcodeProject().buildFiles.size();
		int numberOfGroups = projectUtil.getXcodeProject().groups.size();
		int numberOfCopyFilePhases = projectUtil.getXcodeProject().copyFilesBuildPhases.size();
		
		projectUtil.addDependenciesToTarget("testProject", dependencies);
		
		assertEquals(projectUtil.getXcodeProject().fileReferences.size(), numberOfFileReferences + 1);
		assertNotNull(projectUtil.getXcodeProject().getFileReferenceWithPath(path));
		assertEquals(projectUtil.getXcodeProject().buildFiles.size(), numberOfBuildFiles + 2);
		assertEquals(projectUtil.getXcodeProject().getBuildFileWithFileRefPath(path).size(), 2);
		assertEquals(projectUtil.getXcodeProject().groups.size(), numberOfGroups + 1);
		assertEquals(projectUtil.getXcodeProject().copyFilesBuildPhases.size(), numberOfCopyFilePhases + 1);
		
		
		
	}
	
	@Test
	public void testAddFrameworksThatAlreadyExist() throws MojoExecutionException {
		String path = "target/xcode-dependencies/com.test/test/test.framework";
		File testFrameworkFile = new File(path);
		List<File> dependencies = new ArrayList<File>();
		dependencies.add(testFrameworkFile);
		
		int numberOfFileReferences = projectUtil.getXcodeProject().fileReferences.size();
		int numberOfBuildFiles = projectUtil.getXcodeProject().buildFiles.size();
		
		projectUtil.addDependenciesToTarget("testProject", dependencies);
		
		assertEquals(projectUtil.getXcodeProject().fileReferences.size(), numberOfFileReferences + 1);
		assertNotNull(projectUtil.getXcodeProject().getFileReferenceWithPath(path));
		assertEquals(projectUtil.getXcodeProject().buildFiles.size(), numberOfBuildFiles + 2);
		assertEquals(projectUtil.getXcodeProject().getBuildFileWithFileRefPath(path).size(), 2);
		
		projectUtil.addDependenciesToTarget("testProject", dependencies);
		
		assertEquals(projectUtil.getXcodeProject().fileReferences.size(), numberOfFileReferences + 1);
		assertNotNull(projectUtil.getXcodeProject().getFileReferenceWithPath(path));
		assertEquals(projectUtil.getXcodeProject().buildFiles.size(), numberOfBuildFiles + 2);
		assertEquals(projectUtil.getXcodeProject().getBuildFileWithFileRefPath(path).size(), 2);
	}
	
	@Test
	public void testAddLibrary() throws MojoExecutionException {
		String path = "target/xcode-dependencies/com.test/test/libtest.a";
		File testFrameworkFile = new File(path);
		List<File> dependencies = new ArrayList<File>();
		dependencies.add(testFrameworkFile);
		
		int numberOfFileReferences = projectUtil.getXcodeProject().fileReferences.size();
		int numberOfBuildFiles = projectUtil.getXcodeProject().buildFiles.size();
		
		projectUtil.addDependenciesToTarget("testProject", dependencies);
		
		assertEquals(projectUtil.getXcodeProject().fileReferences.size(), numberOfFileReferences + 1);
		assertNotNull(projectUtil.getXcodeProject().getFileReferenceWithPath(path));
		assertEquals(projectUtil.getXcodeProject().buildFiles.size(), numberOfBuildFiles + 1);
		assertEquals(projectUtil.getXcodeProject().getBuildFileWithFileRefPath(path).size(), 1);
	}
	
	@Test
	public void testAddLibrariesThatAlreadyExist() throws MojoExecutionException {
		String path = "target/xcode-dependencies/com.test/test/libtest.a";
		File testFrameworkFile = new File(path);
		List<File> dependencies = new ArrayList<File>();
		dependencies.add(testFrameworkFile);
		
		int numberOfFileReferences = projectUtil.getXcodeProject().fileReferences.size();
		int numberOfBuildFiles = projectUtil.getXcodeProject().buildFiles.size();
		
		projectUtil.addDependenciesToTarget("testProject", dependencies);
		
		assertEquals(projectUtil.getXcodeProject().fileReferences.size(), numberOfFileReferences + 1);
		assertNotNull(projectUtil.getXcodeProject().getFileReferenceWithPath(path));
		assertEquals(projectUtil.getXcodeProject().buildFiles.size(), numberOfBuildFiles + 1);
		assertEquals(projectUtil.getXcodeProject().getBuildFileWithFileRefPath(path).size(), 1);
		
		projectUtil.addDependenciesToTarget("testProject", dependencies);
		
		assertEquals(projectUtil.getXcodeProject().fileReferences.size(), numberOfFileReferences + 1);
		assertNotNull(projectUtil.getXcodeProject().getFileReferenceWithPath(path));
		assertEquals(projectUtil.getXcodeProject().buildFiles.size(), numberOfBuildFiles + 1);
		assertEquals(projectUtil.getXcodeProject().getBuildFileWithFileRefPath(path).size(), 1);
	}
	
}

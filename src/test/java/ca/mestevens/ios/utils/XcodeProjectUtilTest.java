package ca.mestevens.ios.utils;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = "automated")
public class XcodeProjectUtilTest {
	
	private final String pbxprojString = Thread.currentThread().getContextClassLoader().getResource("project.pbxproj").getPath().toString();
	private XcodeProjectUtil projectUtil;

	@BeforeMethod()
	public void setUp() {
		projectUtil = new XcodeProjectUtil(pbxprojString);
	}
	
	@AfterMethod
	public void tearDown() {
		projectUtil = null;
	}
	
	@Test
	public void test() {
		
	}
	
}

package ca.mestevens.ios.utils;

import java.io.IOException;

import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;

public class XcodeFileUtilTest {
	
	private XcodeFileUtil fileUtil;
	
	@BeforeTest
	public void setUp() throws IOException {
		fileUtil = new XcodeFileUtil(Thread.currentThread().getContextClassLoader().getResource("project.pbxproj").getPath());
	}
	
	@AfterTest
	public void tearDown() {
		fileUtil = null;
	}

}

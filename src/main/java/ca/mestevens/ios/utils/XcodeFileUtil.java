package ca.mestevens.ios.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import ca.mestevens.ios.utils.models.PbxprojReferenceObject;
import ca.mestevens.ios.utils.models.PbxprojSection;

public class XcodeFileUtil {
	
	private final Path path;
	private String content;
	
	public XcodeFileUtil(final String pbxprojPath) throws IOException {
		path = Paths.get(pbxprojPath);
		content = new String(Files.readAllBytes(path));
	}
	
	public void writeFile() throws IOException {
		Files.write(path, content.getBytes());
	}
	
	public void writeFile(String path) throws IOException {
		Files.write(Paths.get(path), content.getBytes());
	}
	
	private String getUUID() {
		String uuid = UUID.randomUUID().toString();
		return uuid.replace("-", "");
	}
	
	public void addFrameworks(List<File> frameworks) {
		String fileReferencesString = "";
		String copyFrameworksUUID = getUUID();
		String copyFrameworksPhaseString = "objects = {\n\t\t" + copyFrameworksUUID + " /* Copy Frameworks */ = {\n" +
				"\t\t\tisa = PBXCopyFilesBuildPhase;\n" +
				"\t\t\tbuildActionMask = 2147483647;\n" +
				"\t\t\tdstPath = \"\";\n" +
				"\t\t\tdstSubfolderSpec = 10;\n" +
				"\t\t\tfiles = (\n";
		
		//Get Remote Global ID String
		int remoteGlobalIDIndex = content.indexOf("remoteGlobalIDString = ");
		String remoteGlobalIDString = content.substring(remoteGlobalIDIndex + ("remoteGlobalIDString = ").length(), content.indexOf(';', remoteGlobalIDIndex));
				
		String frameworksReference = addCopyFrameworksToBuildPhases(remoteGlobalIDString, copyFrameworksUUID);
		String buildConfigurationListString = getBuildConfigurationList(remoteGlobalIDString);
		
		PbxprojSection section = getPbxprojSection("PBXGroup");
		String frameworksId = null;
		for(String id : section.getObjectIds()) {
			if (section.getSection().contains(id + " /* Frameworks */ = {\n")) {
				frameworksId = id;
			}
		}
		List<File> addedFrameworks = new ArrayList<File>();
		for (File framework : frameworks) {
			if (content.contains("/* " + framework.getName() + " in Frameworks */")) {
				continue;
			}
			addedFrameworks.add(framework);
			String buildFileId = getUUID();
			String fileRefId = getUUID();
		
			String frameworkString = "objects = {\n\t\t" + buildFileId + " /* " + framework.getName() + " in Frameworks */ = {isa = PBXBuildFile; fileRef = " 
					+ fileRefId + " /* " + framework.getName() + " */; };\n";
		
			content = content.replace("objects = {\n", frameworkString);
		
			String relativeFrameworkPath = framework.getAbsolutePath().substring(framework.getAbsolutePath().lastIndexOf("target"));
			String fileReferenceFrameworkString = "objects = {\n\t\t" + fileRefId + " /* " + framework.getName() + " */ = {isa = PBXFileReference; lastKnownFileType = wrapper.framework; name = "
					+ framework.getName() + "; path = \"" + relativeFrameworkPath + "\"; sourceTree = SOURCE_ROOT; };\n";
		
			content = content.replace("objects = {\n", fileReferenceFrameworkString);
			
			String frameworkBuildFileId = getUUID();
			fileReferencesString = "objects = {\n\t\t" + frameworkBuildFileId + " /* " + framework.getName() + " in Copy Frameworks */ = {isa = PBXBuildFile; fileRef = "
					+ fileRefId + " /* " + framework.getName() + " */; settings = {ATTRIBUTES = (CodeSignOnCopy, RemoveHeadersOnCopy, ); }; };\n";
			String frameworkFile = "\t\t\t\t" + frameworkBuildFileId + " /* " + framework.getName() + " in Copy Frameworks */,\n";
			copyFrameworksPhaseString += frameworkFile;
			
			content = content.replace("objects = {\n", fileReferencesString);
			
			addFileToFrameworksBuildPhase(frameworksReference, buildFileId, framework.getName());
			
			if (frameworksId == null) {
				frameworksId = getUUID();
				String frameworkGroupsObject = "\t\t" + frameworksId + " = {\n" + "\t\t};\n";
				PbxprojReferenceObject newObject = new PbxprojReferenceObject(frameworkGroupsObject);
				newObject.setProperty("isa", "PBXGroup");
				newObject.addToPropertyList("children", fileRefId);
				newObject.setProperty("name", "Frameworks");
				newObject.setProperty("sourceTree", "\"<group>\"");
				section.addReferenceObject(newObject);
			} else {
				PbxprojReferenceObject frameworksObject = section.getReferenceObject(frameworksId);
				frameworksObject.addToPropertyList("children", fileRefId, framework.getName());
				section.updateReferenceObject(frameworksId, frameworksObject);
			}
			
		}
		
		if (addedFrameworks.size() > 0) {
			PbxprojSection projectSection = getPbxprojSection("PBXProject");
			String mainGroupReference = projectSection.getReferenceObject(projectSection.getObjectIds().get(0)).getProperty("mainGroup");
			PbxprojReferenceObject mainGroup = section.getReferenceObject(mainGroupReference);
			mainGroup.addToPropertyList("children", frameworksId);
			section.updateReferenceObject(mainGroupReference, mainGroup);
		
			content = content.replace(getPbxprojSection("PBXGroup").getSection(), section.getSection());
		
			copyFrameworksPhaseString += "\t\t\t);\n" +
					"\t\t\tname = \"Copy Frameworks\";\n" +
					"\t\t\trunOnlyForDeploymentPostprocessing = 0;\n" +
					"\t\t};\n";
		
			content = content.replace("objects = {\n", copyFrameworksPhaseString);
			
			updateConfigurations(buildConfigurationListString, addedFrameworks);
		}
	}
	
	//Returns the framework reference
	private String addCopyFrameworksToBuildPhases(String remoteGlobalIDString, String copyFrameworksUUID) {
		String nativeTargetString = "/* Begin PBXNativeTarget section */\n";
		String nativeTargetEndString = "/* End PBXNativeTarget section */";
		String nativeTargetSection = content.substring(content.indexOf(nativeTargetString) + nativeTargetString.length(),
				content.indexOf(nativeTargetEndString));
		String modifiedNativeTargetSection = nativeTargetSection;
		
		int nativeTargetGlobalIDIndex = modifiedNativeTargetSection.indexOf(remoteGlobalIDString);
		String mainTarget = modifiedNativeTargetSection.substring(nativeTargetGlobalIDIndex, modifiedNativeTargetSection.indexOf("\n\t\t};", nativeTargetGlobalIDIndex));
		String modifiedMainTarget = mainTarget;
		
		int buildPhaseIndex = mainTarget.indexOf("\t\t\tbuildPhases = (\n");
		String buildPhase = modifiedMainTarget.substring(buildPhaseIndex, modifiedMainTarget.indexOf("\t\t\t);", buildPhaseIndex));
		
		String frameworksReference = null;
		String[] lines = buildPhase.split("\n");
		for(String line : lines) {
			if (line.contains("/* Frameworks */,")) {
				frameworksReference = line.trim().substring(0, line.trim().indexOf(" /*"));
			}
		}
		
		String modifiedBuildPhase = buildPhase;
		modifiedBuildPhase += "\t\t\t\t" + copyFrameworksUUID + " /* Copy Frameworks */,\n";
		
		modifiedMainTarget = modifiedMainTarget.replace(buildPhase, modifiedBuildPhase);
		modifiedNativeTargetSection = modifiedNativeTargetSection.replace(mainTarget, modifiedMainTarget);
		content = content.replace(nativeTargetSection, modifiedNativeTargetSection);
		return frameworksReference;
	}
	
	private void addFileToFrameworksBuildPhase(String frameworksBuildPhaseId, String buildFileId, String fileName) {
		String frameworksBuildPhaseString = "/* Begin PBXFrameworksBuildPhase section */\n";
		String frameworksBuildPhaseEndString = "/* End PBXFrameworksBuildPhase section */";
		String frameworksBuildPhaseSection = content.substring(content.indexOf(frameworksBuildPhaseString) + frameworksBuildPhaseString.length(),
				content.indexOf(frameworksBuildPhaseEndString));
		String modifiedFrameworksBuildPhaseSection = frameworksBuildPhaseSection;
		
		int localFrameworkBuildPhaseId = modifiedFrameworksBuildPhaseSection.indexOf(frameworksBuildPhaseId);
		String mainPhase = modifiedFrameworksBuildPhaseSection.substring(localFrameworkBuildPhaseId, modifiedFrameworksBuildPhaseSection.indexOf("\n\t\t};", localFrameworkBuildPhaseId));
		String modifiedMainPhase = mainPhase;
		
		int filesIndex = modifiedMainPhase.indexOf("\t\t\tfiles = (\n");
		String files = modifiedMainPhase.substring(filesIndex, modifiedMainPhase.indexOf("\t\t\t);", filesIndex));
		String modifiedFiles = files;
		modifiedFiles += "\t\t\t\t" + buildFileId + " /* " + fileName + " */,\n";
		
		modifiedMainPhase = modifiedMainPhase.replace(files, modifiedFiles);
		modifiedFrameworksBuildPhaseSection = modifiedFrameworksBuildPhaseSection.replace(mainPhase, modifiedMainPhase);
		content = content.replace(frameworksBuildPhaseSection, modifiedFrameworksBuildPhaseSection);
	}
	
	private String getBuildConfigurationList(String remoteGlobalIDString) {
		String nativeTargetString = "/* Begin PBXNativeTarget section */\n";
		String nativeTargetEndString = "/* End PBXNativeTarget section */";
		String nativeTargetSection = content.substring(content.indexOf(nativeTargetString) + nativeTargetString.length(),
				content.indexOf(nativeTargetEndString));
		
		int nativeTargetGlobalIDIndex = nativeTargetSection.indexOf(remoteGlobalIDString);
		String mainTarget = nativeTargetSection.substring(nativeTargetGlobalIDIndex, nativeTargetSection.indexOf("\n\t\t};", nativeTargetGlobalIDIndex));
		
		int buildConfigurationIndex = mainTarget.indexOf("\t\t\tbuildConfigurationList = ");
		String buildConfigurationLine = mainTarget.substring(buildConfigurationIndex, mainTarget.indexOf(';', buildConfigurationIndex));
		buildConfigurationLine = buildConfigurationLine.replace("buildConfigurationList = ", "");
		return buildConfigurationLine.trim().substring(0, buildConfigurationLine.trim().indexOf(" /*"));
	}
	
	private void updateConfigurations(String buildConfigurationList, List<File> frameworks) {
		String configurationListString = "/* Begin XCConfigurationList section */\n";
		String configurationListEndString = "/* End XCConfigurationList section */";
		String configurationListSection = content.substring(content.indexOf(configurationListString) + configurationListString.length(),
				content.indexOf(configurationListEndString));
		
		int localBuildConfigurationListIndex = configurationListSection.indexOf(buildConfigurationList);
		String mainList = configurationListSection.substring(localBuildConfigurationListIndex, configurationListSection.indexOf("\n\t\t};", localBuildConfigurationListIndex));
		
		int buildConfigurationIndex = mainList.indexOf("\t\t\tbuildConfigurations = (");
		String buildConfigurations = mainList.substring(buildConfigurationIndex, mainList.indexOf("\t\t\t);", buildConfigurationIndex));
		
		String[] lines = buildConfigurations.replace("\t\t\tbuildConfigurations = (\n", "").split("\n");

		for(String line : lines) {
			String configurationReference = line.trim().substring(0, line.trim().indexOf(" /*"));
			updateConfiguration(configurationReference, frameworks);
		}

	}
	
	private void updateConfiguration(String configurationReference, List<File> frameworks) {
		String buildConfigurationString = "/* Begin XCBuildConfiguration section */\n";
		String buildConfigurationEndString = "/* End XCBuildConfiguration section */";
		String buildConfigurationSection = content.substring(content.indexOf(buildConfigurationString) + buildConfigurationString.length(),
				content.indexOf(buildConfigurationEndString));
		String modifiedBuildConfigurationSection = buildConfigurationSection;
		
		int localBuildConfigurationId = modifiedBuildConfigurationSection.indexOf(configurationReference);
		String mainConfiguration = modifiedBuildConfigurationSection.substring(localBuildConfigurationId, modifiedBuildConfigurationSection.indexOf("\n\t\t};", localBuildConfigurationId));
		String modifiedMainConfiguration = mainConfiguration;
		
		int localBuildSettingsIndex = modifiedMainConfiguration.indexOf("\t\t\tbuildSettings = {");
		String buildSettings = modifiedMainConfiguration.substring(localBuildSettingsIndex, modifiedMainConfiguration.indexOf("\n\t\t\t};", localBuildSettingsIndex));
		String modifiedBuildSettings = buildSettings;
		
		int frameworkSearchPathsIndex = modifiedBuildSettings.indexOf("\t\t\t\tFRAMEWORK_SEARCH_PATHS = (\n");
		if (frameworkSearchPathsIndex == -1) {
			String frameworkPaths = "\n\t\t\t\tFRAMEWORK_SEARCH_PATHS = (\n";
			for (File framework : frameworks) {
				frameworkPaths += "\t\t\t\t\t\"\\\"$(SRCROOT)/" + framework.getAbsolutePath().substring(framework.getAbsolutePath().lastIndexOf("target"),
						framework.getAbsolutePath().lastIndexOf('/')) + "\\\"\",\n";
			}
			frameworkPaths += "\t\t\t\t);";
			modifiedBuildSettings = modifiedBuildSettings.concat(frameworkPaths);
		} else {
			String paths = modifiedBuildSettings.substring(frameworkSearchPathsIndex, modifiedBuildSettings.indexOf("\t\t\t\t);", frameworkSearchPathsIndex));
			String modifiedPaths = paths;
			for (File framework : frameworks) {
				modifiedPaths += "\t\t\t\t\t\"\\\"$(SRCROOT)\\" + framework.getAbsolutePath().substring(framework.getAbsolutePath().lastIndexOf("target"),
						framework.getAbsolutePath().lastIndexOf('\\')) + "\\\"\",\n";
			}
			modifiedBuildSettings = modifiedBuildSettings.replace(paths, modifiedPaths);
		}
		
		int runtimeSearchPathsIndex = modifiedBuildSettings.indexOf("\t\t\t\tLD_RUNPATH_SEARCH_PATHS");
		if (runtimeSearchPathsIndex == -1) {
			String runpaths = "\n\t\t\t\tLD_RUNPATH_SEARCH_PATHS = \"@loader_path/Frameworks @executable_path/Frameworks\";";
			modifiedBuildSettings = modifiedBuildSettings.concat(runpaths);
		}
		
		modifiedMainConfiguration = modifiedMainConfiguration.replace(buildSettings, modifiedBuildSettings);
		modifiedBuildConfigurationSection = modifiedBuildConfigurationSection.replace(mainConfiguration, modifiedMainConfiguration);
		content = content.replace(buildConfigurationSection, modifiedBuildConfigurationSection);
	}
	
	public PbxprojSection getPbxprojSection(String sectionName) {
		String sectionString = "/* Begin " + sectionName + " section */\n";
		String sectionEndString = "/* End " + sectionName + " section */";
		String section = content.substring(content.indexOf(sectionString) + sectionString.length(),
				content.indexOf(sectionEndString));
		return new PbxprojSection(section);
	}

}

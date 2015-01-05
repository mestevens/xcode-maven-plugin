package ca.mestevens.ios.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import lombok.Data;

import org.apache.maven.plugin.MojoExecutionException;

import ca.mestevens.ios.xcode.parser.exceptions.InvalidObjectFormatException;
import ca.mestevens.ios.xcode.parser.models.CommentedIdentifier;
import ca.mestevens.ios.xcode.parser.models.PBXBuildFile;
import ca.mestevens.ios.xcode.parser.models.PBXBuildPhase;
import ca.mestevens.ios.xcode.parser.models.PBXFileElement;
import ca.mestevens.ios.xcode.parser.models.PBXTarget;
import ca.mestevens.ios.xcode.parser.models.XCConfigurationList;
import ca.mestevens.ios.xcode.parser.models.XCodeProject;

@Data
public class XcodeProjectUtil {
	
	private String pbxProjLocation;
	private XCodeProject xcodeProject;
	
	public XcodeProjectUtil(String pbxProjLocation) throws InvalidObjectFormatException {
		this.pbxProjLocation = pbxProjLocation;
		this.xcodeProject = new XCodeProject(pbxProjLocation);
	}

	public void addDependencies(List<File> dependencyFiles) throws MojoExecutionException {
		try {
			List<CommentedIdentifier> frameworkIdentifiers = new ArrayList<CommentedIdentifier>();
			List<CommentedIdentifier> fileReferenceIdentifiers = new ArrayList<CommentedIdentifier>();
			List<CommentedIdentifier> copyIdentifiers = new ArrayList<CommentedIdentifier>();
			boolean containsFrameworks = false;
			boolean containsLibraries = false;
			//Add the framework files as file references and build files
			for (File dependencyFile : dependencyFiles) {
				String frameworkPath = dependencyFile.getAbsolutePath().substring(dependencyFile.getAbsolutePath().lastIndexOf("target"));
				PBXFileElement fileReference = xcodeProject.getFileReferenceWithPath(frameworkPath);
				if (fileReference == null) {
					fileReference = xcodeProject.createFileReference(frameworkPath, "SOURCE_ROOT");
				}
				List<PBXBuildFile> buildFiles = xcodeProject.getBuildFileWithFileRefPath(frameworkPath);
				PBXBuildFile buildFile = null;
				PBXBuildFile copyBuildFile = null;
				for (PBXBuildFile existingFile : buildFiles) {
					if (existingFile.getReference().getComment().equals(dependencyFile.getName() + " in Frameworks")) {
						buildFile = existingFile;
					} else if (existingFile.getReference().getComment().equals(dependencyFile.getName() + " in Embed Frameworks")) {
						copyBuildFile = existingFile;
					}
				}
				String fileExtension = dependencyFile.getAbsolutePath().substring(dependencyFile.getAbsolutePath().lastIndexOf('.') + 1);
				if (fileExtension.equals("a")) {
					containsLibraries = true;
				}
				if (fileExtension.equals("framework")) {
					containsFrameworks = true;
				}
				if (buildFile == null) {
					buildFile = xcodeProject.createBuildFileFromFileReferencePath(frameworkPath, dependencyFile.getName() + " in Frameworks");
				}
				if (copyBuildFile == null && fileExtension.equals("framework")) {
					copyBuildFile = xcodeProject.createBuildFileFromFileReferencePath(frameworkPath, dependencyFile.getName() + " in Embed Frameworks");
					copyBuildFile.getSettings().put("ATTRIBUTES", "(CodeSignOnCopy, )");
				}
				
				frameworkIdentifiers.add(buildFile.getReference());
				fileReferenceIdentifiers.add(fileReference.getReference());
				if (copyBuildFile != null) {
					copyIdentifiers.add(copyBuildFile.getReference());
				}
			}
			PBXBuildPhase copyFrameworksBuildPhase = new PBXBuildPhase("PBXCopyFilesBuildPhase", "\"Embed Frameworks\"", copyIdentifiers, "\"\"", 10);
			//If the Embed Frameworks phase doesn't exist, create it
			boolean foundExistingPhase = false;
			for(PBXBuildPhase copyFilesBuildPhase : xcodeProject.getCopyFilesBuildPhases()) {
				if (copyFilesBuildPhase.getReference().getComment().equals("\"Embed Frameworks\"")) {
					for(CommentedIdentifier identifier : copyIdentifiers) {
						boolean foundFile = false;
						for (CommentedIdentifier fileIdentifier : copyFilesBuildPhase.getFiles()) {
							if (fileIdentifier.getComment().equals(identifier.getComment())) {
								foundFile = true;
							}
						}
						if (!foundFile) {
							copyFilesBuildPhase.getFiles().add(identifier);
						}
					}
					foundExistingPhase = true;
				}
			}
			//Grab the first target
			String firstTargetIdentifier = xcodeProject.getProject().getTargets().get(0).getIdentifier();
			if (!foundExistingPhase) {
				xcodeProject.addCopyFilesBuildPhase(firstTargetIdentifier, copyFrameworksBuildPhase);
			}
			//Get the configuration list identifier for the first target
			String existingFrameworksPhaseId = null;
			String buildConfigurationList = null;
			PBXTarget nativeTarget = xcodeProject.getNativeTargetWithIdentifier(firstTargetIdentifier);
			for(CommentedIdentifier buildPhaseIdentifier : nativeTarget.getBuildPhases()) {
				if (buildPhaseIdentifier.getComment().equals("Frameworks")) {
					existingFrameworksPhaseId = buildPhaseIdentifier.getIdentifier();
					buildConfigurationList = nativeTarget.getBuildConfigurationList().getIdentifier();
					break;
				}
			}
			//Add the framework build files to the frameworks build phase
			PBXBuildPhase frameworksBuildPhase = xcodeProject.getFrameworksBuildPhaseWithIdentifier(existingFrameworksPhaseId);
			if (frameworksBuildPhase.getReference().getIdentifier().equals(existingFrameworksPhaseId)) {
				for(CommentedIdentifier identifier : frameworkIdentifiers) {
					if (!frameworksBuildPhase.getFiles().contains(identifier)) {
						frameworksBuildPhase.getFiles().add(identifier);
					}
				}
			}
			//Add the properties to the build configuration
			XCConfigurationList configuration = xcodeProject.getConfigurationListWithIdentifier(buildConfigurationList);
			for(CommentedIdentifier identifier : configuration.getBuildConfigurations()) {
				if (containsFrameworks) {
					//FRAMEWORK_SEARCH_PATHS
					List<String> frameworkSearchPaths = xcodeProject.getBuildConfigurationPropertyAsList(identifier.getIdentifier(), "FRAMEWORK_SEARCH_PATHS");
					if (frameworkSearchPaths != null) {
						if (!frameworkSearchPaths.contains("\"${PROJECT_DIR}/target/xcode-dependencies/frameworks/**\"")) {
							frameworkSearchPaths.add("\"${PROJECT_DIR}/target/xcode-dependencies/frameworks/**\"");
							xcodeProject.setBuildConfigurationProperty(identifier.getIdentifier(), "FRAMEWORK_SEARCH_PATHS", frameworkSearchPaths);
						}
					} else {
						frameworkSearchPaths = new ArrayList<String>();
						frameworkSearchPaths.add("\"${PROJECT_DIR}/target/xcode-dependencies/frameworks/**\"");
						xcodeProject.setBuildConfigurationProperty(identifier.getIdentifier(), "FRAMEWORK_SEARCH_PATHS", frameworkSearchPaths);
					}
					//LD_RUNPATH_SEARCH_PATHS
					String ldRunpathSearchPaths = xcodeProject.getBuildConfigurationProperty(identifier.getIdentifier(), "LD_RUNPATH_SEARCH_PATHS");
					if (ldRunpathSearchPaths != null) {
						if (ldRunpathSearchPaths.startsWith("\"")) {
							ldRunpathSearchPaths = ldRunpathSearchPaths.substring(1);
						}
						if (ldRunpathSearchPaths.endsWith("\"")) {
							ldRunpathSearchPaths = ldRunpathSearchPaths.substring(0, ldRunpathSearchPaths.length() - 1);
						}
						ldRunpathSearchPaths = ldRunpathSearchPaths.trim();
						if (!ldRunpathSearchPaths.contains("@loader_path/Frameworks")) {
							ldRunpathSearchPaths = ldRunpathSearchPaths.concat(" @loader_path/Frameworks");
						}
						if (!ldRunpathSearchPaths.contains("@executable_path/Frameworks")) {
							ldRunpathSearchPaths = ldRunpathSearchPaths.concat(" @executable_path/Frameworks");
						}
						ldRunpathSearchPaths = "\"" + ldRunpathSearchPaths + "\"";
						xcodeProject.setBuildConfigurationProperty(identifier.getIdentifier(), "LD_RUNPATH_SEARCH_PATHS", ldRunpathSearchPaths);
					} else {
						xcodeProject.setBuildConfigurationProperty(identifier.getIdentifier(), "LD_RUNPATH_SEARCH_PATHS", "\"@loader_path/Frameworks @executable_path/Frameworks\"");
					}
				}
				if (containsLibraries) {
					//HEADER_SEARCH_PATHS
					List<String> headerSearchPaths = xcodeProject.getBuildConfigurationPropertyAsList(identifier.getIdentifier(), "HEADER_SEARCH_PATHS");
					if (headerSearchPaths == null) {
						headerSearchPaths = new ArrayList<String>();
					}
					if (!headerSearchPaths.contains("\"${PROJECT_DIR}/target/xcode-dependencies/libraries/**\"")) {
						headerSearchPaths.add("\"${PROJECT_DIR}/target/xcode-dependencies/libraries/**\"");
						xcodeProject.setBuildConfigurationProperty(identifier.getIdentifier(), "HEADER_SEARCH_PATHS", headerSearchPaths);
					}
					//LIBRARY_SEARCH_PATHS
					List<String> librarySearchPaths = xcodeProject.getBuildConfigurationPropertyAsList(identifier.getIdentifier(), "LIBRARY_SEARCH_PATHS");
					if (librarySearchPaths == null) {
						librarySearchPaths = new ArrayList<String>();
					}
					if (!librarySearchPaths.contains("\"${PROJECT_DIR}/target/xcode-dependencies/libraries/**\"")) {
						librarySearchPaths.add("\"${PROJECT_DIR}/target/xcode-dependencies/libraries/**\"");
						xcodeProject.setBuildConfigurationProperty(identifier.getIdentifier(), "LIBRARY_SEARCH_PATHS", librarySearchPaths);
					}
				}
			}
			//Add/Edit the Frameworks group
			String mainGroupIdentifier = xcodeProject.getProject().getMainGroup().getIdentifier();
			String groupIdentifier = xcodeProject.getGroupWithIdentifier(mainGroupIdentifier).getChildren().get(0).getIdentifier();
			PBXFileElement group = xcodeProject.getGroupWithIdentifier(groupIdentifier);
			String frameworkGroupIdentifier = null;
			for(CommentedIdentifier child : group.getChildren()) {
				if (child.getComment().equals("Frameworks")) {
					frameworkGroupIdentifier = child.getIdentifier();
				}
			}
			PBXFileElement frameworkGroup = null;
			if (frameworkGroupIdentifier != null) {
				frameworkGroup = xcodeProject.getGroupWithIdentifier(frameworkGroupIdentifier);
			} else {
				frameworkGroup = xcodeProject.createGroup("Frameworks", groupIdentifier);
			}
			for (CommentedIdentifier fileReference : fileReferenceIdentifiers) {
				boolean found = false;
				for (CommentedIdentifier child : frameworkGroup.getChildren()) {
					if (child.getComment().equals(fileReference.getComment())) {
						found = true;
					}
				}
				if (!found) {
					frameworkGroup.addChild(fileReference);
				}
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new MojoExecutionException(ex.getMessage());
		}
	}
	
	public void writeProject() throws IOException {
		Files.write(Paths.get(pbxProjLocation), xcodeProject.toString().getBytes());
	}
	
}

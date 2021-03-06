package ca.mestevens.ios.utils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import lombok.Data;

import org.apache.maven.plugin.MojoExecutionException;

import ca.mestevens.ios.models.Dependency;
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
	
	public boolean containsTestTarget() {
		List<PBXTarget> targets = xcodeProject.getNativeTargets();
		for(PBXTarget target : targets) {
			if (target.getProductType() != null && target.getProductType().contains("com.apple.product-type.bundle.unit-test")) {
				return true;
			}
		}
		return false;
	}

	public void addDependenciesToTarget(String targetName, List<File> dynamicFrameworks, List<File> staticFrameworks, List<File> libraries, List<Dependency> externalDependencies) throws MojoExecutionException {
		try {
			List<File> masterFileList = new ArrayList<File>();
			if (libraries == null) {
				libraries = new ArrayList<File>();
			}
			if (staticFrameworks == null) {
				staticFrameworks = new ArrayList<File>();
			}
			if (dynamicFrameworks == null) {
				dynamicFrameworks = new ArrayList<File>();
			}
			if (externalDependencies == null) {
				externalDependencies = new ArrayList<Dependency>();
			}
			masterFileList.addAll(libraries);
			masterFileList.addAll(staticFrameworks);
			masterFileList.addAll(dynamicFrameworks);
			
			//Add the file/build file references
			List<CommentedIdentifier> fileReferenceIdentifiers = new ArrayList<CommentedIdentifier>();
			List<CommentedIdentifier> libraryFileReferences = addFileReferences(libraries);
			List<CommentedIdentifier> staticFrameworkFileReferences = addFileReferences(staticFrameworks);
			List<CommentedIdentifier> dynamicFrameworkFileReferences = addFileReferences(dynamicFrameworks);
			List<CommentedIdentifier> externalFileReferences = addExternalFileReferences(externalDependencies);
			fileReferenceIdentifiers.addAll(libraryFileReferences);
			fileReferenceIdentifiers.addAll(staticFrameworkFileReferences);
			fileReferenceIdentifiers.addAll(dynamicFrameworkFileReferences);
			fileReferenceIdentifiers.addAll(externalFileReferences);
			List<CommentedIdentifier> libraryBuildIdentifiers = addBuildFiles(libraryFileReferences, false);
			List<CommentedIdentifier> staticFrameworkBuildIdentifiers = addBuildFiles(staticFrameworkFileReferences, false);
			List<CommentedIdentifier> externalFrameworkBuildIdentifiers = addBuildFiles(externalFileReferences, false);
			List<CommentedIdentifier> dynamicFrameworkBuildIdentifiers = addBuildFiles(dynamicFrameworkFileReferences, true);
			//Get the target
			PBXTarget target = getNativeTarget(targetName);
			//Link the static libraries
			linkLibraries(target.getReference().getIdentifier(), libraryBuildIdentifiers);
			linkLibraries(target.getReference().getIdentifier(), staticFrameworkBuildIdentifiers);
			linkLibraries(target.getReference().getIdentifier(), externalFrameworkBuildIdentifiers);
			//Embed the dynamic libraries
			embedLibraries(target, dynamicFrameworkBuildIdentifiers);
			//Add the properties to the build configuration
			XCConfigurationList configuration = xcodeProject.getConfigurationListWithIdentifier(target.getBuildConfigurationList().getIdentifier());
			for(CommentedIdentifier identifier : configuration.getBuildConfigurations()) {
				if ((dynamicFrameworks != null && dynamicFrameworks.size() > 0) || (staticFrameworks != null && staticFrameworks.size() > 0)) {
					addPropertyToList(identifier.getIdentifier(), "FRAMEWORK_SEARCH_PATHS", "\"${PROJECT_DIR}/target/xcode-dependencies/frameworks/**\"");
					addPropertyToString(identifier.getIdentifier(), "LD_RUNPATH_SEARCH_PATHS", "@loader_path/Frameworks");
					addPropertyToString(identifier.getIdentifier(), "LD_RUNPATH_SEARCH_PATHS", "@executable_path/Frameworks");
				}
				if (libraries != null && libraries.size() > 0) {
					addPropertyToList(identifier.getIdentifier(), "HEADER_SEARCH_PATHS", "\"${PROJECT_DIR}/target/xcode-dependencies/libraries/**\"");
					addPropertyToList(identifier.getIdentifier(), "LIBRARY_SEARCH_PATHS", "\"${PROJECT_DIR}/target/xcode-dependencies/libraries/**\"");
				}
			}
			if (fileReferenceIdentifiers.size() > 0) {
				createGroup(fileReferenceIdentifiers);
			}
		} catch (Exception ex) {
			ex.printStackTrace();
			throw new MojoExecutionException(ex.getMessage());
		}
	}
	
	public void addPropertyToList(String buildConfiguration, String key, String value) {
		List<String> propertyList = xcodeProject.getBuildConfigurationPropertyAsList(buildConfiguration, key);
		if (propertyList == null) {
			propertyList = new ArrayList<String>();
		}
		if (!propertyList.contains(value)) {
			propertyList.add(value);
			xcodeProject.setBuildConfigurationProperty(buildConfiguration, key, propertyList);
		}
	}
	
	public void addPropertyToString(String buildConfiguration, String key, String value) {
		String property = xcodeProject.getBuildConfigurationProperty(buildConfiguration, key);
		if (property != null) {
			if (property.startsWith("\"")) {
				property = property.substring(1);
			}
			if (property.endsWith("\"")) {
				property = property.substring(0, property.length() - 1);
			}
			property = property.trim();
			if (!property.contains(value)) {
				property = property.concat(" " + value);
			}
			property = "\"" + property + "\"";
			xcodeProject.setBuildConfigurationProperty(buildConfiguration, key, property);
		} else {
			xcodeProject.setBuildConfigurationProperty(buildConfiguration, key, "\"" + value + "\"");
		}
	}
	
	public List<CommentedIdentifier> addBuildFiles(List<CommentedIdentifier> fileReferences, boolean embed) {
		List<CommentedIdentifier> buildFileReferences = new ArrayList<CommentedIdentifier>();
		String xcodeProjLocation = pbxProjLocation.substring(0, pbxProjLocation.lastIndexOf("/project.pbxproj"));
		for(CommentedIdentifier fileReference : fileReferences) {
			boolean skip = false;
			for(PBXFileElement fileElement : xcodeProject.getFileReferences()) {
				if (fileElement.getReference().getIdentifier().equals(fileReference.getIdentifier())) {
					String path = fileElement.getPath();
					if (path.startsWith("\"") && path.endsWith("\"")) {
						path = path.substring(1);
						path = path.substring(0, path.length() - 1);
					}
					String fileName = fileElement.getReference().getComment();
					if (fileName.endsWith(".framework")) {
						fileName = fileName.substring(0, fileName.lastIndexOf("."));
						path += "/" + fileName;
					}		
					File file = new File(xcodeProjLocation + "/../" + path);
					if (fileElement.getSourceTree().equals("SOURCE_ROOT") && !file.exists()) {
						skip = true;
					}
				}
			}
			if (skip) {
				continue;
			}
			List<PBXBuildFile> buildFiles = xcodeProject.getBuildFileWithFileRef(fileReference.getIdentifier());
			//Check for duplicates and remove them
			if (buildFiles.size() > 1) {
				Iterator<PBXBuildFile> buildFileIterator = buildFiles.iterator();
				List<PBXBuildFile> uniqueBuildFiles = new ArrayList<PBXBuildFile>();
				uniqueBuildFiles.add(buildFileIterator.next());
				while(buildFileIterator.hasNext()) {
					PBXBuildFile nextBuildFile = buildFileIterator.next();
					boolean foundBuildFile = false;
					for(PBXBuildFile uniqueBuildFile : uniqueBuildFiles) {
						if (uniqueBuildFile.getFileRef().equals(nextBuildFile.getFileRef()) && uniqueBuildFile.getSettings().equals(nextBuildFile.getSettings())) {
							foundBuildFile = true;
						}
					}
					if (foundBuildFile) {
						//Remove the file
						buildFileIterator.remove();
						removeBuildFile(nextBuildFile.getReference().getIdentifier());
					} else {
						uniqueBuildFiles.add(nextBuildFile);
					}
				}
			}
			PBXBuildFile buildFile = null;
			for (PBXBuildFile existingFile : buildFiles) {
				if (existingFile.getReference().getComment().contains(fileReference.getComment() + " in Frameworks")) {
					buildFile = existingFile;
				} else if (existingFile.getReference().getComment().contains(fileReference.getComment() + " in Embed Frameworks")) {
					buildFile = existingFile;
				}
			}
			if (buildFile == null && embed) {
				buildFile = new PBXBuildFile(fileReference.getComment() + " in Embed Frameworks", fileReference.getIdentifier());
				buildFile.getSettings().put("ATTRIBUTES", "(CodeSignOnCopy, )");
				xcodeProject.getBuildFiles().add(buildFile);
			} else if (buildFile == null) {
				buildFile = new PBXBuildFile(fileReference.getComment() + " in Frameworks", fileReference.getIdentifier());
				xcodeProject.getBuildFiles().add(buildFile);
			}
			buildFileReferences.add(buildFile.getReference());
		}
		return buildFileReferences;
	}
	
	public List<CommentedIdentifier> addFileReferences(List<File> files) {
		List<CommentedIdentifier> fileReferences = new ArrayList<CommentedIdentifier>();
		for (File dependencyFile : files) {
			if (!dependencyFile.exists()) {
				continue;
			}
			String frameworkPath = dependencyFile.getAbsolutePath().substring(dependencyFile.getAbsolutePath().lastIndexOf("target"));
			PBXFileElement fileReference = xcodeProject.getFileReferenceWithPath(frameworkPath);
			if (fileReference == null) {
				fileReference = xcodeProject.getFileReferenceWithPath("\"" + frameworkPath + "\"");
				if (fileReference == null) {
					fileReference = xcodeProject.createFileReference(frameworkPath, "SOURCE_ROOT");
				}
			}
			fileReferences.add(fileReference.getReference());
		}
		return fileReferences;
	}
	
	public List<CommentedIdentifier> addExternalFileReferences(List<Dependency> dependencies) {
		List<CommentedIdentifier> fileReferences = new ArrayList<CommentedIdentifier>();
		for (Dependency dependencyFile : dependencies) {
			String path = dependencyFile.getPath();
			if (path == null) {
				path = "System/Library/Frameworks/" + dependencyFile.getName() + ".framework";
			}
			PBXFileElement fileReference = xcodeProject.getFileReferenceWithPath(path);
			if (fileReference == null) {
				fileReference = xcodeProject.getFileReferenceWithPath("\"" + path + "\"");
				if (fileReference == null) {
					String sourceTree = "SDKROOT";
					if (!(dependencyFile.getPath() == null)) {
						sourceTree = "SOURCE_ROOT";
					}
					fileReference = xcodeProject.createFileReference(path, sourceTree);
				}
			}
			fileReferences.add(fileReference.getReference());
		}
		return fileReferences;
	}
	
	public void createGroup(List<CommentedIdentifier> identifiers) {
		//Add/Edit the Frameworks group
		String frameworkGroupIdentifier = null;
		for(PBXFileElement group : xcodeProject.getGroups()) {
			if (group.getName() != null && (group.getName().equals("Frameworks") || group.getName().equals("\"Frameworks\""))) {
				frameworkGroupIdentifier = group.getReference().getIdentifier();
			}
		}
		
		if (frameworkGroupIdentifier != null) {
			PBXFileElement frameworkGroup = xcodeProject.getGroupWithIdentifier(frameworkGroupIdentifier);
			for (CommentedIdentifier fileReference : identifiers) {
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
		} else {
			String mainGroupIdentifier = xcodeProject.getProject().getMainGroup().getIdentifier();
			xcodeProject.createGroup("Frameworks", identifiers, mainGroupIdentifier);
		}
	}
	
	public void linkLibraries(String targetIdentifier, List<CommentedIdentifier> identifiers) {
		//Get the configuration list identifier for the first target
		String existingFrameworksPhaseId = null;
		PBXTarget nativeTarget = xcodeProject.getNativeTargetWithIdentifier(targetIdentifier);
		for(CommentedIdentifier buildPhaseIdentifier : nativeTarget.getBuildPhases()) {
			if (buildPhaseIdentifier.getComment().equals("Frameworks")) {
				existingFrameworksPhaseId = buildPhaseIdentifier.getIdentifier();
				break;
			}
		}
		//Add the framework build files to the frameworks build phase
		PBXBuildPhase frameworksBuildPhase = xcodeProject.getFrameworksBuildPhaseWithIdentifier(existingFrameworksPhaseId);
		if (frameworksBuildPhase.getReference().getIdentifier().equals(existingFrameworksPhaseId)) {
			for(CommentedIdentifier identifier : identifiers) {
				if (!frameworksBuildPhase.getFiles().contains(identifier) && !frameworksBuildPhase.getFiles().contains("\"" + identifier + "\"")) {
					frameworksBuildPhase.getFiles().add(identifier);
				}
			}
		}
	}
	
	public void embedLibraries(PBXTarget target, List<CommentedIdentifier> identifiers) {
		//If the Embed Frameworks phase doesn't exist, create it
		boolean foundExistingPhase = false;
		String copyFrameworksBuildPhaseIdentifier = null;
		for(CommentedIdentifier buildPhase : target.getBuildPhases()) {
			if (buildPhase.getComment().contains("Embed Frameworks")) {
				copyFrameworksBuildPhaseIdentifier = buildPhase.getIdentifier();
			}
		}
		for(PBXBuildPhase copyFilesBuildPhase : xcodeProject.getCopyFilesBuildPhases()) {
			if (copyFilesBuildPhase.getReference().getIdentifier().equals(copyFrameworksBuildPhaseIdentifier)) {
				for(CommentedIdentifier identifier : identifiers) {
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
		if (!foundExistingPhase && identifiers.size() > 0) {
			PBXBuildPhase copyFrameworksBuildPhase = new PBXBuildPhase("PBXCopyFilesBuildPhase", "\"Embed Frameworks\"", identifiers, "\"\"", 10);
			xcodeProject.addCopyFilesBuildPhase(target.getReference().getIdentifier(), copyFrameworksBuildPhase);
		}
	}
	
	public PBXTarget getNativeTarget(String targetName) {
		for (PBXTarget target : xcodeProject.getNativeTargets()) {
			if (target.getName() != null && (target.getName().equals(targetName) || target.getName().equals("\"" + targetName + "\""))) {
				return target;
			}
		}
		return null;
	}
	
	public void removeBuildFile(String identifier) {
		List<PBXBuildFile> buildFiles = xcodeProject.getBuildFiles();
		Iterator<PBXBuildFile> buildFileIt = buildFiles.iterator();
		while(buildFileIt.hasNext()) {
			PBXBuildFile buildFile = buildFileIt.next();
			if (buildFile.getReference().getIdentifier().equals(identifier)) {
				buildFileIt.remove();
			}
		}
		List<PBXFileElement> groups = xcodeProject.getGroups();
		for (PBXFileElement group : groups) {
			Iterator<CommentedIdentifier> commentedIdentifierIt = group.getChildren().iterator();
			while(commentedIdentifierIt.hasNext()) {
				CommentedIdentifier commentedIdentifier = commentedIdentifierIt.next();
				if (commentedIdentifier.getIdentifier().equals(identifier)) {
					commentedIdentifierIt.remove();
				}
			}
		}
		List<PBXBuildPhase> frameworksBuildPhases = xcodeProject.getFrameworksBuildPhases();
		for (PBXBuildPhase frameworksBuildPhase : frameworksBuildPhases) {
			Iterator<CommentedIdentifier> commentedIdentifierIt = frameworksBuildPhase.getFiles().iterator();
			while(commentedIdentifierIt.hasNext()) {
				CommentedIdentifier commentedIdentifier = commentedIdentifierIt.next();
				if (commentedIdentifier.getIdentifier().equals(identifier)) {
					commentedIdentifierIt.remove();
				}
			}
		}
		List<PBXBuildPhase> copyFilesBuildPhases = xcodeProject.getCopyFilesBuildPhases();
		for (PBXBuildPhase copyFilesBuildPhase : copyFilesBuildPhases) {
			Iterator<CommentedIdentifier> commentedIdentifierIt = copyFilesBuildPhase.getFiles().iterator();
			while(commentedIdentifierIt.hasNext()) {
				CommentedIdentifier commentedIdentifier = commentedIdentifierIt.next();
				if (commentedIdentifier.getIdentifier().equals(identifier)) {
					commentedIdentifierIt.remove();
				}
			}
		}
	}
	
	public void writeProject() throws IOException {
		Files.write(Paths.get(pbxProjLocation), xcodeProject.toString().getBytes());
	}
	
}

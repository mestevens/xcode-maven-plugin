package ca.mestevens.ios.utils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;

public class CopyDependenciesUtil {
	
	private final MavenProject project;
	private final Log log;
	private final ProcessRunner processRunner;
	
	public CopyDependenciesUtil(MavenProject project, Log log, ProcessRunner processRunner) {
		this.project = project;
		this.log = log;
		this.processRunner = processRunner;
	}
	
	public Map<String, List<File>> copyDependencies(final String scope) throws MojoFailureException {		
		Set<Artifact> artifacts = project.getArtifacts();
		Map<String, List<File>> dependencyMap = new HashMap<String, List<File>>();
		List<File> libraryFiles = new ArrayList<File>();
		List<File> dynamicFrameworkFiles = new ArrayList<File>();
		List<File> staticFrameworkFiles = new ArrayList<File>();
		for (Artifact artifact : artifacts) {
			String type = artifact.getType();
			if ("xcode-framework".equals(type)) {
				type = "xcode-dynamic-framework";
			}
			if ("xcode-dynamic-framework".equals(type) || "xcode-static-framework".equals(type) || "xcode-library".equals(type)) {
				try {
					// Get File from result artifact
					File file = artifact.getFile();
					String resultFileName = project.getBuild().getDirectory() + "/xcode-dependencies/";
					if (type.equals("xcode-dynamic-framework") || type.equals("xcode-static-framework")) {
						resultFileName += "frameworks";
					} else if (type.equals("xcode-library")) {
						resultFileName += "libraries";
					}
					resultFileName += "/" + artifact.getGroupId() + "/" + artifact.getArtifactId();
					File resultFile = new File(resultFileName);
					
					if (resultFile.exists()) {
						FileUtils.deleteDirectory(resultFile);
					}
					
					FileUtils.mkdir(resultFile.getAbsolutePath());
					
					processRunner.runProcess(null, "unzip", file.getAbsolutePath(), "-d", resultFile.getAbsolutePath());
					
					if (type.equals("xcode-dynamic-framework")) {
						dynamicFrameworkFiles.add(new File(resultFile.getAbsolutePath() + "/" + artifact.getArtifactId() + ".framework"));
					} else if (type.equals("xcode-static-framework")) {
						staticFrameworkFiles.add(new File(resultFile.getAbsolutePath() + "/" + artifact.getArtifactId() + ".framework"));
					} else if (type.equals("xcode-library")) {
						libraryFiles.add(new File(resultFile.getAbsolutePath() + "/lib" + artifact.getArtifactId() + ".a"));
					}
					
				} catch (IOException e) {
					log.error("Problem creating/deleting framework file: " + artifact.getArtifactId());
					log.error(e.getMessage());
					throw new MojoFailureException("Problem creating/deleting framework file: " + artifact.getArtifactId());
				}
			}
		}
		dependencyMap.put("libraries", libraryFiles);
		dependencyMap.put("static-frameworks", staticFrameworkFiles);
		dependencyMap.put("dynamic-frameworks", dynamicFrameworkFiles);
		return dependencyMap;
	}

}

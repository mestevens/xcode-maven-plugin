# xcode-maven-plugin

## Latest Version
0.1

## Description

A maven plugin that will be used to build/test/deploy/pull in iOS frameworks.

## Pom.xml information

In order to use this plugin, add the following to your pom.xml

```
<plugin>
	<groupId>ca.mestevens</groupId>
	<artifactId>xcode-maven-plugin</artifactId>
	<version>${xcode.maven.plugin.version}</version>
	<extensions>true</extensions>
</plugin>
```

where `${xcode.maven.plugin}` is the version of the plugin you want to use.

## Goals

### framework-dependencies

Will grab xcode-framework artifacts from your maven repository and extract them as frameworks in the `target/xcode-dependencies/frameworks/<groupId>/<artifactId>` directory

### xcode-build

Will run an xcode archive build on your xcode project and place the resulting framework in your target directory. The command that will be run is:

```
${xcodebuild.path} -project ${xcodeproj.path} -scheme ${xcodeproj.scheme.name} -destination generic/platform=iOS CONFIGURATION_BUILD_DIR=${project.build.directory} archive
```

Parameters default to:
xcodebuild.path=/usr/bin/xcodebuild
xcodeproj.path=${basedir}/${project.artifactId}.xcodeproj
xcodeproj.scheme.name=${project.artifactId}
project.build.directory=target

### xcode-test

Will run xcode tests on the iphone simulator. The command that will be run is:

```
${xcodebuild.path} -project ${xcodeproj.path} -scheme ${xcodeproj.scheme.name} -sdk iphonesimulator test
```

Parameters default to:
xcodebuild.path=/usr/bin/xcodebuild
xcodeproj.path=${basedir}/${project.artifactId}.xcodeproj
xcodeproj.scheme.name=${project.artifactId}

### xcode-package-framework

Zips up the framework in the target directory as an `xcode-framework` and will attach it to the project for installation and deploying
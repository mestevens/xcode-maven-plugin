# xcode-maven-plugin

## Latest Version
0.3.1

## Description

A maven plugin that will be used to build/test/deploy/pull in iOS frameworks.

## Pom.xml information

In order to use this plugin, add the following to your pom.xml

```
<plugin>
	<groupId>ca.mestevens.ios</groupId>
	<artifactId>xcode-maven-plugin</artifactId>
	<version>${xcode.maven.plugin.version}</version>
	<extensions>true</extensions>
</plugin>
```

where `${xcode.maven.plugin}` is the version of the plugin you want to use.

## Goals

### framework-dependencies

Will grab xcode-framework/xcode-library artifacts from your maven repository and extract them in the target directory. Frameworks will be under `target/xcode-dependencies/frameworks` and libraries (and their headers) will be under `target/xcode-dependencies/libraries`.

#### Properties

* xcode.project.name
	* A string representing your xcode project name. Default value is: `${project.artifactId}.xcodeproj`
* xcode.add.dependencies
	* A boolean value that will allow try to automatically link your dependencies into your xcodeproj file. Default value is `false`

### xcode-build

Will run an xcode archive build on your xcode project and place the resulting framework in your target directory. The plugin will try and create a universal build for you based off of the architectures in your xcode project.  The command that will be run (for each architecture specified) is:

```
${xcodebuild.path} -project ${xcodeproj.path} -scheme ${xcodeproj.scheme.name} -destination generic/platform=iOS CONFIGURATION_BUILD_DIR=${project.build.directory} archive
```

#### Properties
* xcode.simulator.archs
	* A list of architectures to build for the simulator in the universal build. Possible values are `i386` and `x86_64`, which is what the property defaults to if not specified in your maven properties. So to build for strictly 32bit you would have the following property:
	
		```
		<xcode.simulator.archs>
			<arch>i386</arch>
		</xcode.simulator.archs>
		```
* xcode.device.archs
	* A list of architectures to build for devices in the universal build. If this property isn't specified the plugin will look into your xcodeproject to get the values there. If you're just using the standard architecture set (aka, the xcodeproj/project.pbxproj doesn't contain any information about architectures), this value will default to `armv7` and `arm64`.
* xcode.artifact.name
	* The name of your artifact you want to build. Defaults to `${project.artifactId}`.
* project.build.directory
	* The location of your build directory. Defaults to `target`
* xcode.project.scheme.name
	* The name of the scheme to build. Defaults to `${project.artifactId}`.
* xcode.project.path
	* The path to your xcodeproj file. Defaults to `${basedir}/${project.artifactId}.xcodeproj`
* xcodebuild.path
	* The path to the `xcodebuild` command. Defaults to `/usr/bin/xcodebuild`

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

Zips up the build in the target directory as an `xcode-framework` or `xcode-library` and will attach it to the project for installation and deploying
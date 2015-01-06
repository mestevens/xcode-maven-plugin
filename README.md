# xcode-maven-plugin

## Latest Version
0.4.0

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

In addition, if your project is an ios-app, you need to add the following tag
```
<packaging>xcode-application</packaging>
```
So that the framework-dependencies goal will be bound to your lifecycle by default.

## Documentation

Documentation (although sparse at the moment) can be found [here](http://mestevens.github.io/xcode-maven-plugin/).

## Release Notes
* 0.4.0
	* More tested code!
	* Added a new goal to set your keychain credentials if you need to.
* 0.3.2
	* Static library build/gather dependency support
	* Finer control over architectures built.
# xcode-maven-plugin

## Latest Version
0.7.1

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

## Documentation and release notes

Documentation and release notes can be found [here](http://mestevens.github.io/xcode-maven-plugin/).
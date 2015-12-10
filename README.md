# xcode-maven-plugin

[![Join the chat at https://gitter.im/mestevens/xcode-maven-plugin](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/mestevens/xcode-maven-plugin?utm_source=badge&utm_medium=badge&utm_campaign=pr-badge&utm_content=badge)

## Latest Version
0.9.2

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
# echo

Semi-supervised framework for classifying evolving data streams

Haque, A., Khan, L., Baron, M., Thuraisingham, B., & Aggarwal, C. (2016, May). Efficient handling of concept drift and concept evolution over stream data. In 2016 IEEE 32nd International Conference on Data Engineering (ICDE) (pp. 481-492). IEEE.

This implementation is compatible with pcf's Interceptable (https://github.com/douglas444/pcf).

## Requirements

* Apache Maven 3.6.3 or higher

## Maven Dependencies

* streams 1.0-SNAPSHOT (https://github.com/douglas444/streams)
* pcf-core 1.0-SNAPSHOT (https://github.com/douglas444/pcf)
* junit-jupiter 5.6.2 (available at maven repository)
* commons-math3 3.6.1 (available at maven repository)

## Install

```mvn clean install```

## Using it as a maven dependency

Once you have installed echo, import it at your maven project by including the following dependency to your pom.xml (edit the version if necessary):

```
<dependency>
  <groupId>br.com.douglas444</groupId>
  <artifactId>echo</artifactId>
  <version>1.0-SNAPSHOT</version>
</dependency>
```

Once echo is added to your project as a dependency, you can use the ECHOTest.java test file as an example of how to instantiate the ECHOController class and how to execute it.

## Build the JAR

To build without the dependencies: 

```mvn clean install```

To build with the dependencies included (except pcf-core dependency): 

```mvn clean install assembly:single```

### Observations about the commands to build the JAR

1. We configured the build process in a way that, even if you choose to build with the dependencies included, the pcf-core dependency will not be included. 
The reason is that the pcf-core dependency is already provided by the pcf-gui when the JAR is loaded through the interface.

2. If you choose to build the project without the dependencies included, make sure to load all the JAR dependencies individually at the pcf-gui interface. 
There is no need to load the pcf-core dependency though, since it is already provided by the pcf-gui.

## Using it at pcf-gui

Once you have the JAR, load it in classpath section of the pcf-gui, after that, the class ECHOInterceptable should be listed at the interface.

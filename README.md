# echo

Semi-supervised framework for classifying evolving data streams

Haque, A., Khan, L., Baron, M., Thuraisingham, B., & Aggarwal, C. (2016, May). Efficient handling of concept drift and concept evolution over stream data. In 2016 IEEE 32nd International Conference on Data Engineering (ICDE) (pp. 481-492). IEEE.

This implementation is compatible with pcf's Interceptable (https://github.com/douglas444/pcf).

## Requirements

* Apache Maven 3.6.3 or higher
* Java 8

## Maven Dependencies

* streams 1.0-SNAPSHOT (https://github.com/douglas444/streams)
* pcf-core 1.0-SNAPSHOT (https://github.com/douglas444/pcf)
* JUnit Jupiter API 5.6.2 (available at Maven Central Repository)
* Apache Commons Math 3.6.1 (available at Maven Central Repository)

## How do I install echo at my local maven repository?

*This and the next section explains how to install and use echo as a maven dependency. 
If you don't need to use it as a maven dependency, and only needs the JAR, go to the Build section.*

From the project root, execute the following command line: ```mvn clean install```

Once the process is successfully finished, the project will be installed at the local maven repository.

## How do I use echo as a maven dependency in my own project?

Once you have installed echo, import it at your maven project by including the following dependency 
to your pom.xml (edit the version if necessary):

```
<dependency>
  <groupId>br.com.douglas444</groupId>
  <artifactId>echo</artifactId>
  <version>1.0-SNAPSHOT</version>
</dependency>
```

Once echo is added to your project as a dependency, you can use the ECHOTest.java test file to check an 
example of how to instantiate the ECHOController class and how to execute it.

## How do I build the JAR from the source code?

To build the JAR without the dependencies, execute the following command line from the root folder:

```mvn clean package```

To build the JAR with the dependencies included, execute the following command line from the root folder:

```mvn clean package assembly:single```

Once the process is finished, the JAR will be available at the ```target``` folder as 
```echo.jar``` or ```echo-jar-with-dependencies.jar```.

### Observations:

* We configured the build process in a way that, even if you choose to build with the dependencies included, the pcf-core dependency will not be included. 
The reason is that the pcf-core dependency is already provided by the pcf-gui when the JAR is loaded through the interface.

* If you choose to build the project without the dependencies included, make sure to load all the JAR dependencies individually at the pcf-gui interface. 
There is no need to load the pcf-core dependency though, since it is already provided by the pcf-gui.

## How do I use echo at pcf-gui?

Once you have the JAR, load it in classpath section of the pcf-gui, after that, the class ECHOInterceptable should be listed at the interface.

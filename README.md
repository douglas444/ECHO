# echo

Semi-supervised framework for classifying evolving data streams

Haque, A., Khan, L., Baron, M., Thuraisingham, B., & Aggarwal, C. (2016, May). Efficient handling of concept drift and concept evolution over stream data. In 2016 IEEE 32nd International Conference on Data Engineering (ICDE) (pp. 481-492). IEEE.

This implementation is compatible with *pcf*'s 
*Interceptable* interface (https://github.com/douglas444/pcf).

## Requirements

* Apache Maven 3.6.3 or higher
* Java 8

## Maven Dependencies

* streams 1.0-SNAPSHOT (https://github.com/douglas444/streams)
* pcf-core 1.0-SNAPSHOT (https://github.com/douglas444/pcf)
* JUnit Jupiter API 5.6.2 (available at Maven Central Repository)
* Apache Commons Math 3.6.1 (available at Maven Central Repository)

## How to use *echo* as a *Maven* dependency?

First you need to install *echo* at your *Maven Local Repository*. 
This can be done by executing the following command line: 

```
mvn clean install
```

Once you have installed *echo*, import it at your 
*Maven* project by including the following dependency 
to your project's pom.xml file (edit the version if necessary):

```xml
<dependency>
  <groupId>br.com.douglas444</groupId>
  <artifactId>echo</artifactId>
  <version>1.0-SNAPSHOT</version>
</dependency>
```

For an example of how to use *echo* in your code, check out the file 
```src/test/java/br/com/douglas444/echo/ECHOTest.java```.

## How to use *echo* with *pcf-gui*?

First of all you need to build the project's JAR.
This can be done by executing the following command line from the root folder:

```
mvn clean package
```

If you want to build the JAR with the dependencies included, 
execute the following command line instead:

```
mvn clean package assembly:single
```

Once the process is finished, the JAR will be available at the ```target``` folder as 
```echo.jar``` or ```echo-jar-with-dependencies.jar```.

Once you have the JAR, load it in the classpath section of the *pcf-gui*. After that, 
the class *ECHOInterceptable* should be listed at the interface.

### Observations:

* We configured the JAR's build process in a way that, 
even if you choose to build with the dependencies included, 
the *pcf-core* dependency will not be included. 
The reason is that the *pcf-core* dependency is already provided 
by the *pcf-gui* when the JAR is loaded through the interface.

* If you choose to build the project without the dependencies 
included, make sure to load all the dependencies' JAR
individually at the *pcf-gui* interface. There is no need to load the *pcf-core*
dependency though, since it is already provided by the *pcf-gui*.
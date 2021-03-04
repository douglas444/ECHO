# echo

Semi-supervised framework for classifying evolving data streams

Haque, A., Khan, L., Baron, M., Thuraisingham, B., & Aggarwal, C. (2016, May). Efficient handling of concept drift and concept evolution over stream data. In 2016 IEEE 32nd International Conference on Data Engineering (ICDE) (pp. 481-492). IEEE.

## Requirements

* Apache Maven 3.6.3 or higher

## Maven Dependencies

* streams 1.0-SNAPSHOT (https://github.com/douglas444/streams)
* pcf-core 1.0-SNAPSHOT (https://github.com/douglas444/pcf)
* junit-jupiter 5.6.2 (available at maven repository)
* commons-math3 3.6.1 (available at maven repository)

## Install

```mvn clean install```

## Using it

Once you have installed echo, import it at your maven project by including the following dependency to your pom.xml (edit the version if necessary):

```
<dependency>
  <groupId>br.com.douglas444</groupId>
  <artifactId>echo</artifactId>
  <version>1.0-SNAPSHOT</version>
</dependency>
```

Once echo is added to your project as a dependency, you can use the ECHOTest.java test file as an example of how to instantiate the ECHOController class and how to execute it.

## LoadBalancer
This is an implementation of a LoadBalancer that supports pluggable providers and load-balancing strategies.

## Building and Testing
Maven v3.6.0 and Java version >= 1.8 are needed to build and test. Please follow the below steps to build and test.
 
1. git checkout https://github.com/kayyagari/loadbalancer.git
2. cd loadbalancer
3. mvn clean test

The tests will take about 30seconds to complete.

This project was tested using Java v1.8.0_162 on OS X v10.15.3. 
Please note that the tests use and heavily rely on multiple threads so the outcome may likely differ
when executed on systems with faster CPUs. All these tests are executed on machine containing 1.8 GHz Dual-Core Intel Core i5 processor.

## Documentation
Javadoc and comments have been added in the source files where things are not obvious or needed explanation or worth
having a note. All the remaining parts are not documented. Methods and variables have been named in a way to avoid
the need for additional references or diversions at the time of reading code.
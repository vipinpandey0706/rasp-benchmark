# RASP Benchmark Framework


### Summary

The RASP Benchmark Framework is an automated wrapper that enables testing of Runtime Application Self Protection (RASP) and Web Application Firewall (WAF) technology solutions on the [OWASP Benchmark](https://github.com/OWASP/Benchmark) project. It is a fully automated project that performs the steps to run Benchmark and subsequently executes a number of requests on the Java servlets therein contained. The executed requests are either safe or exploit the application.


### Motivation

Presently, the OWASP Benchmark is designed to test Static Application Security Testing (SAST) tools, Dynamic Application Security Testing (DAST) tools and Interactive Application Security Testing (IAST) tools. Support for testing of RASP and WAF tools is included on the roadmap for Benchmark. The RASP Benchmark Framework provides support for testing RASP and WAF tools, adhering to the following guidelines:

* **Vendor agnostic**
  * the RASP Benchmark Framework loads Benchmark, executes a number of requests and outputs the raw results in a standard format. There is no dependency on a specific vendor, and in fact this can simply be run on reference implementation Java.
* **Easy to use**
  * a one line command is all that's required to run the RASP Benchmark Framework;
* **Delivers industry standard independence**


The RASP Benchmark Framework utilises the [Byteman](http://byteman.jboss.org/index.html) Java tool. This is used to inject code into the Java servlets within Benchmark which allow for setting break points in the application source code to trigger an event either if a given request was successfully executed or was trapped.


### Environment And Dependencies

Since this project is built around the OWASP Benchmark, the tools and environment should firstly be installed and configured as [required to run Benchmark](https://www.owasp.org/index.php/Benchmark#tab=Quick_Start). In addition, the following should be installed:

* Apache Ant (tested using version 1.7.1): http://ant.apache.org/


### Steps To Run

    $ git clone git@github.com:waratek/rasp-benchmark
    $ cd rasp-benchmark/BenchmarkAutomation
    $ ant -Dbenchmark.jdk=/path/to/jdk/ -Drasp.args=/path/to/additional/args

* `-Dbenchmark.jdk` is optional and is used to configure the jdk on which to run Benchmark;
* `-Drasp.args` is optional and allows the user to pass in any additional flags or arguments to the Benchmark process.

If we simply run `ant` (with no additional flags) this will run the RASP Benchmark Framework on whatever Java is installed on the system.

Once completed, the raw data output will be generated in the following file:

    BenchmarkAutomation/owasp_benchmark/output_files/outputRASPBenchmark.log

### License

    Copyright 2017 Waratek Ltd.

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

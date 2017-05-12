The input files inside this directory determine what is run through the
'rasp-benchmark' codebase.

1) File 'servlets.txt' contains a list of all the OWASP Benchmark servlets
   that will be run through.

2) Files named 'BenchmarkTest*****.txt' contain the Payload, expected result
   and injection type for a given servlet. This is given in the following
   format:

   payload<split>expectedResult<split>injectionType

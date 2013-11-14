# Distributed Pi

Distributed parallel computation of fractional digits of Pi using the [BBP][1] algorithm written in Scala and JavaScript.
It contains a servlet that can be deployed on Google App Engine, which will send work units to any web browser that connetcs.
The tasks get executed inside a HTML5 Web Worker so that the user won't notice lag on the site. This would also allow for embedding inside other web sites.

## About the approach

### Performance

The performance isn't as bad as I expected from JavaScript. In fact the non-optimized JS version 
[did slightly better][2] than the non-optimized Scala version in Chrome 30. 

However, maybe WebGL could be utlized for vastly improved performance. 
There is also WebCL currently being developed, which could be useful at some point in the future.

### Algorithm

10 hex digits can be calculated per work unit (starting at 10e6), however at digit positions beyond 10e7 
rounding errors (?) lead to corruption of at least one digit, yielding about nine digts per work unit. 
The implementation does no longer return any correct results at digit position around 10e8.

As JavaScript only supports double values this problem will be hard to overcome. There are arbitrary precision
arithmetic libraries for JavaScript but there are probably to slow. This is not confirmed though.

Another approach would be [Bellard's formula], which would provide a "43% performance increase" over BBP and could be
more suitable for a JS envirionment (?).

### Server

Currently the server side is pretty hacky, doing no validation of the results and not storing the digits at all.
The results are just written to the stanard output. There is some work-in-progress though, aiming at storming them as
byte arrays. Eight hex digits would also fit nicely into an integer.

There is currently a hard coded limit of 30 seconds for a task. However, as digit positios increase this limit is no 
longer appropriate.


## Acknowledgements

Inspiration drawn from:

* [Andrew Collins - Distributed Pi](http://cgi.csc.liv.ac.uk/~acollins/pi and https://github.com/antimatter15/distributed-pi)
* [antimatter15/distributed-pi](https://github.com/antimatter15/distributed-pi)

## Build

Requires [Apache Maven](http://maven.apache.org) 3.0 or greater, and JDK 7 in order to run.

To build, run

    mvn package

To start the app, use the [App Engine Maven Plugin](http://code.google.com/p/appengine-maven-plugin/) that is already included in this demo.  Just run the command.

    mvn appengine:devserver

For further information, consult the [Java App Engine](https://developers.google.com/appengine/docs/java/overview) documentation.

To see all the available goals for the App Engine plugin, run

    mvn help:describe -Dplugin=appengine
    
    
[1]: http://en.wikipedia.org/wiki/Bailey%E2%80%93Borwein%E2%80%93Plouffe_formula
[2]: http://cell303.tumblr.com/post/63805261487/scala-vs-javsscript-performance-for-bbp

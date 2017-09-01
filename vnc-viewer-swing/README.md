# vnc-viewer-swing

This simple VNC viewer application makes use of SSHTools' RFB components. It serves as both an
example for how to build you own VNC viewer based on our components, as well as a useful VNC
viewer in it's own right.

This version uses the 'Swing' Java GUI toolkit, and is recommended for use with Java 7 and above.

## Standalone application and Example 
By default the application is built as a simple executable JAR.

```
java -jar vnc-viewer-swing.jar [<optional-arguments>] <hostname>[:<port-or-display>]
```

If you omit either the hostname (e.g :1), or the port / display (e.g. myhost), then the defaul 
hostname and / or port of locahost:5900 wwill be used.  

By using --help, the following will be displayed detailing all possible options.

```
usage: VNCViewer [-?] [-C] [-e <arg>] [-f <arg>] [-l <arg>] [-p <arg>]
A pure Java VNC viewerr
 -?,--help                 Display help
 -C,--nocopyrect           Do not use the CopyRect driver for window
                           movement (if supported)
 -e,--encodings <arg>      Comma separated list of enabled encoding
 -f,--passwordfile <arg>   A file containing the password that clients
                           must authenticate with.
 -l,--log <arg>            Log level
 -p,--password <arg>       The password that clients must authenticate
                           with.

Provided by SSHTOOLS Limited.

```



 

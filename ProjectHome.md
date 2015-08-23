Latest stable version : 1.1.8


---


The goal of this project is to provide an easy way to upload large files directly from a browser without applets or external components.

Thanks to the new html5 features including reading and slicing files, it is now possible to proceed in sending very large files over http.<br />

This library cuts the file in slices and stream them to the server. Each slice is validated using a js and java crc32 verification.

You can see exactly how the flow is processed [here](Flow.md)

The unfinished files can stay on the server for an amount of configurable days before an automatic removal.

The information related to this upload are stored on the filesystem of the server.

The writing of the files on the filesystem is optimized using servlet 3.0 asynchronous features and a rate limiter algorithm which allows the user to define a custom upload rate for each file individually.
A maximum upload rate for all the uploads of a client and a maximum overall upload rate can be configured.<br />


**1.0**
  * upload large files:
  * pause/resume
  * current upload rate per file
  * total progress of the upload per file
  * file upload state persisted on file system (user can resume an upload after an amount of configurable days)
  * crc validation of all the chunks
  * multiple file uploads within the same control
  * clean up of the pending files after a configured time
  * independent upload rate configuration per file
  * master upload rate configuration (bandwidth divided per all the current uploads)
  * per client upload rate configuration (bandwidth divided per all the current client uploads)

**1.1**
  * [listener system](http://code.google.com/p/java-large-file-uploader/source/browse/trunk/java-large-file-uploader-parent/java-large-file-uploader-jar/src/main/java/com/am/jlfu/notifier/JLFUListener.java) getting events when uploads are started,paused,resumed etc...
  * [authorizer plugin system](http://code.google.com/p/java-large-file-uploader/source/browse/trunk/java-large-file-uploader-parent/java-large-file-uploader-jar/src/main/java/com/am/jlfu/authorizer/Authorizer.java) (default to allow any client to perform anything)
  * [identifier plugin system](http://code.google.com/p/java-large-file-uploader/source/browse/trunk/java-large-file-uploader-parent/java-large-file-uploader-jar/src/main/java/com/am/jlfu/identifier/IdentifierProvider.java) (default to store id in cookie)
  * firebug detection (having firebug enabled can cause trouble)



---


This project is separated in two parts:

### Client side ###

Written in javascript, it sends a file splitted in chunks to a java web server and provides methods to be able to monitor the progress.

The javascript object JavaLargeFileUploader can manage multiple concurrent uploads.

### Server side ###

The server-side part is a Java ARchive that shall be integrated in a Web application ARchive. Using web fragments, it exposes a servlet which handles the upload.


---


### Setup ###

See [Setup](http://code.google.com/p/java-large-file-uploader/wiki/Setup).


---


### Usage ###

The client API is managed using an instance of JavaLargeFileUploader on the javascript side. Please consult [its documentation](JavaLargeFileUploader.md) to know more about how to interact with the API.



---


You can download the last war of the demo [here](http://code.google.com/p/java-large-file-uploader/downloads/detail?name=demo.war) to test it. (Note that your server has to support servlet 3.0)


---


## Known issues ##
  * does not work on Internet Explorer as ie does not provide an api to slice files.
  * might cause chrome to crash when uploading large files veryfast (client/server over a lan or same machine)([filereader api bug](http://code.google.com/p/chromium/issues/detail?id=114548)). I recommend you to limit the maximum bandwidth to 10MB/s per client
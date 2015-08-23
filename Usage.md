## Javascript ##

All the operations available on the client side are performed using a single object instance: [JavaLargeFileUploader](http://code.google.com/p/java-large-file-uploader/wiki/JavaLargeFileUploader).

## Java ##

On the Java side, there are two kind of possible interactions:

### Service ###

JLFU also provides a few methods in [JavaLargeFileUploaderService](http://code.google.com/p/java-large-file-uploader/source/browse/trunk/java-large-file-uploader-parent/java-large-file-uploader-jar/src/main/java/com/am/jlfu/staticstate/JavaLargeFileUploaderService.java) like:
```
getProgress(clientId, fileId)
updateEntity(clientId, entity)
writeEntity(clientId, entity)
writeEntity(File, entity)
getEntityIfPresent(clientId)
clearFile(clientId, fileId)
clearClient(clientId)
enableFileUploader()
disableFileUploader()
```
Consult the JavaDoc for more details about all these methods.

### Listener ###

JLFU provides a Listener system that lets you listen to pretty much everything happening on the server side:

```
onNewClient(clientId)
onClientBack(clientId)
onClientInactivity(clientId, inactivityTime)
onFileUploadEnd(clientId, fileId)
onFileUploadPrepared(clientId, fileId)
onAllFileUploadsPrepared(clientId, fileIds)
onFileUploadCancelled(clientId, fileId)
onFileUploadPaused(clientId, fileId)
onFileUploadResumed(clientId, fileId)
onFileUploadProgress(clientId, fileId, FileProgressStatus)
onFileUploaderDisabled()
onFileUploaderEnabled()
```

You can register a [JLFUListener](http://code.google.com/p/java-large-file-uploader/source/browse/trunk/java-large-file-uploader-parent/java-large-file-uploader-jar/src/main/java/com/am/jlfu/notifier/JLFUListener.java) (or an [JLFUListenerAdapter](http://code.google.com/p/java-large-file-uploader/source/browse/trunk/java-large-file-uploader-parent/java-large-file-uploader-jar/src/main/java/com/am/jlfu/notifier/JLFUListenerAdapter.java)) to the [JLFUListenerPropagator](http://code.google.com/p/java-large-file-uploader/source/browse/trunk/java-large-file-uploader-parent/java-large-file-uploader-jar/src/main/java/com/am/jlfu/notifier/JLFUListenerPropagator.java) which will propagates all the events to the registered listeners.
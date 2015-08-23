This object is the main class of the API and provides method to interact with the server.

```
jlfu = new JavaLargeFileUploader();
```

It has to be first initialized using [JavaLargeFileUploader#initialize](JavaLargeFileUploader#initialize.md)  before any other operation.

This object have a few attributes that are configurable, please see [ConfigurableProperties#javascript-attributes](ConfigurableProperties#javascript-attributes.md).

You can find a complete working example in the demo project. The html/js file managing jlfu is [here](http://code.google.com/p/java-large-file-uploader/source/browse/trunk/java-large-file-uploader-demo/src/main/webapp/index.html).

This object provides the following interaction methods:


---

### initialize ###
```
jlfu.initialize(initializationCallback, exceptionCallback);
```
This method has to be called before any other one and will initialize the object with the configuration defined on the server. It will also retrieve all the pending files that could potentially exist and provide some information about them (see PendingFile).
  * initializationCallback:
> Callback function containing a map of the files previously uploaded as parameter.
> The key of this map is the fileIdentifier.
> The value is a pendingFile (see PendingFile object description).
  * exceptionCallback:
> A callback function with a string formatted describing the exception as parameter triggered if an exception occurred.
Example:
```
jlfu.initialize(function(pendingFiles) {
	//treat pending file
}, function(message){
	//treat exception
));
```

---

### clearFileUpload ###
```
jlfu.clearFileUpload(callback);
```
Clears all state on the server.
Any pending upload will be stopped and deleted on the file system.
  * callback:
> A function with no parameter that will be executed once all files have been removed.
Example:
```
jlfu.clearFileUpload(function() {
	//do something when the call is complete
});
```

---

### cancelFileUpload ###
```
jlfu.cancelFileUpload(pendingFileId, callback);
```
Clears the file with the specified id on the server.
This upload will be stopped and the file deleted on the file system.
  * pendingFileId (string) :
> the id of the file to remove.
  * callback:
> A function with that will be executed once all files have been removed which includes the following parameters:
    1. fileId (string) : the id of the file that has been removed.
Example:
```
jlfu.cancelFileUpload("4ec798ec-eba1-4ef7-afbe-df6f4635783d", function(fileId) {
	//do something when the call is complete
});
```

---

### pauseFileUpload ###
```
jlfu.pauseFileUpload(pendingFileId, callback);
```
Pauses the file with the specified id on the server.
The upload can be resumed using [JavaLargeFileUploader#resumeFileUpload](JavaLargeFileUploader#resumeFileUpload.md).
  * pendingFileId (string) :
> the id of the file to pause.
  * callback:
> A function with that will be executed once the file has been paused containing the following parameters:
    1. pendingFile (PendingFile) : the pending file object instance that has been paused.
Example:
```
jlfu.pauseFileUpload("4ec798ec-eba1-4ef7-afbe-df6f4635783d", function(pendingFile) {
	//do something when the call is complete
});
```

---

### pauseAllFileUploads ###
```
jlfu.pauseAllFileUploads(callback);
```

_since 1.1.2_

Pauses all the uploads of all the files (does not start the files queued).
The uploads can be resumed independently using [JavaLargeFileUploader#resumeFileUpload](JavaLargeFileUploader#resumeFileUpload.md) or [JavaLargeFileUploader#resumeAllFileUploads](JavaLargeFileUploader#resumeAllFileUploads.md).
  * callback:
> A function with that will be executed for all the files once the files have been paused containing the following parameters:
    1. pendingFile (PendingFile) : the pending file object instance that has been paused.
Example:
```
jlfu.pauseAllFileUploads(function(pendingFile) {
	//do something when the call is complete
});
```

---

### resumeFileUpload ###
```
jlfu.resumeFileUpload(pendingFileId, callback);
```
Resumes the file with the specified id on the server that has been previously paused using [JavaLargeFileUploader#pauseFileUpload](JavaLargeFileUploader#pauseFileUpload.md).
  * pendingFileId (string) :
> the id of the file to resume.
  * callback:
> A function which will be executed once the file has been resumed containing the following parameters:
    1. pendingFile (PendingFile) : the pending file object instance that has been resumed.
Example:
```
jlfu.resumeFileUpload("4ec798ec-eba1-4ef7-afbe-df6f4635783d", function(pendingFile) {
	//do something when the call is complete
});
```

---

### resumeAllFileUploads ###
```
jlfu.resumeAllFileUploads(callback);
```

_since 1.1.2_

Resumes all the file that have been paused using [JavaLargeFileUploader#pauseFileUpload](JavaLargeFileUploader#pauseFileUpload.md) or [JavaLargeFileUploader#pauseAllFileUploads](JavaLargeFileUploader#pauseAllFileUploads.md).
  * callback:
> A function which will be executed for all the files once the file has been resumed containing the following parameters:
    1. pendingFile (PendingFile) : the pending file object instance that has been resumed.
Example:
```
jlfu.resumeAllFileUploads(function(pendingFile) {
	//do something when the call is complete
});
```

---

### retryFileUpload ###
```
jlfu.retryFileUpload(pendingFileId, callback);
```
If the connection is lost or another error occurs, you can retry to resume the upload for the file with the specified id.
  * pendingFileId (string) :
> the id of the file to resume.
  * callback:
> A function which will be executed once the file has been resumed containing the following parameters:
    1. success (boolean) : true if the resume is successful, false otherwise.
Example:
```
jlfu.retryFileUpload("4ec798ec-eba1-4ef7-afbe-df6f4635783d", function(ok) {
	//do something when the call is complete
});
```

---

### setRateInKiloBytes ###
```
jlfu.setRateInKiloBytes(pendingFileId, rate);
```
Specifies a maximum upload rate in kilo bytes that will be applied to the PendingFile identified by the specified id.
  * pendingFileId (string) :
> the id of the file on which this rate shall be applied.
  * rate (long) :
> the maximum rate in kilobytes.
Example:
```
jlfu.setRateInKiloBytes("4ec798ec-eba1-4ef7-afbe-df6f4635783d", 20);
```

---

### fileUploadProcess ###
```
jlfu.fileUploadProcess(referenceToFileElement, startCallback, progressCallback, finishCallback, exceptionCallback);
```
Starts or resumes the upload of all the files selected in the file input element specified as parameter.
See the Flow to get more information about how these uploads are actually processed.
Parameters:
  * referenceToFileElement (file input) :
> The input type="file" html element which contains the selection of files that will be processed.
  * startCallback:
> see [PendingFile#startCallback](PendingFile#startCallback.md).
  * progressCallback:
> > see [PendingFile#progressCallback](PendingFile#progressCallback.md).
  * finishCallback:
> > see [PendingFile#finishCallback](PendingFile#finishCallback.md).
  * exceptionCallback:
> > see [PendingFile#exceptionCallback](PendingFile#exceptionCallback.md).
Example:
```
//process the file upload
jlfu.fileUploadProcess(fileElement, 
	
	//define a start callback 
	function(pendingFile, referenceToFileElement) {
	},		
		
	//define a progressCallback
	function(pendingFile, percentageCompleted, uploadRate, estimatedRemainingTime, referenceToFileElement) {
	}, 
	
	//define a finishCallback showing the completion in the em element 
	function(pendingFile, referenceToFileElement) {
	}, 
	
	//define an exception callback
	function(message, referenceToFileElement, potentialfileIdThatCanBeUndefined) {
	}
);
```

---

### setMaxNumberOfConcurrentUploads ###
```
jlfu.setMaxNumberOfConcurrentUploads(number);
```
Specifies the maximum number of uploads that are streamed concurrently.
    * number (int) :
> > the number (between 1 and 5)

Please see [ConfigurableProperties#maxNumberOfConcurrentUploads](ConfigurableProperties#maxNumberOfConcurrentUploads.md).

Example:
```
jlfu.setMaxNumberOfConcurrentUploads(1);
```

---

### getErrorMessages ###
```
jlfu.getErrorMessages();
```
Retrieves the map of all the error messages.

Please see [ConfigurableProperties#errorMessages](ConfigurableProperties#errorMessages.md).

This map can be modified directly:

Example:
```
jlfu.getErrorMessages()[9] = "File queued!";
```

---

### setProgressPollerRefreshRate ###
```
jlfu.setProgressPollerRefreshRate(newRate);
```
Specifies the progress poller refresh rate in milliseconds.
  * newRate (int) :

> the new rate

Please see [ConfigurableProperties#progressPollerRefreshRate](ConfigurableProperties#progressPollerRefreshRate.md).

Example:
```
jlfu.setProgressPollerRefreshRate(1000);
```

---

### setAutoRetry ###
```
jlfu.setAutoRetry(autoRetryBoolean, autoRetryDelay);
```
Specifies the auto retry configuration
  * autoRetryBoolean (boolean) :
> true to enable auto retry, false to disable.
  * autoRetryDelay (int) :
> the amount of time in milliseconds between each retry.

Please see [ConfigurableProperties#autoretry](ConfigurableProperties#autoretry.md).

Example:
```
jlfu.setAutoRetry(true, 5000);
```

---

### setJavaLargeFileUploaderHost ###
```
jlfu.setJavaLargeFileUploaderHost(javaLargeFileUploaderHost);
```
Specifies the full url of the application hosting the servlet handlers.
  * javaLargeFileUploaderHost(string) :
> host url

Please see [ConfigurableProperties#javaLargeFileUploaderHost](ConfigurableProperties#javaLargeFileUploaderHost.md).

Example:
```
jlfu.setJavaLargeFileUploaderHost("http://localhost:8888/demo/");
```
## Java properties ##

Some Java properties are exposed and can be configured using a properties file.

You can add a _java-large-file-uploader.properties_ file to your classpath which can include:



#### maximumRatePerClientInKiloBytes ####

```
jlfu.ratelimiter.maximumRatePerClientInKiloBytes 
```

The maximum upload rate per client in kilo bytes. (Default is 10240 (10MB/s))

Exposed as MBean.



#### maximumOverAllRateInKiloBytes ####

```
jlfu.ratelimiter.maximumOverAllRateInKiloBytes
```

The maximum total upload rate in kilo bytes. (Default is 10240 (10MB/s))

Exposed as MBean.


#### sliceSizeInBytes ####

```
jlfu.sliceSizeInBytes
```

The size of the slice that javascript read and send. (Default is 10485760 (10MB))

#### maximumInactivityInHoursBeforeDelete ####

```
jlfu.filecleaner.maximumInactivityInHoursBeforeDelete
```

The maximum time files can stay inactive on the server (Default is 48)

Exposed as MBean.



#### uploadFolder ####

```
jlfu.defaultUploadFolder
```

The folder where the files and state are stored on the server (Default is "/JavaLargeFileUploader")



#### uploadFolderRelativePath ####

```
jlfu.uploadFolderRelativePath
```

Boolean specifying whether the defaultUploaderFolder described previously is a relative path (true) or an absolute path (false) (Default is `true`)



#### keepOriginalFileName ####

```
jlfu.keepOriginalFileName
```

Boolean specifying whether the originalFileName should be kept while preparing the upload of a file. If set to false, a name generated from a UUID will be assigned to avoid name collision. (Default is `false`)





---




## javascript-attributes ##

#### maxNumberOfConcurrentUploads ####

_default value is **5**_

All browsers are limiting the number of concurrent requests that are active against a similar domain. Chrome and Firefox limit is set to 6.

All requests are then queued but the problem is that the upload streaming requests are (as you can imagine) quite long. If that limit is more than 6, the progress poller requests would be queued and no progress would be retrieved for as long as the files are streamed.

Also, depending of what you want to achieve, it could make sense to allow a maximum of 1 concurrent request as the bandwidth is shared anyway between all the concurrent uploads.

It is advised to use a number between 1 and 5 so that there is always at least one request left for progress poller.

Use [JavaLargeFileUploader#setMaxNumberOfConcurrentUploads](JavaLargeFileUploader#setMaxNumberOfConcurrentUploads.md).

#### errorMessages ####

The error messages can be modified and/or translated by the page using the API.
They are stored inside a map that can be modified from the page.

The initial content of the map is:
```
errorMessages[0] = "Request failed for an unknown reason, please contact an administrator if the problem persists.";
errorMessages[1] = "The request is not multipart.";
errorMessages[2] = "No file to upload found in the request.";
errorMessages[3] = "CRC32 Validation of the part failed.";
errorMessages[4] = "The request cannot be processed because a parameter is missing.";
errorMessages[5] = "Cannot retrieve the configuration.";
errorMessages[6] = "No files have been selected, please select at least one file!";
errorMessages[7] = "Resuming file upload with previous slice as the last part is invalid.";
errorMessages[8] = "Error while uploading a slice of the file";
errorMessages[9] = "Maximum number of concurrent uploads reached, the upload is queued and waiting for one to finish.";
errorMessages[10] = "An exception occurred. Retrying ...";
errorMessages[11] = "Connection lost. Automatically retrying in a moment.";
errorMessages[12] = "You do not have the permission to perform this action.";
errorMessages[13] = "FireBug is enabled, you may experience issues if you do not disable it while uploading.";
errorMessages[14] = "File corrupted. An unknown error has occured and the file is corrupted. The usual cause is that the file has been modified during the upload. Please clear it and re-upload it.";
errorMessages[15] = "File is currently locked, retrying in a moment...";
errorMessages[16] = "Uploads are momentarily disabled, retrying in a moment...";
```
You can retrieve this map using [JavaLargeFileUploader#getErrorMessages](JavaLargeFileUploader#getErrorMessages.md) and modify them directly.

#### progressPollerRefreshRate ####

_default value is **1000**_

The [progress poller](Flow#Progress-Poller.md) is sending a new request to the server every _x_ amount of milliseconds, _x_ being the value of this variable.

Use [JavaLargeFileUploader#setProgressPollerRefreshRate](JavaLargeFileUploader#setProgressPollerRefreshRate.md) to set this value up.

#### autoretry ####

_default autoretry value is **true**_

_default autoretry delay is **5000**_

Whenever the connection is lost, the API can try to resume the file upload automatically.

If the autoretry value is true, it will retry every _x_ milliseconds, _x_ being the delay.

Use [JavaLargeFileUploader#setAutoRetry](JavaLargeFileUploader#setAutoRetry.md) to set these values up.

#### javaLargeFileUploaderHost ####

_default value is **empty** (same host than the server hosting the resource)_

If your javascript resources are hosted on a different machine, you can specify the server handling the calls with this value.

Watch out for same origin policy !

Use [JavaLargeFileUploader#setJavaLargeFileUploaderHost](JavaLargeFileUploader#setJavaLargeFileUploaderHost.md) to set this value.
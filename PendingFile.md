#### id ####
> (string) : identifier of the pending file upload
#### fileComplete ####
> (boolean) : specifies if the file is complete or not.
#### originalFileName ####
> (string) : the original file name.
#### fileCompletionInBytes ####
> (long) : the file completion in bytes.
#### fileCompletion ####
> (string) : the file completion formatted with its unit.
#### originalFileSizeInBytes ####
> (long) : the original file size in bytes.
#### originalFileSize ####
> (string) : the original file size formatted with its unit.
#### percentageCompleted ####
> (float) : completion of the file in percent (2 decimal places)
#### started ####
> (boolean) : true if the file is currently being uploaded, false if it is a file present on the server filesystem and can be resumed.
#### crcedBytes ####
> (long) : amount of bytes that are validated on the server
#### firstChunkCrc ####
> (object) : the crc32 information of the first bytes of the file
    * value (string) : the actual crc32 value
    * read (int) : number of bytes which have been used to compute firstChunkCrc
#### blob ####
> (object) : the file submitted with a file input element
#### paused ####
> (boolean) : specifies whether this upload is paused or not.
#### startCallback ####
> (function) : function that is called once the upload is pre initialized if the file id is not specified. It contains the following parameters:
    1. the PendingFile object
    1. the origin element
#### progressCallback ####
> (function) : function that will be called to monitor the progress
    1. the PendingFile object
    1. the percentage
    1. the current upload rate formatted as a String
    1. the estimated remaining time formatted as a String
    1. the origin element
#### finishCallback ####
> (function): function that will be called when the process is fully complete.
    1. the PendingFile object
    1. the origin element
#### exceptionCallback ####
> (function): function that will be called when an exception occurs.
    1. a string formatted describing the exception
    1. the origin element
    1. the optional PendingFile object. If the exception is related to the control and not a pending file, this parameter is undefined.
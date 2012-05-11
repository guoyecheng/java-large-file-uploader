/*
 * Constructor 
 */
function JavaLargeFileUploader() {
	var globalServletMapping = "javaLargeFileUploaderServlet";
	var uploadServletMapping = "javaLargeFileUploaderAsyncServlet";
	var pendingFiles = new Object();
	var bytesPerChunk;
	
	/*
	 * PendingFile object:
	 *	 	-fileComplete (boolean) : specifies if the file is complete or not.
	 * 		-originalFileName (string) : the original file name. 
	 * 		-fileCompletion (string) : the file completion formatted with its unit. 
	 * 		-originalFileSize (string) : the original file size formatted with its unit. 
	 * 
	 */
	
	/*
	 * 
	 * callback: 
	 * Callback function containing a map of the files previously uploaded as parameter. 
	 * 	The key of this map is the fileIdentifier. 
	 * 	The value is a pendingFile which contains some information described previously:
	 * 
	 * exceptionCallback:
	 * A callback function with a String as paraemeter triggered if an exception occurs. 
	 * 
	 */
	this.initialize = function (callback, exceptionCallback) {
		
		// get the configuration
		$.get(globalServletMapping + "?action=getConfig", function(data) {
			if (data) {
				bytesPerChunk = data.inByte;
	
				// adjust values to display
				if (!jQuery.isEmptyObject(data.pendingFiles)) {
					pendingFiles = data.pendingFiles;
					$.each(data.pendingFiles, function(key, pendingFile) {
						pendingFile.id = key;
						pendingFile.fileCompletion = getFormattedSize(pendingFile.fileCompletionInBytes);
						pendingFile.originalFileSize = getFormattedSize(pendingFile.originalFileSizeInBytes);
						pendingFile.percentageCompleted = format(pendingFile.fileCompletionInBytes * 100 / pendingFile.originalFileSizeInBytes);
						pendingFile.started = false;
					});
				}
				callback(pendingFiles);
	
			} else {
				if (exceptionCallback) {
					exceptionCallback("Error! Cannot retrieve the configuration!");
				}
			}
		});
	
	};
	
	/*
	 * clear all state on the server.
	 */
	this.clearFileUpload = function (callback) {
		pendingFiles = new Object();
		$.get(globalServletMapping + "?action=clearAll", function(e) {
			if (callback) {
				callback();
			}
		});
	};
	
	/*
	 * Specify an upload rate in kilo bytes (minimum is 10kb)
	 */
	this.setRateInKiloBytes = function (fileId, rate) {
		if(fileId && rate)  {
			$.get(globalServletMapping + "?action=setRate&rate="+rate+"&fileId="+fileId);
		}
	};
	
	/*
	 * clear the file with the specified id.
	 * provides a callback including the file id
	 */
	this.cancelFileUpload = function (fileIdI, callback) {
		var fileId = fileIdI;
		if(fileId && pendingFiles[fileId]) {
			delete pendingFiles[fileId];
			$.get(globalServletMapping + "?action=clearFile&fileId=" + fileId,	function(e) {
				if (callback) {
					callback(fileId);
				}
			});
		}
	};
	
	/*
	 * Pauses the fileUpload 
	 * provides a callback including the pendingFile element
	 */
	this.pauseFileUpload = function (fileIdI, callback) {
		var fileId = fileIdI;
		if(fileId && pendingFiles[fileId] && pendingFiles[fileId].paused === false) {
			pendingFiles[fileId].paused = true;
			$.get(globalServletMapping + "?action=pauseFile&fileId=" + fileId,	function(e) {
				if (callback) {
					callback(pendingFiles[fileId]);
				}
			});
		}
	};

	/*
	 * Resumes the fileUpload 
	 */
	this.resumeFileUpload = function (fileIdI, callback) {
		var fileId = fileIdI;
		if(fileId && pendingFiles[fileId] && pendingFiles[fileId].paused === true) {
			//set as not paused anymore
			pendingFiles[fileId].paused = false;
			//restart progress poller
			startProgressPoller(pendingFiles[fileId]);
			//and restart flow
			$.get(globalServletMapping + "?action=resumeFile&fileId=" + fileId,	function(e) {
				if (callback) {
					callback(pendingFiles[fileId]);
				}
			});
		}
	};
	
	/*
	 * referenceToFileElement: reference to the file element 
	 * 
	 * startcallback:
	 * a function that is called once the upload is pre initialized if the file id is not specified
	 * params: 1 is the fileId, 2 is the origin element
	 * 
	 * progressCallback: 
	 * a function that will be called to monitor the progress. 
	 * params: 1 is the fileId, 2 is the percentage as parameter, 3 is the current upload rate formatted, 4 is the origin element
	 * 
	 * finishCallback: 
	 * a function that will be called when the process is fully complete. params: 1 is the fileId, 2 is the origin element 
	 * 
	 * exceptionCallback:
	 * a callback triggered when an exception occurred. params: 1 exception description, 2 the reference to the file element 
	 * 
	 */
	this.fileUploadProcess = function (referenceToFileElement, startCallback, progressCallback,
			finishCallback, exceptionCallback) {

		//read the file information from input
		var allFiles = extractFilesInformation(referenceToFileElement, startCallback, progressCallback,
				finishCallback, exceptionCallback);
		//copy it to another array which is gonna contain the new files to process
		var potentialNewFiles = allFiles.slice(0);

		//try to corrolate information with our pending files
		//corrolate with filename  size and crc of first chunk
		//start resuming if we have a match
		//if we dont have any name/size math, we process an upload 
		var potentialResumeCounter = new Object();
		potentialResumeCounter.counter = 0;
		for (fileKey in allFiles) {
			var pendingFile = allFiles[fileKey];
				
			//look for a match in the pending files
			for (pendingFileToCheckKey in pendingFiles) {
				var pendingFileToCheck = pendingFiles[pendingFileToCheckKey];
				
				if (pendingFileToCheck.originalFileName == pendingFile.fileName && 
						pendingFileToCheck.originalFileSizeInBytes == pendingFile.size) {
					
					//we might have a match, adding a match counter entry
					potentialResumeCounter.counter++;
					
					//check if we have a crc, if we dont, we need to get it!
					//TODO
					//TODO
					//TODO
					//TODO
					//TODO
					//TODO
					//TODO
					//TODO
					//TODO
					//TODO
					//TODO
					//TODO
					
					// prepare the checksum of the slice
					var reader = new FileReader();
					reader.pendingFile = pendingFile;
					reader.pendingFileKey = pendingFileToCheckKey;
					reader.pendingFileToCheck = pendingFileToCheck;
					reader.onloadend = function(e) {
						//if the read is complete
					    if (e.target.readyState == FileReader.DONE) { // DONE == 2

					    	//if that pendingfile is still there
					    	if (potentialNewFiles.indexOf(e.target.pendingFile) != -1) {
					    	
						        //calculate crc of the chunk read
						        //compare it 
						    	//if it is the correct file
						    	//proceed
						        if (decimalToHexString(crc32(e.target.result)) == e.target.pendingFileToCheck.firstChunkCrc) {
									
						        	//remove it from new file ids (as we are now sure it is not a new file)
						        	potentialNewFiles.splice(potentialNewFiles.indexOf(e.target.pendingFile), 1);
									
						        	//fill pending file to check with new info
									//populate stuff retrieved in initialization 
									//TODO that is bad, just add that config to the one already in the array!
						        	e.target.pendingFile.originalFileName = e.target.pendingFileToCheck.originalFileName;
						        	e.target.pendingFile.originalFileSize = getFormattedSize(e.target.pendingFileToCheck.originalFileSizeInBytes);
						        	e.target.pendingFile.fileCompletionInBytes = e.target.pendingFileToCheck.fileCompletionInBytes;
						        	e.target.pendingFile.originalFileSizeInBytes = e.target.pendingFileToCheck.originalFileSizeInBytes;
						        	e.target.pendingFile.crcedBytes = e.target.pendingFileToCheck.crcedBytes;
									e.target.pendingFile.firstChunkCrc = e.target.pendingFileToCheck.firstChunkCrc;
									e.target.pendingFile.started = e.target.pendingFileToCheck.started;
									e.target.pendingFile.firstChunkCrcLength = e.target.pendingFileToCheck.firstChunkCrcLength;
									e.target.pendingFile.id = e.target.pendingFileKey;
									
									//put it into the pending files array
									pendingFiles[e.target.pendingFileKey] = e.target.pendingFile;
									
									
									//if that file is not already being uploaded:
									if (!e.target.pendingFile.started) {
										// process the upload
										fileResumeProcessStarter(e.target.pendingFile);
									}
								} else {
									console.log("Invalid resume crc for "+e.target.pendingFileToCheck.originalFileName+". processing as a new file.");
								}
						        
						        //if its not the correct file, it will be processed in processNewFiles
						        
						        //decrement potential resume counter
						        potentialResumeCounter.counter--;
						        
						        //and if it was the last one, process the new files.
						        if (potentialResumeCounter.counter === 0 && potentialNewFiles.length > 0) {
						        	processNewFiles(potentialNewFiles);
						        }

					    	}
					        
					    }
					};
					//read the first part chunk to calculate the crc
					reader.readAsBinaryString(slice(pendingFile.blob, 0, pendingFileToCheck.firstChunkCrcLength));
					
				} 
			}

		}
		
		//process if no pending to resume
		if (potentialResumeCounter.counter === 0 && potentialNewFiles.length > 0) {
			processNewFiles(potentialNewFiles);
		}
		
		
	};
	
	function extractFilesInformation(referenceToFileElement, startCallback, progressCallback,
			finishCallback, exceptionCallback) {
		
		//extract files
		var files = referenceToFileElement.files;
		if (!files.length) {
			if (exceptionCallback) {
				exceptionCallback("Please select at least one file!", referenceToFileElement);
			}
			return;
		}
		
		var newFiles = [];
		for (fileKey in files) {
			var file = files[fileKey];
			if (file.name && file.size) {
					
				//init the pending file object
				var pendingFile = new Object();
				pendingFile.fileName = file.name; 
				pendingFile.size = file.size;  
				pendingFile.blob = file;  
				pendingFile.progressCallback=progressCallback;
				pendingFile.referenceToFileElement= referenceToFileElement;
				pendingFile.startCallback= startCallback;
				pendingFile.finishCallback= finishCallback;
				pendingFile.exceptionCallback= exceptionCallback;
				pendingFile.paused=false;
				
				//put it into the temporary new file array as every file is potentially a new file until it is proven it is not a new file
				newFiles.push(pendingFile);
			}
		}
		return newFiles;
	}
	
	function processNewFiles(newFiles) {
		
		//for the new files left, prepare initiation
		var jsonVersionOfNewFiles = [];
		var newFilesIds = 0;
		for (pendingFileId in newFiles) {
			var pendingFile = newFiles[pendingFileId];
			
			// we need to prepare the upload
			var fileForPost = new Object();
			fileForPost.tempId=newFilesIds;
			fileForPost.fileName=pendingFile.fileName;
			fileForPost.size=pendingFile.size;
			jsonVersionOfNewFiles[fileForPost.tempId]=fileForPost;
			pendingFiles[fileForPost.tempId]=pendingFile;
			newFilesIds++;

		}

		//we have extracted the new files, we need to prepare them:
		if (jsonVersionOfNewFiles.length > 0) {
			  $.getJSON(globalServletMapping + "?action=prepareUpload", {newFiles: JSON.stringify(jsonVersionOfNewFiles)}, function(data) {
				  
				  //now populate our local entries with ids
				  $.each(data.value, function(tempIdI, fileIdI) {
					  
					  //now that we have the file id, we can assign the object
					  fileId = fileIdI;
					  pendingFile = pendingFiles[tempIdI];
					  pendingFile.id = fileId;
					  pendingFile.fileComplete = false;
					  pendingFile.originalFileSize = getFormattedSize(pendingFile.size);
					  pendingFile.originalFileSizeInBytes = pendingFile.size;
					  pendingFile.originalFileName = pendingFile.fileName;
					  pendingFile.fileCompletionInBytes = 0;
					  pendingFiles[fileId] = pendingFile;
					  delete pendingFiles[tempIdI];
					  
					  //call callback
					  if (pendingFile.startCallback) {
						  pendingFile.startCallback(pendingFile, pendingFile.referenceToFileElement);
					  }
					  
					  // and process the upload
					  fileUploadProcessStarter(pendingFile);
				  });
			});
		}
	}
	
	function fileResumeProcessStarter(pendingFile) {
		
	      //we have to ensure that the last chunk update that have not been validated is correct
		var bytesToValidates = pendingFile.fileCompletionInBytes - pendingFile.crcedBytes;
		
		//if we have bytes to validate
		if (bytesToValidates > 0) {
			
			//slice the not validated part
			var chunk = slice(pendingFile.blob, pendingFile.crcedBytes , pendingFile.fileCompletionInBytes);
			
			//append chunk to a formdata
			var formData = new FormData();
			formData.append("file", chunk);
		
			// prepare the checksum of the slice
			var reader = new FileReader();
			reader.onloadend = function(e) {
			    if (e.target.readyState == FileReader.DONE) { // DONE == 2
					//calculate crc of the chunk read
			        var digest = crc32(e.target.result);
			
			        //and send it
					$.get(globalServletMapping + "?action=verifyCrcOfUncheckedPart&fileId=" + pendingFile.id + "&crc=" + decimalToHexString(digest),	function(data) {
						//verify stuff!
						if (data) {
							pendingFile.exceptionCallback("Resuming file upload with previous slice as the last part is invalid.", pendingFile.referenceToFileElement, pendingFile.id);
							
							//and assign the completion to last verified
							pendingFile.fileCompletionInBytes = pendingFile.crcedBytes;

						} 
						//then process upload
						fileUploadProcessStarter(pendingFile);
					});
			    }
				
			};
			//read the chunk to calculate the crc
			reader.readAsBinaryString(chunk);
			
		} 
		//if we dont have bytes to validate, process
		else {
			
			//if everything is good, resume it:
			fileUploadProcessStarter(pendingFile);

		}
		
		
	}
	
	function fileUploadProcessStarter(pendingFile) {
		
		// start
		pendingFile.end = pendingFile.fileCompletionInBytes + bytesPerChunk;
		pendingFile.started = true;
		
		// launch the progress poller
		if (pendingFile.progressCallback) {
			startProgressPoller(pendingFile);
		}
	
		// then process the recursive function
		go(pendingFile);
	
	}
	
	function slice(blob, start, end) {
		if (blob.mozSlice) {
			return blob.mozSlice(start, end);
		} else {
			return blob.webkitSlice(start, end);
		}
	}
	
	function go(pendingFile) {
	
		//if file id is in the pending files:
		var chunk = slice(pendingFile.blob, pendingFile.fileCompletionInBytes, pendingFile.end);
	
		//append chunk to a formdata
		var formData = new FormData();
		formData.append("file", chunk);
	
		// prepare the checksum of the slice
		var reader = new FileReader();
		reader.onloadend = function(e) {
		    if (e.target.readyState == FileReader.DONE) { // DONE == 2
				//calculate crc of the chunk read
		        var digest = crc32(e.target.result);
		
				// prepare xhr request
				var xhr = new XMLHttpRequest();
				xhr.open('POST', uploadServletMapping + '?action=upload&fileId=' + pendingFile.id + '&crc=' + decimalToHexString(digest), true);
		
				// assign callback
				xhr.onreadystatechange = function() {
					if (xhr.readyState == 4) {
		
						if (xhr.status != 200) {
							if (pendingFile.exceptionCallback) {
								pendingFile.exceptionCallback("Error while uploading slice of byte " + pendingFile.fileCompletionInBytes + "-" + (pendingFile.fileCompletionInBytes + bytesPerChunk), pendingFile.referenceToFileElement, pendingFile.id);
							}
							return;
						}
		
						// progress
						pendingFile.fileCompletionInBytes = pendingFile.end;
						pendingFile.end = pendingFile.fileCompletionInBytes + bytesPerChunk;
		
						// check if we need to go on
						if (pendingFile.fileCompletionInBytes < pendingFile.size) {
							// recursive call
							setTimeout(go, 5, pendingFile);
						} else {
							pendingFile.fileComplete=true;
							// finish callback
							if (pendingFile.finishCallback) {
								pendingFile.finishCallback(pendingFile, pendingFile.referenceToFileElement);
							}
						}
					}
				};
		
				// send xhr request
				try {
					//only send if it is pending, because it could have been asked for cancellation while we were reading the file!
					if (pendingFiles[pendingFile.id]) {
						xhr.send(formData);
					}
				} catch (e) {
					if (pendingFile.exceptionCallback) {
						pendingFile.exceptionCallback(e.message, pendingFile.referenceToFileElement, pendingFile.id);
					}
					return;
				}
		    }
			
		};
		//read the chunk to calculate the crc
		reader.readAsBinaryString(chunk);

	
	}
	
	function getFormattedSize(size) {
		if (size < 1024) {
			return format(size) + 'B';
		} else if (size < 1048576) {
			return format(size / 1024) + 'KB';
		} else if (size < 1073741824) {
			return format(size / 1048576) + 'MB';
		} else if (size < 1099511627776) {
			return format(size / 1073741824) + 'GB';
		} else if (size < 1125899906842624) {
			return format(size / 1099511627776) + 'TB';
		}
	}
	
	function format(size) {
		return size.toFixed(2);
	}
	
	
	function startProgressPoller(pendingFile) {
	
		//process only if we have this id in the pending files
		if (pendingFiles[pendingFile.id] && !pendingFile.paused && !pendingFile.fileComplete) {
			
			// get the progress
			$.get(globalServletMapping + "?action=getProgress&fileId=" + pendingFile.id,	function(data) {
				
				//if the pending file status has not been deleted while we querying:
				if (pendingFiles[pendingFile.id] && !pendingFile.paused && !pendingFile.fileComplete) {

					//if we have information about the rate:
					if (data.uploadRate) {
						var uploadRate = getFormattedSize(data.uploadRate);
					}
					
					//keep progress
					pendingFile.percentageCompleted = format(data.progress);
					
					// specify progress
					pendingFile.progressCallback(pendingFile, pendingFile.percentageCompleted, uploadRate,
							pendingFile.referenceToFileElement);
					
					// continue if not finished
					if (data.progress < 100) {
						setTimeout(startProgressPoller, 500, pendingFile);
					}
					
				}
				
			});
			
		}
		
	}
	
	
	/*  
	===============================================================================
	Crc32 is a JavaScript function for computing the CRC32 of a string
	...............................................................................
	
	Version: 1.2 - 2006/11 - http://noteslog.com/post/crc32-for-javascript/
	
	-------------------------------------------------------------------------------
	Copyright (c) 2006 Andrea Ercolino      
	http://www.opensource.org/licenses/mit-license.php
	===============================================================================
	*/
	
	var strTable = "00000000 77073096 EE0E612C 990951BA 076DC419 706AF48F E963A535 9E6495A3 0EDB8832 79DCB8A4 E0D5E91E 97D2D988 09B64C2B 7EB17CBD E7B82D07 90BF1D91 1DB71064 6AB020F2 F3B97148 84BE41DE 1ADAD47D 6DDDE4EB F4D4B551 83D385C7 136C9856 646BA8C0 FD62F97A 8A65C9EC 14015C4F 63066CD9 FA0F3D63 8D080DF5 3B6E20C8 4C69105E D56041E4 A2677172 3C03E4D1 4B04D447 D20D85FD A50AB56B 35B5A8FA 42B2986C DBBBC9D6 ACBCF940 32D86CE3 45DF5C75 DCD60DCF ABD13D59 26D930AC 51DE003A C8D75180 BFD06116 21B4F4B5 56B3C423 CFBA9599 B8BDA50F 2802B89E 5F058808 C60CD9B2 B10BE924 2F6F7C87 58684C11 C1611DAB B6662D3D 76DC4190 01DB7106 98D220BC EFD5102A 71B18589 06B6B51F 9FBFE4A5 E8B8D433 7807C9A2 0F00F934 9609A88E E10E9818 7F6A0DBB 086D3D2D 91646C97 E6635C01 6B6B51F4 1C6C6162 856530D8 F262004E 6C0695ED 1B01A57B 8208F4C1 F50FC457 65B0D9C6 12B7E950 8BBEB8EA FCB9887C 62DD1DDF 15DA2D49 8CD37CF3 FBD44C65 4DB26158 3AB551CE A3BC0074 D4BB30E2 4ADFA541 3DD895D7 A4D1C46D D3D6F4FB 4369E96A 346ED9FC AD678846 DA60B8D0 44042D73 33031DE5 AA0A4C5F DD0D7CC9 5005713C 270241AA BE0B1010 C90C2086 5768B525 206F85B3 B966D409 CE61E49F 5EDEF90E 29D9C998 B0D09822 C7D7A8B4 59B33D17 2EB40D81 B7BD5C3B C0BA6CAD EDB88320 9ABFB3B6 03B6E20C 74B1D29A EAD54739 9DD277AF 04DB2615 73DC1683 E3630B12 94643B84 0D6D6A3E 7A6A5AA8 E40ECF0B 9309FF9D 0A00AE27 7D079EB1 F00F9344 8708A3D2 1E01F268 6906C2FE F762575D 806567CB 196C3671 6E6B06E7 FED41B76 89D32BE0 10DA7A5A 67DD4ACC F9B9DF6F 8EBEEFF9 17B7BE43 60B08ED5 D6D6A3E8 A1D1937E 38D8C2C4 4FDFF252 D1BB67F1 A6BC5767 3FB506DD 48B2364B D80D2BDA AF0A1B4C 36034AF6 41047A60 DF60EFC3 A867DF55 316E8EEF 4669BE79 CB61B38C BC66831A 256FD2A0 5268E236 CC0C7795 BB0B4703 220216B9 5505262F C5BA3BBE B2BD0B28 2BB45A92 5CB36A04 C2D7FFA7 B5D0CF31 2CD99E8B 5BDEAE1D 9B64C2B0 EC63F226 756AA39C 026D930A 9C0906A9 EB0E363F 72076785 05005713 95BF4A82 E2B87A14 7BB12BAE 0CB61B38 92D28E9B E5D5BE0D 7CDCEFB7 0BDBDF21 86D3D2D4 F1D4E242 68DDB3F8 1FDA836E 81BE16CD F6B9265B 6FB077E1 18B74777 88085AE6 FF0F6A70 66063BCA 11010B5C 8F659EFF F862AE69 616BFFD3 166CCF45 A00AE278 D70DD2EE 4E048354 3903B3C2 A7672661 D06016F7 4969474D 3E6E77DB AED16A4A D9D65ADC 40DF0B66 37D83BF0 A9BCAE53 DEBB9EC5 47B2CF7F 30B5FFE9 BDBDF21C CABAC28A 53B39330 24B4A3A6 BAD03605 CDD70693 54DE5729 23D967BF B3667A2E C4614AB8 5D681B02 2A6F2B94 B40BBE37 C30C8EA1 5A05DF1B 2D02EF8D".split(' ');
	        
    var table = new Array();
    for (var i = 0; i < strTable.length; ++i) {
      table[i] = parseInt("0x" + strTable[i]);
    }

    /* Number */
    function crc32( /* String */ str) {
            var crc = 0;
            var n = 0; //a number between 0 and 255
            var x = 0; //an hex number

            crc = crc ^ (-1);
            for( var i = 0, iTop = str.length; i < iTop; i++ ) {
                    n = ( crc ^ str.charCodeAt( i ) ) & 0xFF;
                    crc = ( crc >>> 8 ) ^ table[n];
            }
            return crc ^ (-1);
    }
	
	function decimalToHexString(number) {
	    if (number < 0) {
	        number = 0xFFFFFFFF + number + 1;
	    }
	
	    return number.toString(16).toLowerCase();
	}
}



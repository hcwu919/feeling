﻿using System;
using System.IO;
using System.Threading.Tasks;
using Microsoft.Extensions.Configuration;
using Microsoft.WindowsAzure.Storage.Auth;
using Microsoft.WindowsAzure.Storage.Blob;
using Newtonsoft.Json;

namespace HeartRate.API
{
    public class BlobManager
    {
        public IConfiguration Configuration { get; }

        public BlobManager(IConfiguration configuration)
        {
            Configuration = configuration;
        }

        public async Task UploadData(Models.HeartRate heartRate)
        {
            var storageCredentials = new StorageCredentials(Configuration["BlobContainerName"], Configuration["BlobContainerKey"]);
            var path = Configuration["BlobBeatsUri"] + heartRate.UserId + "/" + DateTime.UtcNow.ToString("yyyyMMdd_HHmmss") + ".json";
            var blobUri = new Uri(path);
            var blob = new CloudBlockBlob(blobUri, storageCredentials);
            
            using (var uploadStream = ToJsonStream(heartRate))
            {
                await UploadAsync(uploadStream, blob);
            }
        }
        
        public async Task UploadAsync(Stream source, CloudBlockBlob blob)
        {
            blob.Properties.ContentType = "application/octet-stream";
            await blob.UploadFromStreamAsync(source);
        }
        
        public static Stream ToJsonStream(object value, bool ignoreNullValue = false)
        {
            var memoryStream = new MemoryStream();
            var streamWriter = new StreamWriter(memoryStream);
            var jsonSerializer = new JsonSerializer { NullValueHandling = ignoreNullValue ? NullValueHandling.Ignore : NullValueHandling.Include };
            jsonSerializer.Serialize(streamWriter, value);
            streamWriter.Flush();
            memoryStream.Position = 0;
            return memoryStream;
        }
    }
}
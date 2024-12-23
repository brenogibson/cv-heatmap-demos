package awsPrototype.helpers;

import awsPrototype.metadatas.Constants;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.services.s3.S3AsyncClient;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Timer;
import java.util.TimerTask;

public class S3Util {

    private static S3Util instance;

    private final S3AsyncClient s3Client;
    
    private boolean connectionWithAWSIsOK;

    private final Timer timer;
    
    private S3Util() {
            System.out.println("Initializing S3 client...");
            System.out.println("Using AWS region: " + Constants.DEFAULT_S3_REGION);
            System.out.println("Using multipart configuration - threshold: " + Constants.DEFAULT_S3_MULTIPART_THRESHOLD + " bytes, minimum part size: " + Constants.DEFAULT_S3_MINIMUM_PART_SIZE + " bytes");
            
            ProfileCredentialsProvider credentialsProvider = ProfileCredentialsProvider.builder().build();
            System.out.println("Created credentials provider using default profile");
            
            try {
                credentialsProvider.resolveCredentials();
                System.out.println("Successfully resolved AWS credentials");
            } catch (Exception e) {
                System.err.println("Failed to resolve AWS credentials: " + e.getMessage());
            }
            
            s3Client = S3AsyncClient.builder()
            .credentialsProvider(credentialsProvider)
            .crossRegionAccessEnabled(true)
            .region(Constants.DEFAULT_S3_REGION)
            .multipartEnabled(true)
            .multipartConfiguration(b -> b
                .thresholdInBytes(Constants.DEFAULT_S3_MULTIPART_THRESHOLD)
                .minimumPartSizeInBytes(Constants.DEFAULT_S3_MINIMUM_PART_SIZE))
            .build();
            System.out.println("S3 client initialized successfully with cross-region access enabled");
            // check connection later
            connectionWithAWSIsOK = false;
            timer = new Timer();
            System.out.println("Timer initialized for auto-sync operations");
        }
    
        public static S3Util getInstance() {
            System.out.println("Getting S3Util instance...");
            if (instance == null) 
                synchronized(S3Util.class) {
                    if (instance == null) {
                        System.out.println("Creating new S3Util instance - first time initialization");
                        instance = new S3Util();
                    }
                }
            return instance;
        }
    
    public void startAutoSync() {
        System.out.println("Starting auto sync configuration...");
        if (Constants.DEFAULT_S3_CONTINUOUS_RECURRENCY * Constants.DEFAULT_S3_DELAY_START > 0 ) {
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    syncS3Files();
                }
            };
            System.out.println(String.format("Scheduling sync task with initial delay %d seconds and recurrency interval %d seconds", 
                Constants.DEFAULT_S3_DELAY_START, Constants.DEFAULT_S3_CONTINUOUS_RECURRENCY));
            timer.schedule(task, 1000 * Constants.DEFAULT_S3_DELAY_START, 1000 * Constants.DEFAULT_S3_CONTINUOUS_RECURRENCY); 
        } else {
            System.out.println("Auto sync disabled - recurrency or delay start is set to 0");
        }
    }        

    public boolean isConnectionWithAWSIsOK() {
        return connectionWithAWSIsOK;
    }

    public void syncS3Files() {
        System.out.println("Starting S3 files sync operation...");
        System.out.println("Listing objects in bucket: " + Constants.DEFAULT_S3_BUCKET_NAME + " with prefix: " + Constants.DEFAULT_S3_VIDEOS_PATH);
        s3Client.listObjectsV2(req -> req.bucket(Constants.DEFAULT_S3_BUCKET_NAME).prefix(Constants.DEFAULT_S3_VIDEOS_PATH).delimiter("/"))
            .thenAccept(response -> {
                System.out.println(String.format("Found %d objects in bucket %s", response.contents().size(), Constants.DEFAULT_S3_BUCKET_NAME));
                response.contents().forEach(content -> {
                    String objectName = content.key();
                    if (objectName.endsWith(".mp4")) {
                        String fileName = objectName.substring(Constants.DEFAULT_S3_VIDEOS_PATH.length());
                        System.out.println(String.format("Processing video file: %s (Last modified: %s, Size: %d bytes)", 
                            fileName, content.lastModified(), content.size()));
                        if (!VideoFileUtil.getInstance().isVideoFileDownloaded(fileName)) {
                            System.out.println(String.format("Initiating download for video file: %s", fileName));
                            S3Util.getInstance().downloadS3File(fileName);
                        } else {
                            System.out.println(String.format("Video file already exists locally, updating file list: %s", fileName));
                            VideoFileUtil.getInstance().updateVideoFileList(fileName);
                        }
                    }
                });
                this.connectionWithAWSIsOK = true;
                System.out.println("S3 files sync completed successfully - connection status: OK");
        })
        .exceptionally(error -> {
            System.err.println(String.format("Error listing objects from bucket [%s]: %s - connection status: FAILED", 
                Constants.DEFAULT_S3_BUCKET_NAME, error.getMessage()));
            this.connectionWithAWSIsOK = false;
            return null;
        });
    }

    public void downloadS3File(String objectName) {
        System.out.println("Initiating download process for object: " + objectName);
        Path localVideoPath = Paths.get(VideoFileUtil.getInstance().getDefaultVideoFilePath(),objectName);
        String videoS3FilePath = Constants.DEFAULT_S3_VIDEOS_PATH.concat(objectName);
        String jsonObjectName = objectName.replaceAll("\\.mp4$", ".json");
        Path localJsonPath = Paths.get(VideoFileUtil.getInstance().getDefaultVideoFilePath(),jsonObjectName);
        String jsonS3FilePath = Constants.DEFAULT_S3_VIDEOS_PATH.concat(jsonObjectName);

        System.out.println("Starting video file download from S3: " + videoS3FilePath + " to local path: " + localVideoPath);
        // download the video file first...
        s3Client.getObject(req -> req.bucket(Constants.DEFAULT_S3_BUCKET_NAME).key(videoS3FilePath), localVideoPath)
            .thenAccept(responseOfDownloadVideoFile -> {
                    System.out.println("Video file downloaded successfully to: " + localVideoPath);
                    System.out.println("Starting JSON file download from S3: " + jsonS3FilePath + " to local path: " + localJsonPath);
                    // and then, download async the json file...
                    s3Client.getObject(req -> req.bucket(Constants.DEFAULT_S3_BUCKET_NAME).key(jsonS3FilePath), localJsonPath)
                    .thenAccept(response -> {
                        System.out.println("JSON file downloaded successfully to: " + localJsonPath);
                        System.out.println("JSON file downloaded successfully");
                        // and then, if the two files has been downloaded ok, either pre-load the object on the cache or wait for the first usage
                        if (Constants.DEFAULT_PRELOAD_S3_OBJECTS_ON_CACHE) {
                                System.out.println("Pre-loading video file into cache (enabled by configuration): " + objectName);
                                System.out.println("Pre-loading video file into cache: " + objectName);
                                VideoFileUtil.getInstance().loadVideoFromFile(objectName);
                                System.out.println("Video file successfully loaded into cache - connection status: OK");
                                System.out.println("Video file successfully loaded into cache");
                                System.err.println(String.format("Error loading video into cache [%s] from bucket [%s]: %s - connection status: FAILED", 
                                    jsonS3FilePath, Constants.DEFAULT_S3_BUCKET_NAME, error.getMessage()));
                                System.err.println(String.format("Error downloading object [%s] from the bucket [%s]: %s", jsonS3FilePath, Constants.DEFAULT_S3_BUCKET_NAME, error.getMessage()));
                                this.connectionWithAWSIsOK = false;
                        }
                        else {
                            System.out.println("Skipping cache pre-load (disabled by configuration)");
                        }
                        
                    });
                        System.err.println(String.format("Error downloading JSON file [%s] from bucket [%s]: %s - connection status: FAILED", 
                            jsonS3FilePath, Constants.DEFAULT_S3_BUCKET_NAME, error.getMessage()));
                        System.err.println(String.format("Error downloading object [%s] from the bucket [%s]: %s", jsonS3FilePath, Constants.DEFAULT_S3_BUCKET_NAME, error.getMessage()));
                        this.connectionWithAWSIsOK = false;
                        return null;
                    });
                System.err.println(String.format("Error downloading video file [%s] from bucket [%s]: %s - connection status: FAILED", 
                    videoS3FilePath, Constants.DEFAULT_S3_BUCKET_NAME, error.getMessage()));
                System.err.println(String.format("Error downloading object [%s] from the bucket [%s]: %s", videoS3FilePath, Constants.DEFAULT_S3_BUCKET_NAME, error.getMessage()));
                this.connectionWithAWSIsOK = false;
                return null;
    
    }

}

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
            s3Client = S3AsyncClient.builder()
            .credentialsProvider(ProfileCredentialsProvider.builder().build())
            .crossRegionAccessEnabled(true)
            .region(Constants.DEFAULT_S3_REGION)
            .multipartEnabled(true)
            .multipartConfiguration(b -> b
                .thresholdInBytes(Constants.DEFAULT_S3_MULTIPART_THRESHOLD)
                .minimumPartSizeInBytes(Constants.DEFAULT_S3_MINIMUM_PART_SIZE))
            .build();
            System.out.println("S3 client initialized successfully");
            // check connection later
            connectionWithAWSIsOK = false;
            timer = new Timer();
            System.out.println("Timer initialized");
        }
    
        public static S3Util getInstance() {
            System.out.println("Getting S3Util instance...");
            if (instance == null) 
                synchronized(S3Util.class) {
                    if (instance == null) {
                        System.out.println("Creating new S3Util instance");
                        instance = new S3Util();
                    }
                }
            return instance;
        }
    
    public void startAutoSync() {

        System.out.println("Starting auto sync...");
        if (Constants.DEFAULT_S3_CONTINUOUS_RECURRENCY * Constants.DEFAULT_S3_DELAY_START > 0 ) {
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    syncS3Files();
                }
            };
            System.out.println(String.format("Scheduling sync task with delay %d seconds and recurrency %d seconds", 
                Constants.DEFAULT_S3_DELAY_START, Constants.DEFAULT_S3_CONTINUOUS_RECURRENCY));
            timer.schedule(task, 1000 * Constants.DEFAULT_S3_DELAY_START, 1000 * Constants.DEFAULT_S3_CONTINUOUS_RECURRENCY); 
        }
    }        

    public boolean isConnectionWithAWSIsOK() {
        return connectionWithAWSIsOK;
    }

    public void syncS3Files() {
        System.out.println("Starting S3 files sync...");
        s3Client.listObjectsV2(req -> req.bucket(Constants.DEFAULT_S3_BUCKET_NAME).prefix(Constants.DEFAULT_S3_VIDEOS_PATH).delimiter("/"))
            .thenAccept(response -> {
                System.out.println(String.format("Found %d objects in bucket", response.contents().size()));
                response.contents().forEach(content -> {
                    String objectName = content.key();
                    if (objectName.endsWith(".mp4")) {
                        String fileName = objectName.substring(Constants.DEFAULT_S3_VIDEOS_PATH.length());
                        System.out.println(String.format("Processing video file: %s", fileName));
                        if (!VideoFileUtil.getInstance().isVideoFileDownloaded(fileName)) {
                            System.out.println(String.format("Downloading video file: %s", fileName));
                            S3Util.getInstance().downloadS3File(fileName);
                        } else {
                            System.out.println(String.format("Video file already exists, updating list: %s", fileName));
                            VideoFileUtil.getInstance().updateVideoFileList(fileName);
                        }
                    }
                });
                this.connectionWithAWSIsOK = true;
                System.out.println("S3 files sync completed successfully");
        })
        .exceptionally(error -> {
            System.err.println(String.format("Error listing objects from the bucket [%s]: %s",Constants.DEFAULT_S3_BUCKET_NAME, error.getMessage()));
            this.connectionWithAWSIsOK = false;
            return null;
        });
    }

    public void downloadS3File(String objectName) {
        System.out.println("Starting download of S3 files for object: " + objectName);
        Path localVideoPath = Paths.get(VideoFileUtil.getInstance().getDefaultVideoFilePath(),objectName);
        String videoS3FilePath = Constants.DEFAULT_S3_VIDEOS_PATH.concat(objectName);
        String jsonObjectName = objectName.replaceAll("\\.mp4$", ".json");
        Path localJsonPath = Paths.get(VideoFileUtil.getInstance().getDefaultVideoFilePath(),jsonObjectName);
        String jsonS3FilePath = Constants.DEFAULT_S3_VIDEOS_PATH.concat(jsonObjectName);

        System.out.println("Downloading video file from S3: " + videoS3FilePath);
        // download the video file first...
        s3Client.getObject(req -> req.bucket(Constants.DEFAULT_S3_BUCKET_NAME).key(videoS3FilePath), localVideoPath)
            .thenAccept(responseOfDownloadVideoFile -> {
                    System.out.println("Video file downloaded successfully, now downloading JSON file: " + jsonS3FilePath);
                    // and then, download async the json file...
                    s3Client.getObject(req -> req.bucket(Constants.DEFAULT_S3_BUCKET_NAME).key(jsonS3FilePath), localJsonPath)
                    .thenAccept(response -> {
                        System.out.println("JSON file downloaded successfully");
                        // and then, if the two files has been downloaded ok, either pre-load the object on the cache or wait for the first usage
                        if (Constants.DEFAULT_PRELOAD_S3_OBJECTS_ON_CACHE)
                            try {
                                System.out.println("Pre-loading video file into cache: " + objectName);
                                VideoFileUtil.getInstance().loadVideoFromFile(objectName);
                                this.connectionWithAWSIsOK = true;
                                System.out.println("Video file successfully loaded into cache");
                            } catch (IOException error) {
                                System.err.println(String.format("Error downloading object [%s] from the bucket [%s]: %s", jsonS3FilePath, Constants.DEFAULT_S3_BUCKET_NAME, error.getMessage()));
                                this.connectionWithAWSIsOK = false;
                            }
                        
                    })
                    .exceptionally(error -> {
                        System.err.println(String.format("Error downloading object [%s] from the bucket [%s]: %s", jsonS3FilePath, Constants.DEFAULT_S3_BUCKET_NAME, error.getMessage()));
                        this.connectionWithAWSIsOK = false;
                        return null;
                    });
                })
            .exceptionally(error -> {
                System.err.println(String.format("Error downloading object [%s] from the bucket [%s]: %s", videoS3FilePath, Constants.DEFAULT_S3_BUCKET_NAME, error.getMessage()));
                this.connectionWithAWSIsOK = false;
                return null;
            });
    
    }

}

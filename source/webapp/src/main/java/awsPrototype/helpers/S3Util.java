package awsPrototype.helpers;

import awsPrototype.metadatas.Constants;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.services.s3.S3AsyncClient;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;
import java.util.logging.Level;

public class S3Util {

    private static S3Util instance;
    private static final Logger LOGGER = Logger.getLogger(S3Util.class.getName());

    private final S3AsyncClient s3Client;
    
    private boolean connectionWithAWSIsOK;

    private final Timer timer;
    
    private S3Util() {
            ProfileCredentialsProvider credentialsProvider = ProfileCredentialsProvider.builder().build();
            LOGGER.info("Initializing S3 client with credentials: " + credentialsProvider.toString());
            
            s3Client = S3AsyncClient.builder()
            .credentialsProvider(credentialsProvider)
            .crossRegionAccessEnabled(true)
            .region(Constants.DEFAULT_S3_REGION)
            .multipartEnabled(true)
            .multipartConfiguration(b -> b
                .thresholdInBytes(Constants.DEFAULT_S3_MULTIPART_THRESHOLD)
                .minimumPartSizeInBytes(Constants.DEFAULT_S3_MINIMUM_PART_SIZE))
            .build();
            
            LOGGER.info("S3 client initialized with region: " + Constants.DEFAULT_S3_REGION);
            // check connection later
            connectionWithAWSIsOK = false;
            timer = new Timer();
        }
    
        public static S3Util getInstance() {
            if (instance == null) 
                synchronized(S3Util.class) {
                    if (instance == null) {
                        LOGGER.info("Creating new S3Util instance");
                        instance = new S3Util();
                    }
                }
            return instance;
        }
    
    public void startAutoSync() {
        LOGGER.info("Starting auto sync with delay: " + Constants.DEFAULT_S3_DELAY_START + "s and recurrency: " + Constants.DEFAULT_S3_CONTINUOUS_RECURRENCY + "s");

        if (Constants.DEFAULT_S3_CONTINUOUS_RECURRENCY * Constants.DEFAULT_S3_DELAY_START > 0 ) {
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    syncS3Files();
                }
            };
             timer.schedule(task, 1000 * Constants.DEFAULT_S3_DELAY_START, 1000 * Constants.DEFAULT_S3_CONTINUOUS_RECURRENCY); 
            }
    }        
        public boolean isConnectionWithAWSIsOK() {
            return connectionWithAWSIsOK;
        }

        public void syncS3Files() {
            LOGGER.info("Starting S3 file sync from bucket: " + Constants.DEFAULT_S3_BUCKET_NAME);
            s3Client.listObjectsV2(req -> req.bucket(Constants.DEFAULT_S3_BUCKET_NAME).prefix(Constants.DEFAULT_S3_VIDEOS_PATH).delimiter("/"))
                .thenAccept(response -> {
                    response.contents().forEach(content -> {
                        String objectName = content.key();
                        if (objectName.endsWith(".mp4")) {
                            String fileName = objectName.substring(Constants.DEFAULT_S3_VIDEOS_PATH.length());
                            if (!VideoFileUtil.getInstance().isVideoFileDownloaded(fileName)) {
                                 LOGGER.info("Downloading new file: " + fileName);
                                 S3Util.getInstance().downloadS3File(fileName);
                            } else {
                                LOGGER.fine("File already exists: " + fileName);
                                VideoFileUtil.getInstance().updateVideoFileList(fileName);
                            }
                        }
                    });
                    this.connectionWithAWSIsOK = true;
                    LOGGER.info("S3 file sync completed successfully");
            })
            .exceptionally(error -> {
                LOGGER.log(Level.SEVERE, String.format("Error listing objects from the bucket [%s]: %s",Constants.DEFAULT_S3_BUCKET_NAME, error.getMessage()));
                this.connectionWithAWSIsOK = false;
                return null;
            });
    }

    public void downloadS3File(String objectName) {
        Path localVideoPath = Paths.get(VideoFileUtil.getInstance().getDefaultVideoFilePath(),objectName);
        String videoS3FilePath = Constants.DEFAULT_S3_VIDEOS_PATH.concat(objectName);
        String jsonObjectName = objectName.replaceAll("\\.mp4$", ".json");
        Path localJsonPath = Paths.get(VideoFileUtil.getInstance().getDefaultVideoFilePath(),jsonObjectName);
        String jsonS3FilePath = Constants.DEFAULT_S3_VIDEOS_PATH.concat(jsonObjectName);

        LOGGER.info("Starting download of video file: " + videoS3FilePath);
        // download the video file first...
        s3Client.getObject(req -> req.bucket(Constants.DEFAULT_S3_BUCKET_NAME).key(videoS3FilePath), localVideoPath)
            .thenAccept(responseOfDownloadVideoFile -> {
                    LOGGER.info("Video file downloaded successfully, starting JSON file download: " + jsonS3FilePath);
                    // and then, download async the json file...
                    s3Client.getObject(req -> req.bucket(Constants.DEFAULT_S3_BUCKET_NAME).key(jsonS3FilePath), localJsonPath)
                    .thenAccept(response -> {
                        // and then, if the two files has been downloaded ok, either pre-load the object on the cache or wait for the first usage
                        if (Constants.DEFAULT_PRELOAD_S3_OBJECTS_ON_CACHE)
                            try {
                                LOGGER.info("Preloading video file to cache: " + objectName);
                                VideoFileUtil.getInstance().loadVideoFromFile(objectName);
                                this.connectionWithAWSIsOK = true;
                            } catch (IOException error) {
                                LOGGER.log(Level.SEVERE, String.format("Error downloading object [%s] from the bucket [%s]: %s", jsonS3FilePath, Constants.DEFAULT_S3_BUCKET_NAME, error.getMessage()));
                                this.connectionWithAWSIsOK = false;
                            }
                        
                    })
                    .exceptionally(error -> {
                        LOGGER.log(Level.SEVERE, String.format("Error downloading object [%s] from the bucket [%s]: %s", jsonS3FilePath, Constants.DEFAULT_S3_BUCKET_NAME, error.getMessage()));
                        this.connectionWithAWSIsOK = false;
                        return null;
                    });
                })
            .exceptionally(error -> {
                LOGGER.log(Level.SEVERE, String.format("Error downloading object [%s] from the bucket [%s]: %s", videoS3FilePath, Constants.DEFAULT_S3_BUCKET_NAME, error.getMessage()));
                this.connectionWithAWSIsOK = false;
                return null;
            });
    
    }

}

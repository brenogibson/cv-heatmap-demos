package awsPrototype.metadatas;

import java.nio.file.Path;
import java.util.Properties;

import software.amazon.awssdk.regions.Region;

public class Constants {

    private static final String PROPERTIES_FILENAME = "application.properties";

    public static String EMBEDED_VIDEO_FILE_PATH = "/static/mp4/in-store-video.mp4";
    public static String EMBEDED_JSON_FILE_PATH = "/static/json/detectedPersons.json";
    public static String DEFAULT_EMBEDED_VIDEO_FILE_NAME = "in-store-video.mp4";
    public static String DEFAULT_EMBEDED_JSON_FILE_NAME = "in-store-video.json";
    public static Boolean PRE_LOAD_DEFAULT_VIDEO_AND_JSON = true;
    
    public static Long MB = 1024L*1024L;
    
    public static Integer BLOCK_SIZE_256K = 256*1024; //256K

    public static Integer BLOCK_SIZE = 1024*1024; //1MB

    public static Integer MAX_VIDEO_CACHE_ENTRIES = 100;

    public static String DEFAULT_TMP_DIR_NAME = "video.analytics.tmp";

    public static Region DEFAULT_S3_REGION = Region.US_EAST_1;
    public static Long DEFAULT_S3_MULTIPART_THRESHOLD = 16 * Constants.MB;
    public static Long DEFAULT_S3_MINIMUM_PART_SIZE = 10 * Constants.MB;
    public static String DEFAULT_S3_BUCKET_NAME = "heatmap-demo";
    public static String DEFAULT_S3_VIDEOS_PATH = "output/";
    public static Boolean DEFAULT_PRELOAD_S3_OBJECTS_ON_CACHE = false;
    public static Integer DEFAULT_S3_DELAY_START = 0;
    public static Integer DEFAULT_S3_CONTINUOUS_RECURRENCY = 15;

    public static Region DEFAULT_SQS_REGION = Region.US_EAST_1;
    public static String DEFAULT_SQS_QUEUE_URL = "https://sqs.us-east-1.amazonaws.com/351691338195/heatmap-demo-rekognition-pp-results";
    public static Integer DEFAULT_SQS_WAIT_TIME = 20;
    public static Integer DEFAULT_SQS_DELAY_START = 5;
    public static Integer DEFAULT_SQS_CONTINUOUS_RECURRENCY = 5;

    static {
        Properties props = new Properties();
        try {
            props.load(Constants.class.getClassLoader().getResourceAsStream(PROPERTIES_FILENAME));

            EMBEDED_VIDEO_FILE_PATH = props.getProperty("embeded.video.file.path", EMBEDED_VIDEO_FILE_PATH);
            EMBEDED_JSON_FILE_PATH = props.getProperty("embeded.json.file.path", EMBEDED_JSON_FILE_PATH);
            DEFAULT_EMBEDED_VIDEO_FILE_NAME = props.getProperty("embeded.json.file.path", DEFAULT_EMBEDED_VIDEO_FILE_NAME);
            DEFAULT_EMBEDED_JSON_FILE_NAME = props.getProperty("embeded.json.file.path", DEFAULT_EMBEDED_JSON_FILE_NAME);
            PRE_LOAD_DEFAULT_VIDEO_AND_JSON = Boolean.parseBoolean(props.getProperty("pre.load.default.video.and.json", PRE_LOAD_DEFAULT_VIDEO_AND_JSON.toString()));
            

            BLOCK_SIZE = Integer.parseInt(props.getProperty("block.size", BLOCK_SIZE.toString()));
            
            MAX_VIDEO_CACHE_ENTRIES = Integer.parseInt(props.getProperty("max.video.cache.entries", MAX_VIDEO_CACHE_ENTRIES.toString()));
            DEFAULT_TMP_DIR_NAME = props.getProperty("tmp.dir.name", DEFAULT_TMP_DIR_NAME);

            DEFAULT_S3_REGION = Region.of(props.getProperty("s3.region", DEFAULT_S3_REGION.toString()));
            DEFAULT_S3_MULTIPART_THRESHOLD = Long.parseLong(props.getProperty("s3.multipart.threshold", DEFAULT_S3_MULTIPART_THRESHOLD.toString()));
            DEFAULT_S3_MINIMUM_PART_SIZE = Long.parseLong(props.getProperty("s3.minimum.part.size", DEFAULT_S3_MINIMUM_PART_SIZE.toString()));
            DEFAULT_S3_BUCKET_NAME = props.getProperty("s3.bucket.name", DEFAULT_S3_BUCKET_NAME);
            DEFAULT_S3_VIDEOS_PATH = props.getProperty("s3.videos.path", DEFAULT_S3_VIDEOS_PATH);
            DEFAULT_PRELOAD_S3_OBJECTS_ON_CACHE = Boolean.parseBoolean(props.getProperty("preload.s3.objects.on.cache", DEFAULT_PRELOAD_S3_OBJECTS_ON_CACHE.toString()));
            DEFAULT_S3_DELAY_START = Integer.parseInt(props.getProperty("sqs.delay.start", DEFAULT_S3_DELAY_START.toString()));
            DEFAULT_S3_CONTINUOUS_RECURRENCY = Integer.parseInt(props.getProperty("sqs.continuous.recurrency", DEFAULT_S3_CONTINUOUS_RECURRENCY.toString()));

            DEFAULT_SQS_REGION = Region.of(props.getProperty("sqs.region", DEFAULT_SQS_REGION.toString()));
            DEFAULT_SQS_QUEUE_URL = props.getProperty("sqs.queue.url", DEFAULT_SQS_QUEUE_URL);
            DEFAULT_SQS_WAIT_TIME = Integer.parseInt(props.getProperty("sqs.wait.time", DEFAULT_SQS_WAIT_TIME.toString()));
            DEFAULT_SQS_DELAY_START = Integer.parseInt(props.getProperty("sqs.delay.start", DEFAULT_SQS_DELAY_START.toString()));
            DEFAULT_SQS_CONTINUOUS_RECURRENCY = Integer.parseInt(props.getProperty("sqs.continuous.recurrency", DEFAULT_SQS_CONTINUOUS_RECURRENCY.toString()));

        } catch (Exception e) {
            System.err.println(String.format("Error loading properties from the file [%s], using default values: %s",PROPERTIES_FILENAME, e.getMessage()));
        }
    }


}

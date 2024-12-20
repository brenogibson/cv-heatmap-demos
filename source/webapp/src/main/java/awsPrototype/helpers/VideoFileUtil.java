package awsPrototype.helpers;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListMap;

import org.rapidoid.log.Log;

import awsPrototype.metadatas.Constants;
import awsPrototype.metadatas.VideoRawData;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class VideoFileUtil {

    private static VideoFileUtil instance;

    private final VideoCacheUtil videoCacheUtil;

    private String defaultVideoFilePath;

    private ConcurrentSkipListMap<String,Object> downloadedVideoFilesList;

    private boolean isPreInitialized = false;

    private VideoFileUtil() {
        this.videoCacheUtil = VideoCacheUtil.getInstance();
        
        this.downloadedVideoFilesList = new ConcurrentSkipListMap<String,Object>();
        
        setDefaultVideoFilePath(getTempFilePath());

        Log.info(String.format("Using the path [%s] as default temp folder for the mp4 and json files.",defaultVideoFilePath));
    }

    public static VideoFileUtil getInstance() {
        if (instance == null) 
        synchronized (VideoFileUtil.class) {
            if (instance == null) {
                instance = new VideoFileUtil();
            }
        } 
        return instance;
    }
 
    private static String getTempFilePath() {
        String baseDir = System.getProperty("java.io.tmpdir");
        String tempDirName = Constants.DEFAULT_TMP_DIR_NAME;
        String tempDir = baseDir.concat(tempDirName);
        return tempDir;
    }

    public void setDefaultVideoFilePath(String defaultVideoFilePath) {
        Path videoDirPath = Paths.get(defaultVideoFilePath);
        if (!Files.exists(videoDirPath)) {
            try {
                Files.createDirectories(videoDirPath);
            } catch (IOException e) {
                System.err.println(String.format("Error creating directory [%s]: %s", videoDirPath.toAbsolutePath(), e.getMessage()));
                return;
            }
        }

        this.defaultVideoFilePath = defaultVideoFilePath;
    }

    public String getDefaultVideoFilePath() {
        return defaultVideoFilePath;
    }

    public void preInitializeDownloadedVideoFilesList() {
        if (!isPreInitialized) {
            isPreInitialized = true;
            if (Constants.PRE_LOAD_DEFAULT_VIDEO_AND_JSON) {
                try {
                    streamResourceToFile(Constants.EMBEDED_VIDEO_FILE_PATH, Constants.DEFAULT_EMBEDED_VIDEO_FILE_NAME);
                    streamResourceToFile(Constants.EMBEDED_JSON_FILE_PATH, Constants.DEFAULT_EMBEDED_JSON_FILE_NAME);
                } catch (IOException e) {
                    System.err.println(String.format("Error pre-loading default video and json files: %s", e.getMessage()));
                }
            }
            File folder = new File(defaultVideoFilePath);
            File[] listOfFiles = folder.listFiles();
            for (File file : listOfFiles) {
                if (file.isFile()&&file.getName().endsWith(".mp4")) {
                    downloadedVideoFilesList.put(file.getName(), file.getName());
                    if (Constants.DEFAULT_PRELOAD_S3_OBJECTS_ON_CACHE)
                        try {
                            loadVideoFromFile(file.getName());
                        } catch (IOException e) {
                            // TODO: silent, just try and let it goes
                        }
                }
            }
        }
    }


    private void streamResourceToFile(String resourcePath, String targetFileName) throws IOException {
        File outputFile = new File(defaultVideoFilePath, targetFileName);

        if (!outputFile.exists()) {
            // Get input stream from resource
            BufferedInputStream resourceStream = new BufferedInputStream(
                VideoFileUtil.class.getResourceAsStream(resourcePath));
 
            // Copy resource to file using Java NIO
            Files.copy(resourceStream, outputFile.toPath());
            
            // Close resource stream
            resourceStream.close();
        }
        
    }    

    public List<String> listVideoFileNames() {
        LinkedList<String> files = new LinkedList<String>();
        downloadedVideoFilesList.keySet().forEach(f->files.add(f));
        return files;

    }

    public void updateVideoFileList(String videoName) {
        downloadedVideoFilesList.put(videoName, videoName);
    }

    public void loadVideoFromFile(String videoName) throws IOException, FileNotFoundException {
        
        BufferedInputStream bisInStoreVideoMp4;
        File fileVideo = new File(defaultVideoFilePath,videoName);
        int bufferSize = (int)fileVideo.length();
        bisInStoreVideoMp4 = new BufferedInputStream(new FileInputStream(fileVideo));
        byte[] bufferVideoStream = new byte[bufferSize];
        bisInStoreVideoMp4.read(bufferVideoStream, 0, bufferSize);
        bisInStoreVideoMp4.close();
        
        String jsonName = videoName.replaceAll("\\.mp4$", ".json");
        BufferedInputStream bisInStoreJson;
        File fileJson = new File(defaultVideoFilePath,jsonName);
        int bufferJsonSize = (int)fileJson.length();
        bisInStoreJson = new BufferedInputStream(new FileInputStream(fileJson));
        byte[] bufferJson = new byte[bufferJsonSize];
        bisInStoreJson.read(bufferJson, 0, bufferJsonSize);
        bisInStoreJson.close();

         // Add to downloaded files list
         downloadedVideoFilesList.put(videoName, videoName);

        VideoRawData videoMetadata = new VideoRawData(bufferVideoStream, bufferSize, videoName, bufferJson);

        videoCacheUtil.addVideoMetadata(videoName, videoMetadata);
    }

    public boolean isVideoFileDownloaded(String videoName) {
        File fileVideo = new File(defaultVideoFilePath,videoName);
        return fileVideo.exists();
    }

    public String getFirstVideoFileName() {
        if (downloadedVideoFilesList.size()>0) {
            return downloadedVideoFilesList.firstKey();
        } else {
            return null;
        }
    }
}

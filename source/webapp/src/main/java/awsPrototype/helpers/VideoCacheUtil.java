package awsPrototype.helpers;

import java.io.FileNotFoundException;
import java.io.IOException;

import awsPrototype.metadatas.Constants;
import awsPrototype.metadatas.VideoRawData;

public class VideoCacheUtil {

    private static VideoCacheUtil instance;
    
    private final ConcurrentLRUCache<String,VideoRawData> videoCache;

    private VideoCacheUtil() {
        this.videoCache = new ConcurrentLRUCache<String,VideoRawData>(Constants.MAX_VIDEO_CACHE_ENTRIES);
    }
    
    public static VideoCacheUtil getInstance() {
        if (instance == null) {
            synchronized (VideoCacheUtil.class) {
                if (instance == null) {
                    instance = new VideoCacheUtil();
                }
            }
        }
        return instance;
    }

    public VideoRawData getVideoRawData(String videoName) throws FileNotFoundException, IOException {
        if (!videoCache.containsKey(videoName)) {
            VideoFileUtil.getInstance().loadVideoFromFile(videoName);
        }
        return videoCache.get(videoName);
    }

    public void addVideoMetadata(String videoName, VideoRawData videoMetadata) {
        videoCache.put(videoName, videoMetadata);
    }

    public void removeVideoMetadata(String videoName) {
        videoCache.remove(videoName);
    }

    public boolean containsVideoMetadata(String videoName) {
        return videoCache.containsKey(videoName);
    }

    public int getVideoCacheSize() {
        return videoCache.size();
    }

    public void clearVideoCache() {
        videoCache.clear();
    }


}



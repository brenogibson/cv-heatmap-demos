package awsPrototype.metadatas;

public class VideoRawData {

    byte[] bufferVideoStream;
    int bufferSize;
    String videoName;
    byte[] bufferJson;

    public VideoRawData(byte[] bufferVideoStream, int bufferSize, String videoName, byte[] bufferJson) {
        this.bufferVideoStream = bufferVideoStream;
        this.bufferSize = bufferSize;
        this.videoName = videoName;
        this.bufferJson = bufferJson;
    }

    public byte[] getBufferVideoStream() {
        return bufferVideoStream;
    }

    public int getBufferSize() {
        return bufferSize;
    }
    
    public String getVideoName() {
        return videoName;
    }

    public byte[] getBufferJson() {
        return bufferJson;
    }
    
}

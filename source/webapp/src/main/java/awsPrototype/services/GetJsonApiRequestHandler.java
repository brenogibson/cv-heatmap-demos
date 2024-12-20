package awsPrototype.services;

import java.io.FileNotFoundException;
import java.io.IOException;

import org.rapidoid.http.Req;
import org.rapidoid.http.ReqRespHandler;
import org.rapidoid.http.Resp;

import awsPrototype.helpers.VideoCacheUtil;
import awsPrototype.metadatas.VideoRawData;

public class GetJsonApiRequestHandler implements ReqRespHandler {

    private final VideoCacheUtil videoCacheUtil;

    public GetJsonApiRequestHandler() {
        this.videoCacheUtil = VideoCacheUtil.getInstance();
    }

    @Override
    public Object execute(Req req, Resp resp) throws FileNotFoundException, IOException {
        String videoName = req.param("videoName");
        VideoRawData videoRawData = videoCacheUtil.getVideoRawData(videoName);
        byte[] bufferJson = videoRawData.getBufferJson();
        return new String(bufferJson);
    }

}

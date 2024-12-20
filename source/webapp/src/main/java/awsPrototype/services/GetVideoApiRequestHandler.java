package awsPrototype.services;

import java.util.Arrays;

import org.rapidoid.http.Req;
import org.rapidoid.http.ReqRespHandler;
import org.rapidoid.http.Resp;

import awsPrototype.helpers.VideoCacheUtil;
import awsPrototype.metadatas.Constants;
import awsPrototype.metadatas.VideoRawData;

public class GetVideoApiRequestHandler implements ReqRespHandler {

    private final VideoCacheUtil videoCacheUtil;

    public GetVideoApiRequestHandler() {
        this.videoCacheUtil = VideoCacheUtil.getInstance();
    }

    /*
    *   This will create a range download for the video and let the browser controls the skiping of video frames
    *   Info in the page https://developer.mozilla.org/en-US/docs/Web/HTTP/Range_requests
    */
    @Override
    public Object execute(Req req, Resp resp) throws Exception {
            String videoName = req.param("videoName");
            VideoRawData videoRawData = videoCacheUtil.getVideoRawData(videoName);
            byte[] bufferVideoStream = videoRawData.getBufferVideoStream();
            int rangeStart = 0;
            int length = Constants.BLOCK_SIZE;
            
            int rangeEnd = rangeStart + length;
            //Http header Range: bytes=0-1023
            String range = req.header("Range",null);
            int videoBufferSize = videoRawData.getBufferSize();
            if (range==null||"".equals(range)) {
                range = "bytes=0-" + String.valueOf(videoBufferSize);
            }
            String[] rangeParts = range.split("=");
            String[] rangeStartEndValues = rangeParts[1].split("-");
            rangeStart = Integer.parseInt(rangeStartEndValues[0]);
            if (rangeStartEndValues.length>1&&!"".equals(rangeStartEndValues[1])) {
                rangeEnd = Integer.parseInt(rangeStartEndValues[1]);
            } else {
                rangeEnd = rangeStart + length;
            }

            if (rangeEnd>videoBufferSize)
                rangeEnd = videoBufferSize;
            
            length = rangeEnd - rangeStart;
            byte[] buffer;
            if (length==videoBufferSize)
                buffer = bufferVideoStream;
            else {
                buffer = Arrays.copyOfRange(bufferVideoStream, rangeStart, rangeEnd);
            }
            resp.header("Accept-Ranges","bytes");
            resp.header("Content-Length", String.valueOf(length));
            resp.header("Content-Type", "video/mp4");                
            resp.header("Content-Range", "bytes " + rangeStart + "-"+ (rangeEnd-1) + "/" + videoBufferSize);
            if (rangeStart==0&&rangeEnd==videoBufferSize)
                resp.code(200);//complete content    
            else
                resp.code(206);//partial content
            resp.body(buffer);
            resp.done();
            return resp;

    }

}

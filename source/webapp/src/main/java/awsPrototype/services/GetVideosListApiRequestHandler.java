package awsPrototype.services;

import org.rapidoid.http.Req;
import org.rapidoid.http.ReqRespHandler;
import org.rapidoid.http.Resp;

import awsPrototype.helpers.S3Util;
import awsPrototype.helpers.VideoFileUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GetVideosListApiRequestHandler implements ReqRespHandler {

    @Override
    public Object execute(Req req, Resp resp) {
        Map<String,Object> response = new HashMap<>(1);
        response.put("videos", VideoFileUtil.getInstance().listVideoFileNames());
        response.put("isConnectionOk", S3Util.getInstance().isConnectionWithAWSIsOK());
        return response;
    }

}

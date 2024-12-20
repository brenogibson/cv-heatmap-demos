package awsPrototype.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.rapidoid.http.Req;
import org.rapidoid.http.ReqRespHandler;
import org.rapidoid.http.Resp;

import awsPrototype.helpers.VideoFileUtil;

public class GetDefaultVideoNameApiRequestHandler implements ReqRespHandler {

    @Override
    public Object execute(Req req, Resp resp) {
        Map<String,String> response = new HashMap<>(1);
        response.put("defaultVideoName", VideoFileUtil.getInstance().getFirstVideoFileName());
        return response;
    }

}

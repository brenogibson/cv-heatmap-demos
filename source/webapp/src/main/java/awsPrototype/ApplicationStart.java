package awsPrototype;

import org.rapidoid.setup.App;
import org.rapidoid.setup.On;

import awsPrototype.helpers.S3Util;
import awsPrototype.helpers.SqsUtil;
import awsPrototype.helpers.VideoFileUtil;
import awsPrototype.services.GetDefaultVideoNameApiRequestHandler;
import awsPrototype.services.GetJsonApiRequestHandler;
import awsPrototype.services.GetVideosListApiRequestHandler;
import awsPrototype.services.GetVideoApiRequestHandler;

public class ApplicationStart {

    public static void main(String[] args) {
        
        /*
        /*
         *   This will boot the Default Rapidoid Http Server, and by default it will find the static files to serve from the resources/static folder
         */
        App.bootstrap(args);
 
        On.get("/get-video.mp4").plain(new GetVideoApiRequestHandler());
        On.get("/get-json.json").plain(new GetJsonApiRequestHandler());
        On.get("/get-videos-list.json").json(new GetVideosListApiRequestHandler());
        On.get("/get-default-video-name.json").json(new GetDefaultVideoNameApiRequestHandler());

        VideoFileUtil.getInstance().preInitializeDownloadedVideoFilesList();
        SqsUtil.getInstance().startReceivingUpdates();
        S3Util.getInstance().startAutoSync();
    }

}

package awsPrototype.helpers;

import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import awsPrototype.metadatas.Constants;
import software.amazon.awssdk.auth.credentials.ProcessCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.SqsException;

public class SqsUtil {

    private static SqsUtil instance;

    private final SqsAsyncClient sqsClient;

    private final Timer timer;

    private SqsUtil() {
        sqsClient = SqsAsyncClient.builder()
            .credentialsProvider(ProfileCredentialsProvider.builder().build())
            .region(Constants.DEFAULT_SQS_REGION)
            .build();
        timer = new Timer();
    }

    public static SqsUtil getInstance() {
        if (instance == null) 
            synchronized(SqsUtil.class) {
                if (instance == null) {
                    instance = new SqsUtil();
                }
            }
        return instance;
    }

    public void startReceivingUpdates() {

        if (Constants.DEFAULT_SQS_CONTINUOUS_RECURRENCY * Constants.DEFAULT_SQS_DELAY_START > 0 ) {
            TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    receiveUpdates();
                }
            };
             timer.schedule(task, 1000 * Constants.DEFAULT_SQS_DELAY_START, 1000 * Constants.DEFAULT_SQS_CONTINUOUS_RECURRENCY); 
            }
    }

    private void receiveUpdates() {
        try {

            ReceiveMessageRequest receiveMessageRequest = ReceiveMessageRequest.builder()
                .queueUrl(Constants.DEFAULT_SQS_QUEUE_URL)
                .waitTimeSeconds(Constants.DEFAULT_SQS_WAIT_TIME)
                .maxNumberOfMessages(5)
                .build();
            sqsClient.receiveMessage(receiveMessageRequest).thenAccept(response->{

                List<Message> messages = response.messages();
                //TODO: not using the messages yet, but lets re-sync from S3...

                S3Util.getInstance().syncS3Files();

            });
        } catch (SqsException e) {
            e.printStackTrace();
        }
    }
}

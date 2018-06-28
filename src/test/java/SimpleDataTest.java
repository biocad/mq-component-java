import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import component.Config;
import component.MoniqueComponent;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.junit.Assert;
import org.junit.Test;
import protocol.MoniqueMessage;
import protocol.MoniqueTaggedMessage;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static component.Constant.DATA;
import static component.Constant.JSON_TYPE;
import static component.Constant.NEVER_EXPIRES;
import static component.Converter.objectToByteArray;

public class SimpleDataTest {

    private static final String SPEC = "simple_data";

    @Test
    public void testSimpleDataInOut() throws InterruptedException {
        SimpleDataComponentExample dataProcessor = new SimpleDataComponentExample();
        dataProcessor.start();
        Thread.sleep(5000);

        Assert.assertNotNull(dataProcessor.outgoingMessage);
        Assert.assertNotNull(dataProcessor.incomingMessage);
        Assert.assertEquals(dataProcessor.outgoingMessage, dataProcessor.incomingMessage.getMoniqueMessage());
    }

    private class SimpleDataComponentExample extends MoniqueComponent {

        MoniqueMessage outgoingMessage;
        MoniqueTaggedMessage incomingMessage;

        @Override
        protected List<String> availableIncomingSpecifications() {
            return Collections.singletonList(SPEC);
        }

        @Override
        protected void run() {

            new Thread(() -> {
                try {
                    outgoingMessage = new MoniqueMessage("", UUID.randomUUID().toString(), NEVER_EXPIRES, SPEC,
                            JSON_TYPE, DATA, objectToByteArray(new SimpleData(1, "testData")));
                } catch (JsonProcessingException e) {
                    e.printStackTrace();
                }
                sendMoniqueMessage(outgoingMessage);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();

            new Thread(() -> {
                try {
                    incomingMessage = receiveMessage();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }

        @Override
        protected Config createConfiguration() {
            try {
                return new ObjectMapper().readValue(getClass().getResourceAsStream("config.json"), Config.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }


    @AllArgsConstructor
    @Getter
    private class SimpleData {
        private Integer id;
        private String name;
    }

}

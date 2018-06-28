package component;

import com.fasterxml.jackson.annotation.JsonProperty;
import exception.InvalidValueException;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.zeromq.ZContext;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;
import protocol.MoniqueError;
import protocol.MoniqueMessage;
import protocol.MoniqueTaggedMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.LinkedBlockingQueue;

import static component.Constant.*;
import static component.Converter.*;
import static component.TagUtils.*;
import static protocol.ErrorCodes.TECHNICAL_ERROR;

/**
 * MoniQue component is responsible for interaction with core MoniQue functionality
 * It opens ZeroMQ channels for communication purpose
 * Component implementations define its own custom logic for message processing
 * e.g. communication with other services and systems
 *
 * @author Pavel Didkovskii
 */
public abstract class MoniqueComponent {

    private static final Log log = LogFactory.getLog(MoniqueComponent.class);

    private static final LinkedBlockingQueue<IdentifiedMoniqueError> errorQueue = new LinkedBlockingQueue<>();

    private static final LinkedBlockingQueue<MoniqueTaggedMessage> incoming = new LinkedBlockingQueue<>();

    private static final LinkedBlockingQueue<MoniqueMessage> outgoing = new LinkedBlockingQueue<>();

    private static final List<Thread> communicationThreads = new ArrayList<>();

    private final CommunicationManager communicationManager = new CommunicationManager();

    private static Config config;

    private static volatile Boolean isCommunicationAlive = false;

    private static Boolean started = false;

    private static Boolean restartCommunication = false;


    /**
     * MoniQue Component subclasses have to implement this method with all processing logic related to it
     *
     * @return void
     */
    protected abstract void run();

    /**
     * MoniQue Component subclasses have to implement this method to filter incoming messages
     *
     * @return all available specifications
     */
    protected abstract List<String> availableIncomingSpecifications();

    /**
     * MoniQue Component subclasses have to implement this method to set up component config
     *
     * @return component configuration
     */
    protected abstract Config createConfiguration();

    /**
     * Focal point for all MoniqueComponent subclasses
     * <p>
     * Initialize and start MoniQue threads and message processing logic
     * Start only once for Component instance due to synchronization and started flag
     * All methods except setConfig() starts asynchronously
     *
     * @throws InterruptedException
     */
    public synchronized void start() throws InterruptedException {
        if (!started) {
            setConfig();
            initTechThread();
            initErrorThread();
            initCommunicationThread();
            initMonitoring();
            runAsync();
            listenCommunicationThreadForRestart();
            started = true;
        }
    }

    /**
     * Push message to outgoing queue
     */
    protected static void sendMoniqueMessage(MoniqueMessage message) {
        outgoing.add(message);
    }

    /**
     * Push message to error queue
     */
    protected static void sendErrorMessage(MoniqueError message, String taskId) {
        errorQueue.add(new IdentifiedMoniqueError(taskId, message));
    }

    /**
     * Receive and remove message from incoming queue and block it if empty
     *
     * @throws InterruptedException
     */
    protected static MoniqueTaggedMessage receiveMessage() throws InterruptedException {
        return incoming.take();
    }

    private void setConfig() {
        config = createConfiguration();
    }

    private void runAsync() {
        new Thread(this::run).start();
    }


    private void initTechThread() {
        TechnicalManager.getInstance().initTechnicalThread();
    }

    private void initErrorThread() {
        ErrorManager.getInstance().initErrorThread();
    }

    private void initCommunicationThread() {
        communicationManager.initCommunicationThreads(availableIncomingSpecifications());
    }

    private void initMonitoring() {
        MonitoringManager.getInstance().initMonitoringThread();
    }

    private void listenCommunicationThreadForRestart() {
        new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                if (restartCommunication) {
                    restartCommunication = false;
                    communicationThreads.forEach(Thread::interrupt);
                    isCommunicationAlive = false;
                    communicationThreads.clear();
                }
                communicationManager.initCommunicationThreads(availableIncomingSpecifications());
            }
        });
    }

    private class CommunicationManager {

        /**
         * Creates new ZMQ context to receive and send messages from/to MoniQue scheduler as soon as they appears
         *
         * @param specifications - list of available specifications which will be accepted by listener
         */
        private void initCommunicationThreads(List<String> specifications) {

            communicationThreads.add(new Thread(() -> {
                try (ZContext context = new ZContext()) {
                    ZMQ.Socket messageSub = context.createSocket(ZMQ.SUB);
                    messageSub.subscribe("");
                    messageSub.connect("tcp://" + config.getDeploy().getMonique().getOut().getHost() +
                            ":" + config.getDeploy().getMonique().getOut().getComport());
                    while (!Thread.currentThread().isInterrupted()) {
                        ZMsg zMsg = ZMsg.recvMsg(messageSub);
                        ZFrame tagFrame = zMsg.getFirst();
                        try {
                            String tag = objectFromMessagePack(tagFrame.getData(), String.class);
                            if (specifications.contains(getTagPart(tag, TagPart.SPEC))) {
                                ZFrame messageFrame = zMsg.getLast();
                                incoming.add(new MoniqueTaggedMessage(
                                        tag, objectFromMessagePack(messageFrame.getData(), MoniqueMessage.class)));
                            }
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }));

            communicationThreads.add(new Thread(() -> {
                try (ZContext context = new ZContext()) {
                    ZMQ.Socket messageSender = context.createSocket(ZMQ.PUSH);
                    messageSender.connect("tcp://" + config.getDeploy().getMonique().getIn().getHost() +
                            ":" + config.getDeploy().getMonique().getIn().getComport());
                    processIncomingMessage(messageSender);
                }
            }));

            if (config.getDeploy().getMonique().getController() != null) {
                communicationThreads.add(new Thread(() -> {
                    try (ZContext context = new ZContext()) {
                        ZMQ.Socket messageSender = context.createSocket(ZMQ.PULL);
                        messageSender.connect("tcp://" + config.getDeploy().getMonique().getController().getHost() +
                                ":" + config.getParam().getPort());
                        processIncomingMessage(messageSender);
                    }
                }));
            }

            communicationThreads.forEach(Thread::start);
            log.info("Communcation threads successfully started");
            isCommunicationAlive = true;
        }
    }

    private void processIncomingMessage(ZMQ.Socket socket) {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                MoniqueMessage out = outgoing.take();
                socket.sendMore(objectToMessagePack(createMessageTag(out)));
                socket.send(objectToMessagePack(out));
            } catch (InterruptedException e) {
                log.info("Communication thread was interrupted");
                break;
            } catch (Exception e) {
                log.error("An error occurred in Communication thread: " + e.getCause());
            }
        }
    }

    private static class ErrorManager {

        private static class ErrorManagerHolder {
            static final ErrorManager instance = new ErrorManager();
        }

        static ErrorManager getInstance() {
            return ErrorManagerHolder.instance;
        }

        /**
         * Creates new ZMQ context and send error messages to MoniQue scheduler as soon as they appears
         */
        private void initErrorThread() {
            new Thread(() -> {
                try (ZContext context = new ZContext()) {
                    ZMQ.Socket errSender = context.createSocket(ZMQ.PUSH);
                    errSender.connect("tcp://" + config.getDeploy().getMonique().getIn().getHost() +
                            ":" + config.getDeploy().getMonique().getIn().getComport());
                    while (!Thread.currentThread().isInterrupted()) {
                        try {
                            IdentifiedMoniqueError error = errorQueue.take();
                            String pid = error.getTaskId() != null ? error.getTaskId() : "";
                            MoniqueMessage message = new MoniqueMessage(pid, UUID.randomUUID().toString(),
                                    NEVER_EXPIRES, ERROR, JSON_TYPE, ERROR, objectToByteArray(error));
                            errSender.sendMore(objectToMessagePack(createMessageTag(message)));
                            errSender.send(objectToMessagePack(message));
                        } catch (InterruptedException e) {
                            log.info("Error thread was interrupted");
                            break;
                        } catch (Exception e) {
                            log.error("An error occurred in Error thread: " + e.getCause());
                        }
                    }
                }
            }).start();
            log.info("Error thread successfully started");
        }
    }

    private static class TechnicalManager {

        private static class TechnicalManagerHolder {
            static final TechnicalManager instance = new TechnicalManager();
        }

        static TechnicalManager getInstance() {
            return TechnicalManagerHolder.instance;
        }

        /**
         * Creates new ZMQ context and receive technical messages from MoniQue scheduler as soon as they appears
         */
        private void initTechnicalThread() {
            new Thread(() -> {
                try (ZContext context = new ZContext()) {
                    ZMQ.Socket techSub = context.createSocket(ZMQ.SUB);
                    techSub.subscribe("");
                    techSub.connect("tcp://" + config.getDeploy().getMonique().getOut().getHost() +
                            ":" + config.getDeploy().getMonique().getOut().getComport());
                    while (!Thread.currentThread().isInterrupted()) {
                        try {
                            ZMsg zMsg = ZMsg.recvMsg(techSub);
                            ZFrame tagFrame = zMsg.getFirst();
                            String tag = objectFromMessagePack(tagFrame.getData(), String.class);
                            if (CONFIG.equals(TagUtils.getTagPart(tag, TagUtils.TagPart.TYPE)) &&
                                    KILL.equals(TagUtils.getTagPart(tag, TagUtils.TagPart.SPEC))) {
                                restartCommunication = true;
                            }
                        } catch (IOException | InvalidValueException e) {
                            log.error("An error occurred while receiving technical message from MoniQue: " + e.getCause());
                            errorQueue.add(new IdentifiedMoniqueError(
                                    new MoniqueError(TECHNICAL_ERROR.getCode(), e.getMessage())));
                        }
                    }
                }
            }).start();
            log.info("Technical thread successfully started");
        }
    }

    private static class MonitoringManager {

        static class MonitoringManagerHolder {
            static final MonitoringManager instance = new MonitoringManager();
        }

        static MonitoringManager getInstance() {
            return MonitoringManagerHolder.instance;
        }

        /**
         * Creates new ZMQ context and send monitoring messages to MoniQue scheduler by cron
         */
        private void initMonitoringThread() {
            new Thread(() -> {
                try (ZContext context = new ZContext()) {
                    ZMQ.Socket monitoringSender = context.createSocket(ZMQ.PUSH);
                    monitoringSender.connect("tcp://" + config.getDeploy().getMonique().getIn().getHost() +
                            ":" + config.getDeploy().getMonique().getIn().getComport());
                    while (!Thread.currentThread().isInterrupted()) {
                        try {
                            MoniqueMonitoring monitoring = new MoniqueMonitoring(config.getParam().getName(),
                                    isCommunicationAlive, "");
                            MoniqueMessage message = new MoniqueMessage("", UUID.randomUUID().toString(),
                                    NEVER_EXPIRES, MONITORING, JSON_TYPE, DATA, Converter.objectToByteArray(monitoring));
                            monitoringSender.sendMore(objectToMessagePack(createMessageTag(message)));
                            monitoringSender.send(objectToMessagePack(message));
                            Thread.sleep(config.getParam().getFrequency());
                        } catch (InterruptedException e) {
                            break;
                        } catch (Exception e) {
                            log.error("An error occurred while sending monitor message to MoniQue: " + e.getCause());
                            errorQueue.add(new IdentifiedMoniqueError
                                    (new MoniqueError(TECHNICAL_ERROR.getCode(), e.getMessage())));
                        }
                    }
                }
            }).start();
            log.info("Monitoring thread successfully started");
        }
    }

    @Getter
    @AllArgsConstructor
    private static class IdentifiedMoniqueError {
        private String taskId;
        private MoniqueError moniqueError;

        IdentifiedMoniqueError(MoniqueError moniqueError) {
            this.taskId = "";
            this.moniqueError = moniqueError;
        }
    }

    @NoArgsConstructor
    @Getter
    @ToString
    private static class MoniqueMonitoring {

        private String name;

        @JsonProperty("sync_time")
        private Integer syncTime;

        @JsonProperty("is_alive")
        private Boolean isAlive;

        private String message;

        public MoniqueMonitoring(String name, Boolean isAlive, String message) {
            this.name = name;
            this.syncTime = (int) (System.currentTimeMillis() % Integer.MAX_VALUE);
            this.isAlive = isAlive;
            this.message = message;
        }
    }
}


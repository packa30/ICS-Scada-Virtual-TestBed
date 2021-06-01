package xyz.scada.testbed.node;

import org.openmuc.j60870.*;
import xyz.scada.testbed.node.dbwork.IOA;
import xyz.scada.testbed.node.dbwork.dbWork;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HMIConnectInterrogate extends Thread {

    private enum  StoredInfo{
        ASDU,
        COT,
        TID,
        IOA

    }

    private static Logger LOGGER = null;

    static {
        System.setProperty("java.util.logging.SimpleFormatter.format", "[%1$tF %1$tT] [%4$-7s] %5$s %n");
        LOGGER = Logger.getLogger(HMIConnectInterrogate.class.getName());
    }

    private String host;
    private int port;
    private int commonAddress;
    private int time_sleep;

    private volatile Connection clientConnection;
    private BufferedReader is;

    HMIConnectInterrogate(String host, int port, int commonAddress, int time_sleep) {
        this.host = host;
        this.port = port;
        this.commonAddress = commonAddress;
        this.time_sleep = time_sleep;
    }

    private class ClientEventListener implements ConnectionEventListener {
        private boolean initialized = false;
//        private int type = 0;
//        private ArrayList<Integer> knownASDU = new ArrayList<Integer>();
//        private ArrayList<CauseOfTransmission> knownCauses = new ArrayList<CauseOfTransmission>();
//        private ArrayList<TypeId> knownTypesASDU_requests = new ArrayList<TypeId>();
//        private ArrayList<Integer[]> knownIOA = new ArrayList<Integer[]>();

        private ArrayList<IOA> IOAs = new ArrayList<IOA>();
        Map<Integer, IOA> IOAs_mapped = new HashMap<Integer, IOA>();
        private dbWork dbdata = new dbWork(this.IOAs, clientConnection);

//        private <Void> void addToKnown_add(Void object, StoredInfo info){
//            switch (info){
//                case ASDU:
//                    this.knownASDU.add((Integer) object);
//                    break;
//                case COT:
//                    this.knownCauses.add((CauseOfTransmission) object);
//                    break;
//                case TID:
//                    this.knownTypesASDU_requests.add((TypeId) object);
//                    break;
//                case IOA:
//                    this.knownIOA.add((Integer[]) object);
//                    break;
//                default:
//                    System.out.println("ERROR");
//                    break;
//            }
//        }

//        private  <Void> void addToKnown(Void[] objects, StoredInfo info){
//            for (Void object : objects) {
//                addToKnown_add(object, info);
//            }
//        }
//
//        private void addAsduInfo(ASdu aSdu){
//            addToKnown_add(aSdu.getCommonAddress(), StoredInfo.ASDU);
//            addToKnown_add(aSdu.getCauseOfTransmission(), StoredInfo.COT);
//            addToKnown_add(aSdu.getTypeIdentification(), StoredInfo.TID);
//            InformationObject[] objects = aSdu.getInformationObjects();
//            ArrayList<Integer> local_IOA = new ArrayList<Integer>();
//            ArrayList<Integer[]> loc = new ArrayList<Integer[]>();
//            for (InformationObject object : objects){
//                local_IOA.add(object.getInformationObjectAddress());
//            }
//            addToKnown_add(local_IOA.toArray(new Integer[local_IOA.size()]), StoredInfo.IOA);
//        }

        @Override
        public void newASdu(ASdu aSdu) {
            LOGGER.log(Level.INFO, "[TID{0}] {1}:{2} ASDU: {3}|{4}\n\t{5}", new Object[]{Thread.currentThread().getId(), host, port, aSdu.getTypeIdentification(), aSdu.getCauseOfTransmission(), aSdu});

            try {
                CauseOfTransmission[] unknownCauses = { CauseOfTransmission.UNKNOWN_COMMON_ADDRESS_OF_ASDU, CauseOfTransmission.UNKNOWN_CAUSE_OF_TRANSMISSION,
                        CauseOfTransmission.UNKNOWN_TYPE_ID, CauseOfTransmission.UNKNOWN_INFORMATION_OBJECT_ADDRESS};

                if(clientConnection.contains(unknownCauses, aSdu.getCauseOfTransmission())){
                    // return false without unknown replay, bcs unknown reply already obtained // maybe todo
                }else{
                    if(aSdu.getCommonAddress() != commonAddress){
                        clientConnection.sendUnknown(aSdu, Connection.UnknownInfo.ASDU);
                    }else if(!this.initialized){
                        if (aSdu.getCauseOfTransmission().equals(CauseOfTransmission.ACTIVATION_CON) ||
                            aSdu.getCauseOfTransmission().equals(CauseOfTransmission.ACTIVATION_TERMINATION) ||
                            aSdu.getCauseOfTransmission().equals(CauseOfTransmission.INTERROGATED_BY_STATION)){
                            this.dbdata.addIoAsHmi(aSdu);
                        }
                        if (aSdu.getCauseOfTransmission().equals(CauseOfTransmission.ACTIVATION_TERMINATION)){
                            this.initialized = true;
                        }
                    }else{
                        this.dbdata.checkASduHmi(aSdu);
                    }
                }


//                if(this.initialized){


//                    this.IOAs.add(new IOA(, IOA.StoredInfo.interrogation, aSdu.getCommonAddress()));
//                    Integer[][] knownIOA = this.knownIOA.toArray(new Integer[this.knownIOA.size()][]);
//                    if(!clientConnection.checkCorrect(aSdu, this.knownASDU.toArray(new Integer[this.knownASDU.size()]),
//                                                            knownIOA,
//                                                            this.knownCauses.toArray(new CauseOfTransmission[this.knownCauses.size()]),
//                                                            this.knownTypesASDU_requests.toArray(new TypeId[this.knownTypesASDU_requests.size()]))){
//                        System.out.println("ERROR");
//                    }
//                }else{
//                    if (aSdu.getCauseOfTransmission().equals(CauseOfTransmission.ACTIVATION_CON) ||
//                        aSdu.getCauseOfTransmission().equals(CauseOfTransmission.ACTIVATION_TERMINATION) ||
//                        aSdu.getCauseOfTransmission().equals(CauseOfTransmission.INTERROGATED_BY_STATION)){
//                        addAsduInfo(aSdu);
//                    }
//                    if (aSdu.getCauseOfTransmission().equals(CauseOfTransmission.ACTIVATION_TERMINATION)){
//                        this.initialized = true;
//                    }
//                }
            }catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Unable to connect to remote host: {0}:{1}", new Object[]{host, port});
                return;
            }
//            for(InformationObject ob : aSdu.getInformationObjects()){
//                for(InformationElement[] ie : ob.getInformationElements()){
//                    LOGGER.log(Level.INFO, ie.toString());
//                }
//            }
        }

        @Override
        public void connectionClosed(IOException e) {
            String reason = "Unknown";
            if (!e.getMessage().isEmpty()) {
                reason = e.getMessage();
            }

            LOGGER.log(Level.INFO, " Received connection closed signal. Reason: {0}", reason);

        }
    }

    public void run() {
//            for (int i = 1000; i > 1; i += 1000) {

        LOGGER.log(Level.INFO, "[TID{0}] Connecting to: {1}:{2}", new Object[]{Thread.currentThread().getId(), host, port});
        System.out.println("Connecting to: " + host + ":" + port + " (IEC104)");

        InetAddress address;
        try {
            address = InetAddress.getByName(host);
        } catch (UnknownHostException e) {
            LOGGER.log(Level.WARNING, "Unknown host: {0}", host);
            return;
        }

        ClientConnectionBuilder clientConnectionBuilder = new ClientConnectionBuilder(address).setPort(port).setMaxUnconfirmedIPdusReceived(2).setMaxIdleTime(20000);

        try {
            clientConnection = clientConnectionBuilder.connect();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Unable to connect to remote host: {0}:{1}", new Object[]{host, port});
//            LOGGER.log(Level.SEVERE, e.toString(), e);
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                clientConnection.close();
            }
        });

        try {
            ClientEventListener newListener;
            try {
                newListener = new ClientEventListener();
                clientConnection.startDataTransfer(newListener, 5000);

            } catch (TimeoutException e2) {
                throw new IOException(" Starting data transfer timed out.");
            }

            LOGGER.log(Level.INFO, "[TID{0}] Successfully connected", Thread.currentThread().getId());

//            if(this.host.equals("192.168.1.11")){
//                ScadaToSubstation_Normal(newListener);
//            }else if(this.host.equals("192.168.1.12")){
//                iec104_first(newListener);
//            }else{
//                testing(newListener);
////                experiment(newListener);
//            }
            testing(newListener);

        } catch (IOException e) {
            LOGGER.log(Level.INFO, "Connection closed for the following reason: {0}", e.getMessage());
            return;
        } finally {
            clientConnection.close();
        }

    }

    public void testing(ClientEventListener Listener){

        try {
            clientConnection.interrogation(commonAddress, CauseOfTransmission.ACTIVATION, new IeQualifierOfInterrogation(20));//init
            Thread.sleep(time_sleep);

            clientConnection.singleCommand(commonAddress, CauseOfTransmission.ACTIVATION, 2, new IeSingleCommand(true, 0, false));
            Thread.sleep(time_sleep);

            clientConnection.doubleCommand(commonAddress, CauseOfTransmission.ACTIVATION, 1, new IeDoubleCommand(IeDoubleCommand.DoubleCommandState.OFF, 0, false));
            Thread.sleep(time_sleep);

            clientConnection.regulatingStepCommand(commonAddress, CauseOfTransmission.ACTIVATION, 1, new IeRegulatingStepCommand(IeRegulatingStepCommand.StepCommandState.NEXT_STEP_HIGHER, 0, false));
            Thread.sleep(time_sleep);

            clientConnection.bitStringCommand(commonAddress, CauseOfTransmission.ACTIVATION, 3, new IeBinaryStateInformation(1<<25 ));
            Thread.sleep(time_sleep);

            clientConnection.setNormalizedValueCommand(commonAddress, CauseOfTransmission.ACTIVATION, 1, new IeNormalizedValue(1024), new IeQualifierOfSetPointCommand(0, false));
            Thread.sleep(time_sleep);

            clientConnection.setScaledValueCommand(commonAddress, CauseOfTransmission.ACTIVATION, 3, new IeScaledValue(123), new IeQualifierOfSetPointCommand(0, false));
            Thread.sleep(time_sleep);

            clientConnection.setShortFloatCommand(commonAddress, CauseOfTransmission.ACTIVATION, 1, new IeShortFloat((float)3.14), new IeQualifierOfSetPointCommand(0, false));
            Thread.sleep(time_sleep);


            while (true) {

                clientConnection.interrogation(commonAddress, CauseOfTransmission.ACTIVATION, new IeQualifierOfInterrogation(20));//check changes
                Thread.sleep(time_sleep);

            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Unable to connect to remote host: {0}:{1}", new Object[]{host, port});
            return;
        }catch (InterruptedException e) {
            LOGGER.log(Level.INFO, "[TID{0}] Got interrupted!",Thread.currentThread().getId() );
        }
    }

//     public void ScadaToSubstation_Normal(ClientEventListener Listener){
//        int counter = 6; // 6*10minutes before wrong packets are sent
//         try {
//             clientConnection.setOriginatorAddress(2);
//             Listener.addToKnown_add(TypeId.C_DC_NA_1, StoredInfo.TID);
//             Listener.addToKnown_add(11272301, StoredInfo.IOA);
//             boolean DPcommand = false;
//             while (true) {
//                 LOGGER.log(Level.INFO, " Sent Interrogation Command.");
//
//                 clientConnection.interrogation(3, CauseOfTransmission.ACTIVATION, new IeQualifierOfInterrogation(20));
//
//                 /*only every second time is to be called for special command*/
//                if(!DPcommand){
//                    if (counter == 0){
//                        /*unknown ASDU*/
//                        clientConnection.doubleCommand(99, CauseOfTransmission.ACTIVATION,11272301, new IeDoubleCommand(IeDoubleCommand.DoubleCommandState.ON, 0, false));
//                        Thread.sleep(310000);
//                        /*unknown COT*/
//                        clientConnection.doubleCommand(3, CauseOfTransmission.INTERROGATED_BY_STATION,11272301, new IeDoubleCommand(IeDoubleCommand.DoubleCommandState.ON, 0, false));
//                        Thread.sleep(310000);
//                        /*unknown TID*/
//                        clientConnection.resetProcessCommand(3, new IeQualifierOfResetProcessCommand(0));
//                        Thread.sleep(310000);
//                        /*unknown IOA*/
//                        clientConnection.doubleCommand(3, CauseOfTransmission.ACTIVATION,420, new IeDoubleCommand(IeDoubleCommand.DoubleCommandState.ON, 0, false));
//                        Thread.sleep(310000);
//                        counter--;
//                    }else{
//                        DPcommand = true;
//                        Thread.sleep(310000);
//                        clientConnection.doubleCommand(3, CauseOfTransmission.ACTIVATION,11272301, new IeDoubleCommand(IeDoubleCommand.DoubleCommandState.ON, 0, false));
//                        Thread.sleep(20000);
//                        if (counter > 0){
//                            counter--;
//                        }
//                    }
//                    DPcommand = true;
//                    Thread.sleep(310000);
//                    clientConnection.doubleCommand(3, CauseOfTransmission.ACTIVATION,11272301, new IeDoubleCommand(IeDoubleCommand.DoubleCommandState.ON, 0, false));
//                    Thread.sleep(20000);
//                }else{
//                    DPcommand = false;
//                    Thread.sleep(330000);
//                }
////                clientConnection.interrogation(commonAddress, CauseOfTransmission.ACTIVATION, new IeQualifierOfInterrogation(100));
//             }
//         } catch (IOException e) {
//             LOGGER.log(Level.SEVERE, "Unable to connect to remote host: {0}:{1}", new Object[]{host, port});
////            LOGGER.log(Level.SEVERE, e.toString(), e);
//             return;
//         } catch (InterruptedException e) {
//            LOGGER.log(Level.INFO, "[TID{0}] Got interrupted!",Thread.currentThread().getId() );
//         }
//
//     }

//    public void iec104_first(ClientEventListener Listener){
//        Listener.addToKnown_add(CauseOfTransmission.SPONTANEOUS, StoredInfo.COT);
//        TypeId[] self_learn = {TypeId.C_IC_NA_1,TypeId.C_SC_NA_1,TypeId.C_DC_NA_1,TypeId.C_RC_NA_1,
//                               TypeId.C_BO_NA_1,TypeId.C_SE_NA_1,TypeId.C_SE_NB_1,TypeId.C_SE_NC_1};
//        Listener.addToKnown(self_learn,StoredInfo.TID);
//
//        try {
//            clientConnection.interrogation(10, CauseOfTransmission.ACTIVATION, new IeQualifierOfInterrogation(20));//init
//            while (true) {
//                clientConnection.interrogation(10, CauseOfTransmission.ACTIVATION, new IeQualifierOfInterrogation(20));
//                Thread.sleep(time_sleep);
//
//                clientConnection.singleCommand(10, CauseOfTransmission.ACTIVATION, 2, new IeSingleCommand(true, 0, false));
//                Thread.sleep(time_sleep);
//
//                clientConnection.singleCommand(10, CauseOfTransmission.ACTIVATION, 13, new IeSingleCommand(true, 0, false));
//                Thread.sleep(time_sleep);
//
//                clientConnection.doubleCommand(10, CauseOfTransmission.ACTIVATION, 1, new IeDoubleCommand(IeDoubleCommand.DoubleCommandState.OFF, 0, false));
//                Thread.sleep(time_sleep);
//
//                clientConnection.doubleCommand(10, CauseOfTransmission.ACTIVATION, 14, new IeDoubleCommand(IeDoubleCommand.DoubleCommandState.ON, 0, false));
//                Thread.sleep(time_sleep);
//
//                clientConnection.regulatingStepCommand(10, CauseOfTransmission.ACTIVATION, 1, new IeRegulatingStepCommand(IeRegulatingStepCommand.StepCommandState.NEXT_STEP_HIGHER, 0, false));
//                Thread.sleep(time_sleep);
//
//                clientConnection.regulatingStepCommand(10, CauseOfTransmission.ACTIVATION, 12, new IeRegulatingStepCommand(IeRegulatingStepCommand.StepCommandState.NEXT_STEP_LOWER, 0, false));
//                Thread.sleep(time_sleep);
//
//                clientConnection.bitStringCommand(10, CauseOfTransmission.ACTIVATION, 3, new IeBinaryStateInformation(1<<25 ));
//                Thread.sleep(time_sleep);
//
//                clientConnection.bitStringCommand(10, CauseOfTransmission.ACTIVATION, 14, new IeBinaryStateInformation(1<<26));
//                Thread.sleep(time_sleep);
//
//                clientConnection.setNormalizedValueCommand(10, CauseOfTransmission.ACTIVATION, 1, new IeNormalizedValue(1024), new IeQualifierOfSetPointCommand(0, false));
//                Thread.sleep(time_sleep);
//
//                clientConnection.setNormalizedValueCommand(10, CauseOfTransmission.ACTIVATION, 12, new IeNormalizedValue(8192), new IeQualifierOfSetPointCommand(0, false));
//                Thread.sleep(time_sleep);
//
//                clientConnection.setScaledValueCommand(10, CauseOfTransmission.ACTIVATION, 3, new IeScaledValue(123), new IeQualifierOfSetPointCommand(0, false));
//                Thread.sleep(time_sleep);
//
//                clientConnection.setScaledValueCommand(10, CauseOfTransmission.ACTIVATION, 14, new IeScaledValue(456), new IeQualifierOfSetPointCommand(0, false));
//                Thread.sleep(time_sleep);
//
//                clientConnection.setShortFloatCommand(10, CauseOfTransmission.ACTIVATION, 1, new IeShortFloat((float)3.14), new IeQualifierOfSetPointCommand(0, false));
//                Thread.sleep(time_sleep);
//
//                clientConnection.setShortFloatCommand(10, CauseOfTransmission.ACTIVATION, 12, new IeShortFloat((float)9.87), new IeQualifierOfSetPointCommand(0, false));
//                Thread.sleep(time_sleep);
//            }
//        } catch (IOException e) {
//            LOGGER.log(Level.SEVERE, "Unable to connect to remote host: {0}:{1}", new Object[]{host, port});
////            LOGGER.log(Level.SEVERE, e.toString(), e);
//            return;
//        }catch (InterruptedException e) {
//            LOGGER.log(Level.INFO, "[TID{0}] Got interrupted!",Thread.currentThread().getId() );
//        }
//
//    }

//    public void experiment(ClientEventListener Listener){
//        try {
//            TypeId[] toBe_sent_requests = { TypeId.C_SC_NA_1, TypeId.C_DC_NA_1, TypeId.C_RC_NA_1, TypeId.C_BO_NA_1,
//                                            TypeId.C_SE_NA_1, TypeId.C_SE_NB_1, TypeId.C_SE_NC_1};
//
//            clientConnection.interrogation(commonAddress, CauseOfTransmission.ACTIVATION, new IeQualifierOfInterrogation(20));//init
//            Thread.sleep(time_sleep);
//            Listener.addToKnown(toBe_sent_requests, StoredInfo.TID);
//
//
//            while (true) {
//                for ( TypeId request  : toBe_sent_requests) {
//                    System.out.println("request:     " + request);
//                    int counter = 0;
//                    for ( TypeId known_reply : Listener.knownTypesASDU_requests) {
//                        System.out.println(known_reply);
//                        switch (request){
//                            case C_SC_NA_1:
//                                if (known_reply.equals(TypeId.M_SP_TB_1) || known_reply.equals(TypeId.M_SP_NA_1)){
//                                    for (Integer IOA : Listener.knownIOA.get(counter)){
//                                        clientConnection.singleCommand(commonAddress, CauseOfTransmission.ACTIVATION, IOA, new IeSingleCommand(true, 0, false));
//                                    }
//                                }else{}
//                                break;
//                            case C_DC_NA_1:
//                                for (Integer IOA : Listener.knownIOA.get(counter)){
//                                    if (known_reply.equals(TypeId.M_DP_NA_1)) {
//                                        clientConnection.regulatingStepCommand(10, CauseOfTransmission.ACTIVATION, 1, new IeRegulatingStepCommand(IeRegulatingStepCommand.StepCommandState.NEXT_STEP_HIGHER, 0, false));
//                                       }else if(known_reply.equals(TypeId.M_DP_TB_1)){
//                                        clientConnection.doubleCommand(commonAddress, CauseOfTransmission.ACTIVATION, IOA, new IeDoubleCommand(IeDoubleCommand.DoubleCommandState.ON, 0, false));
//                                    }else{}
//                                }
//                                break;
//                            case C_RC_NA_1:
//                                for (Integer IOA : Listener.knownIOA.get(counter)){
//                                    if (known_reply.equals(TypeId.M_ST_NA_1)) {
//                                        clientConnection.regulatingStepCommand(commonAddress, CauseOfTransmission.ACTIVATION, IOA, new IeRegulatingStepCommand(IeRegulatingStepCommand.StepCommandState.NEXT_STEP_LOWER, 0, false));
//                                    }else if(known_reply.equals(TypeId.M_ST_TB_1)){
//                                        clientConnection.regulatingStepCommand(commonAddress, CauseOfTransmission.ACTIVATION, IOA, new IeRegulatingStepCommand(IeRegulatingStepCommand.StepCommandState.NEXT_STEP_LOWER, 0, false));
//                                    }else{}
//                                }
//                                break;
//                            case C_BO_NA_1:
//                                for (Integer IOA : Listener.knownIOA.get(counter)){
//                                    if (known_reply.equals(TypeId.M_BO_NA_1)) {
//                                        clientConnection.bitStringCommand(commonAddress, CauseOfTransmission.ACTIVATION, IOA, new IeBinaryStateInformation(1<<25 ));
//                                    }else if(known_reply.equals(TypeId.M_BO_TB_1)){
//                                        clientConnection.bitStringCommand(commonAddress, CauseOfTransmission.ACTIVATION, IOA, new IeBinaryStateInformation(1<<26));
//                                    }else{}
//                                }
//                                break;
//                            case C_SE_NA_1:
//                                for (Integer IOA : Listener.knownIOA.get(counter)){
//                                    if (known_reply.equals(TypeId.M_ME_NA_1)) {
//                                        clientConnection.setNormalizedValueCommand(commonAddress, CauseOfTransmission.ACTIVATION, IOA, new IeNormalizedValue(1024), new IeQualifierOfSetPointCommand(0, false));
//                                    }else if(known_reply.equals(TypeId.M_ME_TD_1)){
//                                        clientConnection.setNormalizedValueCommand(commonAddress, CauseOfTransmission.ACTIVATION, IOA, new IeNormalizedValue(8192), new IeQualifierOfSetPointCommand(0, false));
//                                    }else{}
//                                }
//                                break;
//                            case C_SE_NB_1:
//                                for (Integer IOA : Listener.knownIOA.get(counter)){
//                                    if (known_reply.equals(TypeId.M_ME_NB_1)) {
//                                        clientConnection.setScaledValueCommand(commonAddress, CauseOfTransmission.ACTIVATION, IOA, new IeScaledValue(123), new IeQualifierOfSetPointCommand(0, false));
//                                    }else if(known_reply.equals(TypeId.M_ME_TE_1)){
//                                        clientConnection.setScaledValueCommand(commonAddress, CauseOfTransmission.ACTIVATION, IOA, new IeScaledValue(456), new IeQualifierOfSetPointCommand(0, false));
//                                    }else{}
//                                }
//                                break;
//                            case C_SE_NC_1:
//                                for (Integer IOA : Listener.knownIOA.get(counter)){
//                                    if (known_reply.equals(TypeId.M_ME_NC_1)) {
//                                        clientConnection.setShortFloatCommand(commonAddress, CauseOfTransmission.ACTIVATION, IOA, new IeShortFloat((float)3.14), new IeQualifierOfSetPointCommand(0, false));
//                                    }else if(known_reply.equals(TypeId.M_ME_TF_1)){
//                                        clientConnection.setShortFloatCommand(commonAddress, CauseOfTransmission.ACTIVATION, IOA, new IeShortFloat((float)9.87), new IeQualifierOfSetPointCommand(0, false));
//                                    }else{}
//                                }
//                                break;
//                            default:
//                                break;
//                        }
//                        counter++;
//                        Thread.sleep(1000);
//                    }
//                    Thread.sleep(10000);
//                }
//            }
//        } catch (IOException e) {
//            LOGGER.log(Level.SEVERE, "Unable to connect to remote host: {0}:{1}", new Object[]{host, port});
////            LOGGER.log(Level.SEVERE, e.toString(), e);
//            return;
//        }catch (InterruptedException e) {
//            LOGGER.log(Level.INFO, "[TID{0}] Got interrupted!",Thread.currentThread().getId() );
//        }
//    }
}

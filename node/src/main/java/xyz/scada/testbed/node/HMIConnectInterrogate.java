package xyz.scada.testbed.node;

import org.openmuc.j60870.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HMIConnectInterrogate extends Thread {

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
        private boolean type;

        public ClientEventListener(boolean type) {
            this.type = type;
        }

        @Override
        public void newASdu(ASdu aSdu) {
            LOGGER.log(Level.INFO, "[TID{0}] {1}:{2} ASDU: {3}|{4}\n\t{5}", new Object[]{Thread.currentThread().getId(), host, port, aSdu.getTypeIdentification(), aSdu.getCauseOfTransmission(), aSdu});

            Integer[] knownASDU = {3};
            CauseOfTransmission[] knownCauses = { CauseOfTransmission.ACTIVATION_CON, CauseOfTransmission.ACTIVATION_TERMINATION,
                                                  CauseOfTransmission.INTERROGATED_BY_STATION};
            TypeId[] knownTypesASDU_requests = {TypeId.C_IC_NA_1,TypeId.C_DC_NA_1, TypeId.M_SP_NA_1, TypeId.M_DP_NA_1, TypeId.M_ST_NA_1};
            Integer[][] knownIOA = {
                    {220008, 270193},
                    {9900001, 9900002, 9900003, 9900004, 9900005, 9900006, 9900007, 9900008, 9900009, 9900010,
                     9900011, 9900012, 9900013, 9900014, 9900015, 9900016, 9900017, 9900018, 9900019, 9900020},
                    {11201050, 11270060, 11270062, 11270069, 11270070, 11270071, 11270073, 11270075, 11270076, 11270083, 11270149, 11270157, 15300029, 15300030, 15301050},
                    {231000, 231100, 231226, 371000, 371106, 371107},
                    {11273701}, //end of normall periodic known
                    {11272301}, // end of special case
                    {0} // 0 is implicit for calls from HMI
            };

            Integer[] knownASDU2 = {10};
            CauseOfTransmission[] knownCauses2 = { CauseOfTransmission.ACTIVATION_CON, CauseOfTransmission.ACTIVATION_TERMINATION,
                    CauseOfTransmission.INTERROGATED_BY_STATION, CauseOfTransmission.SPONTANEOUS, CauseOfTransmission.INITIALIZED};
            TypeId[] knownTypesASDU_requests2 = {TypeId.C_IC_NA_1,TypeId.C_SC_NA_1,TypeId.C_DC_NA_1,TypeId.C_RC_NA_1,
                    TypeId.C_BO_NA_1,TypeId.C_SE_NA_1,TypeId.C_SE_NB_1,TypeId.C_SE_NC_1, TypeId.M_SP_NA_1, TypeId.M_DP_NA_1,
                    TypeId.M_ST_NA_1, TypeId.M_BO_NA_1, TypeId.M_ME_NA_1,
                    TypeId.M_ME_NB_1, TypeId.M_ME_NC_1, TypeId.M_SP_TB_1, TypeId.M_DP_TB_1, TypeId.M_ST_TB_1,
                    TypeId.M_BO_TB_1, TypeId.M_ME_TD_1, TypeId.M_ME_TE_1, TypeId.M_ME_TF_1,
                    TypeId.M_EI_NA_1 };
            Integer[][] knownIOA2 = {
                    {1,2,3,4},
                    {11,12,13,14},
                    {0}
            };


            try {
                if(this.type){
                    if(!clientConnection.checkCorrect(aSdu,knownASDU,knownIOA,knownCauses,knownTypesASDU_requests)){
                       System.out.println("ERROR");
                    }
                }else{
                    if(!clientConnection.checkCorrect(aSdu,knownASDU2,knownIOA2,knownCauses2,knownTypesASDU_requests2)){
                        System.out.println("ERROR");
                    }
                }

            }catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Unable to connect to remote host: {0}:{1}", new Object[]{host, port});
//            LOGGER.log(Level.SEVERE, e.toString(), e);
                return;
            }
            for(InformationObject ob : aSdu.getInformationObjects()){
                for(InformationElement[] ie : ob.getInformationElements()){
                    LOGGER.log(Level.INFO, ie.toString());
                }
            }
        }

        @Override
        public void connectionClosed(IOException e) {
            String reason = "Unknown";
            if (!e.getMessage().isEmpty()) {
                reason = e.getMessage();
            }

            LOGGER.log(Level.INFO, " Received connection closed signal. Reason: {0}", reason);

//            try {
//                is.close();
//            } catch (IOException e1) {
//                LOGGER.log(Level.SEVERE, "Closing connection: {0}", e1);
//            }
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
//                TODO: Reconnection attempts.
//            ClientConnectionBuilder clientConnectionBuilder = new ClientConnectionBuilder(address).setPort(port).setMaxUnconfirmedIPdusReceived(2);
//            int count = 3000;
//            int maxTries = -1;
//            while(true) {
//                try {
//                    clientConnection = clientConnectionBuilder.connect();
//                } catch (IOException e) {
//                    LOGGER.log(Level.SEVERE, "{0} {1}:{2}", new Object[]{e.toString(), host, port});
//                    LOGGER.log(Level.INFO, "Retrying connection in {0}s.\n", ((count/1000)%60));
//                    try {
//                        Thread.sleep(count);
//                    }catch (InterruptedException ez) {
//                        LOGGER.log(Level.INFO, "[TID{0}] Got interrupted!",Thread.currentThread().getId() );
//                    }
//                    count += 3000;
//                    if (count == maxTries) break;
//                }
//            }

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
            try {
                if(this.host.equals("192.168.1.11")){
                    clientConnection.startDataTransfer(new ClientEventListener(true), 5000);
                }else if(this.host.equals("192.168.1.12")){
                    clientConnection.startDataTransfer(new ClientEventListener(false), 5000);
                }{
                    clientConnection.startDataTransfer(new ClientEventListener(true), 5000);
                }

            } catch (TimeoutException e2) {
                throw new IOException(" Starting data transfer timed out.");
            }

            LOGGER.log(Level.INFO, "[TID{0}] Successfully connected", Thread.currentThread().getId());

            if(this.host.equals("192.168.1.11")){
                ScadaToSubstation_Normal();
            }else if(this.host.equals("192.168.1.12")){
                iec104_first();
            }else{
                ScadaToSubstation_Normal();
            }

        } catch (IOException e) {
            LOGGER.log(Level.INFO, "Connection closed for the following reason: {0}", e.getMessage());
            return;
        } finally {
            clientConnection.close();
        }

    }

     public void ScadaToSubstation_Normal(){
         try {
             clientConnection.setOriginatorAddress(2);
             boolean DPcommand = false;
             while (true) {
                 LOGGER.log(Level.INFO, " Sent Interrogation Command.");

                 clientConnection.interrogation(3, CauseOfTransmission.ACTIVATION, new IeQualifierOfInterrogation(20));

                 /*only every second time is to be called for special command*/
                if(!DPcommand){
                    DPcommand = true;
                    Thread.sleep(310000);
                    clientConnection.doubleCommand(3, CauseOfTransmission.ACTIVATION,11272301, new IeDoubleCommand(IeDoubleCommand.DoubleCommandState.ON, 0, false));
                    Thread.sleep(20000);
                }else{
                    DPcommand = false;
                    Thread.sleep(330000);
                }
//                clientConnection.interrogation(commonAddress, CauseOfTransmission.ACTIVATION, new IeQualifierOfInterrogation(100));
             }
         } catch (IOException e) {
             LOGGER.log(Level.SEVERE, "Unable to connect to remote host: {0}:{1}", new Object[]{host, port});
//            LOGGER.log(Level.SEVERE, e.toString(), e);
             return;
         } catch (InterruptedException e) {
            LOGGER.log(Level.INFO, "[TID{0}] Got interrupted!",Thread.currentThread().getId() );
         }

     }

    public void iec104_first(){
        try {
            clientConnection.interrogation(10, CauseOfTransmission.ACTIVATION, new IeQualifierOfInterrogation(20));//init
            while (true) {
                clientConnection.interrogation(10, CauseOfTransmission.ACTIVATION, new IeQualifierOfInterrogation(20));
                Thread.sleep(time_sleep);

                clientConnection.singleCommand(10, CauseOfTransmission.ACTIVATION, 2, new IeSingleCommand(true, 0, false));
                Thread.sleep(time_sleep);

                clientConnection.singleCommand(10, CauseOfTransmission.ACTIVATION, 13, new IeSingleCommand(true, 0, false));
                Thread.sleep(time_sleep);

                clientConnection.doubleCommand(10, CauseOfTransmission.ACTIVATION, 1, new IeDoubleCommand(IeDoubleCommand.DoubleCommandState.OFF, 0, false));
                Thread.sleep(time_sleep);

                clientConnection.doubleCommand(10, CauseOfTransmission.ACTIVATION, 14, new IeDoubleCommand(IeDoubleCommand.DoubleCommandState.ON, 0, false));
                Thread.sleep(time_sleep);

                clientConnection.regulatingStepCommand(10, CauseOfTransmission.ACTIVATION, 1, new IeRegulatingStepCommand(IeRegulatingStepCommand.StepCommandState.NEXT_STEP_HIGHER, 0, false));
                Thread.sleep(time_sleep);

                clientConnection.regulatingStepCommand(10, CauseOfTransmission.ACTIVATION, 12, new IeRegulatingStepCommand(IeRegulatingStepCommand.StepCommandState.NEXT_STEP_LOWER, 0, false));
                Thread.sleep(time_sleep);

                clientConnection.bitStringCommand(10, CauseOfTransmission.ACTIVATION, 3, new IeBinaryStateInformation(1<<25 ));
                Thread.sleep(time_sleep);

                clientConnection.bitStringCommand(10, CauseOfTransmission.ACTIVATION, 14, new IeBinaryStateInformation(1<<26));
                Thread.sleep(time_sleep);

                clientConnection.setNormalizedValueCommand(10, CauseOfTransmission.ACTIVATION, 1, new IeNormalizedValue(1024), new IeQualifierOfSetPointCommand(0, false));
                Thread.sleep(time_sleep);

                clientConnection.setNormalizedValueCommand(10, CauseOfTransmission.ACTIVATION, 12, new IeNormalizedValue(8192), new IeQualifierOfSetPointCommand(0, false));
                Thread.sleep(time_sleep);

                clientConnection.setScaledValueCommand(10, CauseOfTransmission.ACTIVATION, 3, new IeScaledValue(123), new IeQualifierOfSetPointCommand(0, false));
                Thread.sleep(time_sleep);

                clientConnection.setScaledValueCommand(10, CauseOfTransmission.ACTIVATION, 14, new IeScaledValue(456), new IeQualifierOfSetPointCommand(0, false));
                Thread.sleep(time_sleep);

                clientConnection.setShortFloatCommand(10, CauseOfTransmission.ACTIVATION, 1, new IeShortFloat((float)3.14), new IeQualifierOfSetPointCommand(0, false));
                Thread.sleep(time_sleep);

                clientConnection.setShortFloatCommand(10, CauseOfTransmission.ACTIVATION, 12, new IeShortFloat((float)9.87), new IeQualifierOfSetPointCommand(0, false));
                Thread.sleep(time_sleep);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Unable to connect to remote host: {0}:{1}", new Object[]{host, port});
//            LOGGER.log(Level.SEVERE, e.toString(), e);
            return;
        }catch (InterruptedException e) {
            LOGGER.log(Level.INFO, "[TID{0}] Got interrupted!",Thread.currentThread().getId() );
        }

    }
}

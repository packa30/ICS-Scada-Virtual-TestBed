package xyz.scada.testbed.node;

import org.openmuc.j60870.*;
import xyz.scada.testbed.node.dbwork.IOA;
import xyz.scada.testbed.node.dbwork.dbWork;

import java.io.EOFException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RTU {
    
    public RTU() { }

    private static Logger LOGGER = null;

    static {
        System.setProperty("java.util.logging.SimpleFormatter.format", "%n");
        LOGGER = Logger.getLogger(RTU.class.getName());
    }

    public class ServerListener implements ServerEventListener {

        public class ConnectionListener implements ConnectionEventListener {

            private final Connection connection;
            private final int connectionId;
            private boolean initialization = false;
            private Integer ASdu = null;
            private ArrayList<IOA> IOAs = new ArrayList<IOA>();
            Map<Integer, IOA> IOAs_mapped = new HashMap<Integer, IOA>();
            private dbWork dbdata;

            private boolean[] init = {false, false}; // activation / termination false

            public ConnectionListener(Connection connection, int connectionId) {
                this.connection = connection;
                this.connectionId = connectionId;
            }

            public void newASdu(ASdu aSdu) {
                InformationObject info[] = aSdu.getInformationObjects();

//                SubstationToScada_Normal(aSdu);
//                iec104_first(aSdu);
                try {
                    testing(aSdu);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

            public void connectionClosed(IOException e) {
                LOGGER.log(Level.INFO, " Connection ID:{0} closed. {1}", new Object[]{connectionId, e.getMessage()});
            }

            public void testing(ASdu aSdu) throws IOException {
                if (!this.initialization){
                    Integer[][] knownIOA ={
                            {aSdu.getInformationObjects()[0].getInformationObjectAddress()},//global
                            {1,2,3,4},//without time
                            {11,12,13,14}//with time56
                    };
                    int type = 0;
                    for (Integer[] ioaS : knownIOA){
                        for (Integer ioa: ioaS){
                            switch (type){
                                case 0:
                                    this.IOAs.add(new IOA(ioa, IOA.StoredInfo.interrogation, aSdu.getOriginatorAddress()));
                                    break;
                                case 1:
                                    this.IOAs.add(new IOA(ioa, IOA.StoredInfo.woTime, aSdu.getOriginatorAddress()));
                                    break;
                                case 2:
                                    this.IOAs.add(new IOA(ioa, IOA.StoredInfo.wTime65, aSdu.getOriginatorAddress()));
                                    break;
                            }
                        }
                        type++;
                    }
                    for (IOA ioa : IOAs){
                        IOAs_mapped.put(ioa.getIOA(), ioa);
                    }
                    this.dbdata = new dbWork(this.IOAs, connection);

                    this.initialization = true;
                }

                dbdata.rewriteSend(aSdu);

//                switch (aSdu.getTypeIdentification()) {
//                    case C_IC_NA_1:
//                        dbdata.introgen(aSdu);
//                        break;
//                    case C_SC_NA_1: case C_DC_NA_1: case C_RC_NA_1: case C_BO_NA_1: case C_SE_NA_1: case C_SE_NB_1: case C_SE_NC_1:
//
//                        break;
//                    default:
//                        connection.sendUnknown(aSdu, Connection.UnknownInfo.TID);
//                        break;
//                }
            }



            public void SubstationToScada_Normal(ASdu aSdu) {
                Integer[] knownASDU = {3};
                CauseOfTransmission[] knownCauses = {CauseOfTransmission.ACTIVATION};
                TypeId[] knownTypesASDU_requests = {TypeId.C_IC_NA_1,TypeId.C_DC_NA_1};
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

                TypeId[] NormalTypesOfIOA = {TypeId.M_SP_NA_1, TypeId.M_SP_NA_1, TypeId.M_SP_NA_1, TypeId.M_DP_NA_1, TypeId.M_ST_NA_1};
//                TypeId[] SpecialTypesOfIOA = {TypeId.C_DC_NA_1};

                try {
                    if(connection.checkCorrect(aSdu,knownASDU,knownIOA,knownCauses,knownTypesASDU_requests)){
                        connection.sendConfirmation(aSdu);
                        ASdu send = null;

                        switch (aSdu.getTypeIdentification()) {
                            case C_IC_NA_1:
                                for (int i = 0; i < NormalTypesOfIOA.length; i++){
                                    int indexIOA = 0;
                                    InformationObject informationObjects[] = new InformationObject[knownIOA[i].length];
                                    InformationElement[][] informationElements = null;
                                    for (Integer IOA : knownIOA[i]){
                                        switch (i){
                                            case 0: case 1: case 2:
                                                informationElements = new InformationElement[][]{{new IeQualifierOfInterrogation(0)}};
                                                break;
                                            case 3:
                                                informationElements = new InformationElement[][]{{new IeDoubleCommand(IeDoubleCommand.DoubleCommandState.OFF, 0, false)}};
                                                break;
                                            case 4:
                                                informationElements = new InformationElement[][]{{ new IeValueWithTransientState(-64, false), new IeQualifierOfInterrogation(64) }};
                                                break;
                                            default:
                                                break;
                                        }
                                        informationObjects[indexIOA] = new InformationObject(IOA,informationElements);
                                        indexIOA++;
                                    }

                                    send = new ASdu(NormalTypesOfIOA[i], false, CauseOfTransmission.INTERROGATED_BY_STATION, false, false,
                                            aSdu.getOriginatorAddress(), aSdu.getCommonAddress(),
                                            informationObjects);

                                    connection.send(send);
                                    System.out.println(send.toString());

                                }
                                break;
                            case C_DC_NA_1:
                                //do not need to do anything... no Introgen communication required only sending CON & TERM
//                                connection.doubleCommand(aSdu.getCommonAddress(), CauseOfTransmission.ACTIVATION_CON, knownIOA[5][0], new IeDoubleCommand(IeDoubleCommand.DoubleCommandState.ON, 0, false));
                                break;
                            default:
                                break;

                        }
                        if (aSdu.getCauseOfTransmission() == CauseOfTransmission.ACTIVATION) {
                            send = new ASdu(aSdu.getTypeIdentification(), false, CauseOfTransmission.ACTIVATION_TERMINATION, false, false,
                                    aSdu.getOriginatorAddress(), aSdu.getCommonAddress(),aSdu.getInformationObjects());
                            connection.send(send);
                            System.out.println(send.toString());
                        }
                    }
                } catch (EOFException e) {
                    LOGGER.log(Level.SEVERE, "Will quit listening for commands on connection (" + connectionId
                            + ") because socket was closed.");
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Will quit listening for commands on connection (" + connectionId
                            + ") because of error: \"" + e.getMessage() + "\".");
                }
            }


            public void iec104_first(ASdu aSdu) {
                Integer[] knownASDU = {10};
                CauseOfTransmission[] knownCauses = {CauseOfTransmission.ACTIVATION};
                TypeId[] knownTypesASDU_requests = {TypeId.C_IC_NA_1,TypeId.C_SC_NA_1,TypeId.C_DC_NA_1,TypeId.C_RC_NA_1,
                                                    TypeId.C_BO_NA_1,TypeId.C_SE_NA_1,TypeId.C_SE_NB_1,TypeId.C_SE_NC_1};
                Integer[][] knownIOA = {
                        {1,2,3,4},
                        {11,12,13,14},
                        {0}
                };

                TypeId[] NormalTypesOfIOA = { TypeId.M_SP_NA_1, TypeId.M_DP_NA_1, TypeId.M_ST_NA_1, TypeId.M_BO_NA_1, TypeId.M_ME_NA_1,
                                              TypeId.M_ME_NB_1, TypeId.M_ME_NC_1, TypeId.M_SP_TB_1, TypeId.M_DP_TB_1, TypeId.M_ST_TB_1,
                                              TypeId.M_BO_TB_1, TypeId.M_ME_TD_1, TypeId.M_ME_TE_1, TypeId.M_ME_TF_1};




//                TypeId[] SpecialTypesOfIOA = {  TypeId.C_IC_NA_1,TypeId.C_SC_NA_1,TypeId.C_DC_NA_1,TypeId.C_RC_NA_1,
//                                                TypeId.C_BO_NA_1,TypeId.C_SE_NA_1,TypeId.C_SE_NB_1,TypeId.C_SE_NC_1};

                try {
                    if(connection.checkCorrect(aSdu,knownASDU,knownIOA,knownCauses,knownTypesASDU_requests)){
                        if(this.init[0] == true) {
                            connection.sendConfirmation(aSdu);
                        }
                        ASdu send = null;

                        switch (aSdu.getTypeIdentification()) {
                            case C_IC_NA_1:
                                if(this.init[0] == false){
                                    ASdu s = new ASdu(TypeId.M_EI_NA_1, false, CauseOfTransmission.INITIALIZED, false, false,
                                            0, aSdu.getCommonAddress(),
                                            new InformationObject[] { new InformationObject(0, new InformationElement[][] { { new IeCauseOfInitialization(0, false) } }) });

                                    connection.send(s);
                                    System.out.println(s.toString());
                                    this.init[0] = true;
//                                    break;
                                }
                                for (int i = 0; i < NormalTypesOfIOA.length; i++){
                                    int indexIOA = 0;
                                    InformationObject informationObjects[] = new InformationObject[knownIOA[i >= 7 ? 1 : 0].length];
                                    InformationElement[][] informationElements = null;
                                    for (Integer IOA : knownIOA[i >= 7 ? 1 : 0]){
                                        switch (i >= 7 ? i-7 : i){
                                            case 0:
                                                informationElements = new InformationElement[][]{{new IeQualifierOfInterrogation(0)}};
                                                break;
                                            case 1:
                                                informationElements = new InformationElement[][]{{new IeDoubleCommand(IeDoubleCommand.DoubleCommandState.NOT_PERMITTED_A, 0, false)}};
                                                break;
                                            case 2:
                                                informationElements = new InformationElement[][]{{ new IeValueWithTransientState(0, false), new IeQualifierOfInterrogation(0) }};
                                                break;
                                            case 3:
                                                informationElements = new InformationElement[][]{{new IeBinaryStateInformation(0), new IeQualifierOfInterrogation(0)}};
                                                break;
                                            case 4:
                                                informationElements = new InformationElement[][]{{ new IeNormalizedValue(0), new IeQualifierOfInterrogation(0) }};
                                                break;
                                            case 5:
                                                informationElements = new InformationElement[][]{{ new IeScaledValue(0), new IeQualifierOfInterrogation(0) }};
                                                break;
                                            case 6:
                                                informationElements = new InformationElement[][]{{ new IeShortFloat(0), new IeQualifierOfInterrogation(0) }};
                                                break;
                                            default:
                                                break;

                                        }
                                        if(i >= 7){//adding timestamp to existing warriant
                                            informationElements = new InformationElement[][]{informationElements[0], {new IeTime56(System.currentTimeMillis())}};
                                        }else{}
                                        informationObjects[indexIOA] = new InformationObject(IOA,informationElements);
                                        indexIOA++;
                                    }

                                    send = new ASdu(NormalTypesOfIOA[i], false, CauseOfTransmission.INTERROGATED_BY_STATION, false, false,
                                            aSdu.getOriginatorAddress(), aSdu.getCommonAddress(),
                                            informationObjects);

                                    connection.send(send);
                                    System.out.println(send.toString());

                                }
                                break;
                            default:
                                break;// nothing to be done here

                        }

                        if (aSdu.getCauseOfTransmission() == CauseOfTransmission.ACTIVATION && this.init[1]) {
                            send = new ASdu(aSdu.getTypeIdentification(), false, CauseOfTransmission.ACTIVATION_TERMINATION, false, false,
                                    aSdu.getOriginatorAddress(), aSdu.getCommonAddress(),aSdu.getInformationObjects());
                            connection.send(send);
                            System.out.println(send.toString());
                        }else if(!this.init[1]){
                            this.init[1] = true;
                        }else{}

                        // after termination spontaneous part
                        if(aSdu.getTypeIdentification() != TypeId.C_IC_NA_1){
                            InformationObject info[] = aSdu.getInformationObjects();

                            int IOA = 0;

                            for (InformationObject obj : info){
                                InformationObject informationObjects[] = new InformationObject[info.length];
                                TypeId toBe = TypeId.M_SP_NA_1;
                                InformationElement[][] informationElements = null;
                                switch (aSdu.getTypeIdentification()) {
                                    case C_SC_NA_1:
                                        toBe = connection.contains(knownIOA[1], obj.getInformationObjectAddress()) ? TypeId.M_SP_TB_1 : TypeId.M_SP_NA_1;
                                        informationElements = new InformationElement[][]{{
                                                new IeSinglePointWithQuality(true, false, false, false, false)}};
                                        break;
                                    case C_DC_NA_1:
                                        toBe = connection.contains(knownIOA[1], obj.getInformationObjectAddress()) ? TypeId.M_DP_TB_1 : TypeId.M_DP_NA_1;
                                        informationElements = connection.contains(knownIOA[1], obj.getInformationObjectAddress())?
                                                new InformationElement[][]{{new IeDoubleCommand(IeDoubleCommand.DoubleCommandState.ON, 0, false)}} :
                                                new InformationElement[][]{{new IeDoubleCommand(IeDoubleCommand.DoubleCommandState.OFF, 0, false)}};
                                        break;
                                    case C_RC_NA_1:
                                        toBe = connection.contains(knownIOA[1], obj.getInformationObjectAddress()) ? TypeId.M_ST_TB_1 : TypeId.M_ST_NA_1;
                                        informationElements = connection.contains(knownIOA[1], obj.getInformationObjectAddress())?
                                                new InformationElement[][]{{new IeValueWithTransientState(-1, false), new IeQualifierOfInterrogation(0)}}:
                                                new InformationElement[][]{{new IeValueWithTransientState(1, false), new IeQualifierOfInterrogation(0)}};
                                        break;
                                    case C_BO_NA_1:
                                        toBe = connection.contains(knownIOA[1], obj.getInformationObjectAddress()) ? TypeId.M_BO_TB_1 : TypeId.M_BO_NA_1;
                                        informationElements = connection.contains(knownIOA[1], obj.getInformationObjectAddress())?
                                                new InformationElement[][]{{new IeBinaryStateInformation(1<<26 ), new IeQualifierOfInterrogation(0)}}:
                                                new InformationElement[][]{{new IeBinaryStateInformation(1<<25 ), new IeQualifierOfInterrogation(0)}};
                                        break;
                                    case C_SE_NA_1:
                                        toBe = connection.contains(knownIOA[1], obj.getInformationObjectAddress()) ? TypeId.M_ME_TD_1 : TypeId.M_ME_NA_1;
                                        informationElements = connection.contains(knownIOA[1], obj.getInformationObjectAddress())?
                                                new InformationElement[][]{{new IeNormalizedValue(8192), new IeQualifierOfSetPointCommand(0, false)}}:
                                                new InformationElement[][]{{new IeNormalizedValue(1024), new IeQualifierOfSetPointCommand(0, false)}};
                                        break;
                                    case C_SE_NB_1:
                                        toBe = connection.contains(knownIOA[1], obj.getInformationObjectAddress()) ? TypeId.M_ME_TE_1 : TypeId.M_ME_NB_1;
                                        informationElements = connection.contains(knownIOA[1], obj.getInformationObjectAddress())?
                                                new InformationElement[][]{{new IeScaledValue(456), new IeQualifierOfSetPointCommand(0, false)}}:
                                                new InformationElement[][]{{new IeScaledValue(123), new IeQualifierOfSetPointCommand(0, false)}};
                                        break;
                                    case C_SE_NC_1:
                                        toBe = connection.contains(knownIOA[1], obj.getInformationObjectAddress()) ? TypeId.M_ME_TF_1 : TypeId.M_ME_NC_1;
                                        informationElements = connection.contains(knownIOA[1], obj.getInformationObjectAddress())?
                                                new InformationElement[][]{{new IeShortFloat((float)9.87), new IeQualifierOfSetPointCommand(0, false)}}:
                                                new InformationElement[][]{{new IeShortFloat((float)3.14), new IeQualifierOfSetPointCommand(0, false)}};
                                        break;
                                    default:
                                        break;// nothing to be done here
                                }
                                if (connection.contains(knownIOA[1], obj.getInformationObjectAddress())){
                                    informationElements = new InformationElement[][]{informationElements[0], {new IeTime56(System.currentTimeMillis())}};
                                }
                                informationObjects[IOA] = new InformationObject(obj.getInformationObjectAddress(),informationElements);
                                IOA++;
                                send = new ASdu(toBe, false, CauseOfTransmission.SPONTANEOUS, false, false,
                                        aSdu.getOriginatorAddress(), aSdu.getCommonAddress(), informationObjects);
                                connection.send(send);
                                System.out.println(send.toString());
                            }
                        }
                    }
                } catch (EOFException e) {
                    LOGGER.log(Level.SEVERE, "Will quit listening for commands on connection (" + connectionId
                            + ") because socket was closed.");
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Will quit listening for commands on connection (" + connectionId
                            + ") because of error: \"" + e.getMessage() + "\".");
                }


            }



        }

        public void connectionIndication(Connection connection) {

            int myConnectionId = connectionIdCounter++;
            LOGGER.log(Level.INFO, " Client Connected ID:{0}", myConnectionId);

            try {
                connection.waitForStartDT(new ConnectionListener(connection, myConnectionId), 5000);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, " ID:{0} interrupted while waiting for StartDT:{1}", new Object[]{myConnectionId, e.getMessage()});
                return;
            } catch (TimeoutException e) {
                LOGGER.log(Level.SEVERE, e.getMessage());
            }

            LOGGER.log(Level.INFO, " Handshake complete for ID:{0}", myConnectionId);

        }

        public void serverStoppedListeningIndication(IOException e) {
            LOGGER.log(Level.INFO, "Server has stopped listening for new connections : {}", e.getMessage());
        }

        public void connectionAttemptFailed(IOException e) {
            LOGGER.log(Level.INFO, "Connection attempt failed: {0}", e.getMessage());
        }
    }

    private int connectionIdCounter = 1;

    public static void main(String[] args) {
        new RTU().start();
    }

    public void start() {
        Server server = new Server.Builder().build();

        try {
            server.start(new ServerListener());
            LOGGER.log(Level.INFO, " RTU Server started!");
            System.out.println("RTU Started.");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Unable to start listening: {0}", e.getMessage());
            return;
        }
    }
}

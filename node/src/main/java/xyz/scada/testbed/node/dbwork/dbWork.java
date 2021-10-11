package xyz.scada.testbed.node.dbwork;

import org.openmuc.j60870.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class dbWork {
    private ArrayList<IOA> IOAs;
    private Map<Integer, IOA> IOAm = new HashMap<Integer, IOA>();
    private final Connection connection;

    public dbWork(ArrayList<IOA> IOAs, Connection connection){
        this.connection = connection;
        this.IOAs = IOAs;
        for (IOA IoA : IOAs){
            this.IOAm.put(IoA.getIOA(), IoA);
        }

    }

    public void addIOAm(IOA ioa){
        this.IOAm.put(ioa.getIOA(), ioa);
    }

    public void rewriteSend(ASdu aSdu) throws IOException {
        switch (aSdu.getTypeIdentification()) {
            case C_IC_NA_1:
                introgen(aSdu);
                break;
            case C_SC_NA_1: case C_DC_NA_1: case C_RC_NA_1: case C_BO_NA_1: case C_SE_NA_1: case C_SE_NB_1: case C_SE_NC_1:
                checkRewrite(aSdu);
                sendSpont(aSdu);
                break;
            default:
                connection.sendUnknown(aSdu, Connection.UnknownInfo.TID);
                break;
        }
    }

    private void spontSent(ASdu aSdu, IOA ioa, TypeId response, InformationElement[][] informationElements) throws IOException {
        ASdu send = new ASdu(response,
                false, CauseOfTransmission.SPONTANEOUS, false, false,
                aSdu.getOriginatorAddress(), aSdu.getCommonAddress(),
                new InformationObject[]{new InformationObject(ioa.getIOA(), informationElements)});
        connection.send(send);
        System.out.println(send.toString());
    }

    private void sendSpont(ASdu aSdu) throws IOException {
        for (InformationObject IoA : aSdu.getInformationObjects()) {
            IOA ioa = IOAm.get(IoA.getInformationObjectAddress());
            if (ioa != null) {
                TypeId id = aSdu.getTypeIdentification();

                if(ioa.singlePoint.getRequest() == id) {
                    spontSent(aSdu, ioa, ioa.singlePoint.getResponse(), ioa.singlePoint.singlePointElement());
                }else if(ioa.doublePoint.getRequest() == id) {
                    spontSent(aSdu, ioa, ioa.doublePoint.getResponse(), ioa.doublePoint.doublePointElement());
                }else if(ioa.stepPosition.getRequest() == id) {
                    spontSent(aSdu, ioa, ioa.stepPosition.getResponse(), ioa.stepPosition.stepPositionElement());
                }else if(ioa.bitString.getRequest() == id) {
                    spontSent(aSdu, ioa, ioa.bitString.getResponse(), ioa.bitString.bitStringElement());
                }else if(ioa.measureNormal.getRequest() == id) {
                    spontSent(aSdu, ioa, ioa.measureNormal.getResponse(), ioa.measureNormal.measureNormalElement());
                }else if(ioa.measureScaled.getRequest() == id) {
                    spontSent(aSdu, ioa, ioa.measureScaled.getResponse(), ioa.measureScaled.measureScaledElement());
                }else if(ioa.measureShort.getRequest() == id) {
                    spontSent(aSdu, ioa, ioa.measureShort.getResponse(), ioa.measureShort.measureShortElement());
                }else{}
            }
        }
    }

    private boolean decodeSelected(String string){
        Pattern pattern = Pattern.compile("selected: true", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(string);
        return matcher.find();
    }

    private Integer decodeQualifier(String string){
        Pattern pattern = Pattern.compile("qualifier: (.*)");
        Matcher matcher = pattern.matcher(string);
        String q = matcher.find() ? matcher.group(1) : "0";
        return (Integer.parseInt(q) << 2);
    }


    private boolean checkRewrite(ASdu aSdu) throws IOException {
        System.out.println(aSdu);
        if(aSdu.getOriginatorAddress() != IOAm.get(0).getASdu()){
            connection.sendUnknown(aSdu, Connection.UnknownInfo.ASDU);
            return false;
        }
        if(aSdu.getCauseOfTransmission() != CauseOfTransmission.ACTIVATION){
            connection.sendUnknown(aSdu, Connection.UnknownInfo.COT);
            return false;
        }
        for(InformationObject IoA : aSdu.getInformationObjects()){
            IOA ioa = IOAm.get(IoA.getInformationObjectAddress());
            InformationElement[][] elements = IoA.getInformationElements();

            if(ioa != null){
                TypeId id = aSdu.getTypeIdentification();
                if(ioa.info != IOA.StoredInfo.interrogation){
                    if(ioa.singlePoint.getRequest() == id){
                        ioa.singlePoint.decodeMessage(elements);
                    }else if(ioa.doublePoint.getRequest() == id){
                        ioa.doublePoint.decodeMessage(elements);
                    }else if(ioa.stepPosition.getRequest() == id){
                        ioa.stepPosition.decodeMessage(elements);
                    }else if(ioa.bitString.getRequest() == id){
                        ioa.bitString.decodeMessage(elements);
                    }else if(ioa.measureNormal.getRequest() == id){
                        ioa.measureNormal.decodeMessage(elements);
                    }else if(ioa.measureScaled.getRequest() == id){
                        ioa.measureScaled.decodeMessage(elements);
                    }else if(ioa.measureShort.getRequest() == id){
                        ioa.measureShort.decodeMessage(elements);
                    }else{
                        connection.sendUnknown(aSdu, Connection.UnknownInfo.TID);
                        return false;
                    }

                }
            }else{
                connection.sendUnknown(aSdu, Connection.UnknownInfo.IOA);
                return false;
            }
        }
        connection.sendConfirmation(aSdu);
        ASdu send = new ASdu(aSdu.getTypeIdentification(), false, CauseOfTransmission.ACTIVATION_TERMINATION, false, false,
                aSdu.getOriginatorAddress(), aSdu.getCommonAddress(),aSdu.getInformationObjects());
        connection.send(send);
        System.out.println(send.toString());
        return true;
    }

    public void introgen( ASdu aSdu) throws IOException {
        IOA initiator = this.IOAm.get(aSdu.getInformationObjects()[0].getInformationObjectAddress());
        if(aSdu.getCauseOfTransmission() != CauseOfTransmission.ACTIVATION){
            connection.sendUnknown(aSdu, Connection.UnknownInfo.COT);
            return;
        }else if (initiator == null){
            connection.sendUnknown(aSdu, Connection.UnknownInfo.IOA);
            return;
        }else if(initiator.interrogation == null){
            connection.sendUnknown(aSdu, Connection.UnknownInfo.IOA);
            return;
        }
        else{
            connection.sendConfirmation(aSdu);
        }
        ArrayList<ASdu> aSdu_woTime = new ArrayList<ASdu>();
        ArrayList<ASdu> aSdu_wTime56 = new ArrayList<ASdu>();
        for(int i = 0; i < 7; i++){
            ArrayList<InformationObject> informationObjects_woTime = new ArrayList<InformationObject>();
            ArrayList<InformationObject> informationObjects_wTime56 = new ArrayList<InformationObject>();
            for(IOA ioa : this.IOAs){
                if(ioa.interrogation == null){
                    InformationElement[][] informationElements = null;
                    switch (i){
                        case 0:
                            informationElements = ioa.singlePoint.singlePointElement();
                            break;
                        case 1:
                            informationElements = ioa.doublePoint.doublePointElement();
                            break;
                        case 2:
                            informationElements = ioa.stepPosition.stepPositionElement();
                            break;
                        case 3:
                            informationElements = ioa.bitString.bitStringElement();
                            break;
                        case 4:
                            informationElements = ioa.measureNormal.measureNormalElement();
                            break;
                        case 5:
                            informationElements = ioa.measureScaled.measureScaledElement();
                            break;
                        case 6:
                            informationElements = ioa.measureShort.measureShortElement();
                            break;
                        default:
                            break;
                    }
                    if (informationElements == null){}
                    else if (ioa.info == IOA.StoredInfo.woTime){
                        informationObjects_woTime.add(new InformationObject(ioa.getIOA(), informationElements));
                    }else if (ioa.info == IOA.StoredInfo.wTime65){
                        informationObjects_wTime56.add(new InformationObject(ioa.getIOA(), informationElements));
                    }else{}
                }
            }

            if(!informationObjects_woTime.isEmpty()){
                TypeId[] NormalTypesOfIOA = {   TypeId.M_SP_NA_1, TypeId.M_DP_NA_1, TypeId.M_ST_NA_1, TypeId.M_BO_NA_1,
                        TypeId.M_ME_NA_1, TypeId.M_ME_NB_1, TypeId.M_ME_NC_1    };
                InformationObject[] infoObject = new InformationObject[informationObjects_woTime.size()];
                int k = 0;
                for(InformationObject object : informationObjects_woTime){
                    infoObject[k] = object;
                    k++;
                }

                aSdu_woTime.add(
                        new ASdu(NormalTypesOfIOA[i],
                                false,
                                CauseOfTransmission.INTERROGATED_BY_STATION,
                                false,
                                false,
                                aSdu.getOriginatorAddress(),
                                aSdu.getCommonAddress(),
                                infoObject )
                );
            }
            if(!informationObjects_wTime56.isEmpty()){
                TypeId[] NormalTypesOfIOA = {   TypeId.M_SP_TB_1, TypeId.M_DP_TB_1, TypeId.M_ST_TB_1, TypeId.M_BO_TB_1,
                        TypeId.M_ME_TD_1, TypeId.M_ME_TE_1, TypeId.M_ME_TF_1    };

                InformationObject[] infoObject = new InformationObject[informationObjects_wTime56.size()];
                int k = 0;
                for(InformationObject object : informationObjects_wTime56){
                    infoObject[k] = object;
                    k++;
                }

                aSdu_wTime56.add(
                        new ASdu(NormalTypesOfIOA[i],
                                false,
                                CauseOfTransmission.INTERROGATED_BY_STATION,
                                false,
                                false,
                                aSdu.getOriginatorAddress(),
                                aSdu.getCommonAddress(),
                                infoObject )
                );
            }

        }

        for (ASdu asdu : aSdu_woTime){
            for(InformationObject IoA : aSdu.getInformationObjects()) {
                System.out.println(IoA.getInformationObjectAddress());
            }
            connection.send(asdu);
            System.out.println(asdu.toString());
        }
        for (ASdu asdu : aSdu_wTime56){
            connection.send(asdu);
            System.out.println(asdu.toString());
        }
        ASdu send = new ASdu(aSdu.getTypeIdentification(), false, CauseOfTransmission.ACTIVATION_TERMINATION, false, false,
                aSdu.getOriginatorAddress(), aSdu.getCommonAddress(),aSdu.getInformationObjects());
        connection.send(send);
        System.out.println(send.toString());
    }

    public void addIoAsHmi(ASdu aSdu){
        for (InformationObject object : aSdu.getInformationObjects() ) {
            int ioa = object.getInformationObjectAddress();
            InformationElement[][] elements = object.getInformationElements();
            if (this.IOAm.get(ioa) == null) {
                this.IOAm.put(ioa, new IOA(ioa, IOA.StoredInfo.hmi, aSdu.getCommonAddress()));
            }

            switch(aSdu.getTypeIdentification()){
                case C_IC_NA_1:
                    IOAm.get(ioa).setInterrogation();
                    break;
                case M_SP_NA_1: case M_SP_TB_1:
                    IOAm.get(ioa).setSinglePoint(aSdu.getTypeIdentification() != TypeId.M_SP_TB_1 ? IOA.StoredInfo.woTime : IOA.StoredInfo.wTime65);
                    IOAm.get(ioa).singlePoint.decodeMessage(elements);
                    break;
                case M_DP_NA_1: case M_DP_TB_1:
                    IOAm.get(ioa).setDoublePoint(aSdu.getTypeIdentification() != TypeId.M_DP_TB_1 ? IOA.StoredInfo.woTime : IOA.StoredInfo.wTime65);
                    IOAm.get(ioa).doublePoint.decodeMessage(elements);
                    break;
                case M_ST_NA_1: case M_ST_TB_1:
                    IOAm.get(ioa).setStepPosition(aSdu.getTypeIdentification() != TypeId.M_ST_TB_1 ? IOA.StoredInfo.woTime : IOA.StoredInfo.wTime65);
                    IOAm.get(ioa).stepPosition.decodeMessage(elements);
                    break;
                case M_BO_NA_1: case M_BO_TB_1:
                    IOAm.get(ioa).setBitString(aSdu.getTypeIdentification() != TypeId.M_BO_TB_1 ? IOA.StoredInfo.woTime : IOA.StoredInfo.wTime65);
                    IOAm.get(ioa).bitString.decodeMessage(elements);
                    break;
                case M_ME_NA_1: case M_ME_TD_1:
                    IOAm.get(ioa).setMeasureNormal(aSdu.getTypeIdentification() != TypeId.M_ME_TD_1 ? IOA.StoredInfo.woTime : IOA.StoredInfo.wTime65);
                    IOAm.get(ioa).measureNormal.decodeMessage(elements);
                    break;
                case M_ME_NB_1: case M_ME_TE_1:
                    IOAm.get(ioa).setMeasureScaled(aSdu.getTypeIdentification() != TypeId.M_ME_TE_1 ? IOA.StoredInfo.woTime : IOA.StoredInfo.wTime65);
                    IOAm.get(ioa).measureScaled.decodeMessage(elements);
                    break;
                case M_ME_NC_1: case M_ME_TF_1:
                    IOAm.get(ioa).setMeasureShort(aSdu.getTypeIdentification() != TypeId.M_ME_TF_1 ? IOA.StoredInfo.woTime : IOA.StoredInfo.wTime65);
                    IOAm.get(ioa).measureShort.decodeMessage(elements);
                    break;
                default:
                    break;
            }

        }

    }

    public void checkASduHmi(ASdu aSdu) throws IOException { /// checking actual values might be added in future
        for (InformationObject object : aSdu.getInformationObjects() ) {
            int ioa = object.getInformationObjectAddress();
            InformationElement[][] elements = object.getInformationElements();

            if(this.IOAm.get(ioa) == null){
                connection.sendUnknown(aSdu, Connection.UnknownInfo.IOA);
            }else if(this.IOAm.get(ioa).getASdu() != aSdu.getCommonAddress()){
                connection.sendUnknown(aSdu, Connection.UnknownInfo.ASDU);
            }else{

            }
        }
    }
}

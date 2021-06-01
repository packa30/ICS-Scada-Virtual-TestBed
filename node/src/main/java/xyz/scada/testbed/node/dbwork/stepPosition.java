package xyz.scada.testbed.node.dbwork;

import org.openmuc.j60870.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class stepPosition {
    private final TypeId request;
    private final TypeId response;
    private IeValueWithTransientState vty;
    private IeQuality qualifier;
    private final IOA.StoredInfo time;
    private IeTime56 time56;

    stepPosition(IOA.StoredInfo info){
        this.request = TypeId.C_RC_NA_1;
        this.vty = new IeValueWithTransientState(0, false);
        this.qualifier = new IeQuality(false, false, false, false, false);
        this.time = info;

        switch (info){
            case wTime65:
                this.time56 = new IeTime56(System.currentTimeMillis());
                this.response = TypeId.M_ST_TB_1;
                break;
            default:
                this.response = TypeId.M_ST_NA_1;
                break;
        }
    }

    public TypeId getRequest() {
        return request;
    }

    public TypeId getResponse() {
        return response;
    }

    public IeValueWithTransientState getVty() {
        return vty;
    }

    public void setVty(IeValueWithTransientState vty) {
        this.vty = vty;
    }

    public IeQuality getQualifier() {
        return qualifier;
    }

    public void setQualifier(IeQuality qualifier) {
        this.qualifier = qualifier;
    }

    public IeTime56 getTime56() {
        return time56;
    }

    public void setTime56() {
        this.time56 = new IeTime56(System.currentTimeMillis());
    }

    public InformationElement[][] stepPositionElement(){
        return this.time != IOA.StoredInfo.interrogation ?
                this.time == IOA.StoredInfo.woTime ?
                        new InformationElement[][]{{
                                this.getVty(),
                                this.qualifier }} :
                        new InformationElement[][]{{
                                this.getVty(),
                                this.qualifier,
                                this.time56 }} :
                null;
    }

    private Integer decodeStepState( String string){
        Pattern pattern1 = Pattern.compile("state: NOT_PERMITTED_A", Pattern.CASE_INSENSITIVE);
        Pattern pattern2 = Pattern.compile("state: NEXT_STEP_LOWER", Pattern.CASE_INSENSITIVE);
        Pattern pattern3 = Pattern.compile("state: NEXT_STEP_HIGHER", Pattern.CASE_INSENSITIVE);
        Pattern pattern4 = Pattern.compile("state: NOT_PERMITTED_B", Pattern.CASE_INSENSITIVE);
        if (pattern1.matcher(string).find()){ return 0;}
        else if (pattern2.matcher(string).find()){return 1;}
        else if (pattern3.matcher(string).find()){return 2;}
        else if (pattern4.matcher(string).find()){return 3;}
        else{ return 0;}
    }

    private IeRegulatingStepCommand.StepCommandState stepHelp(Integer State){
        switch (State){
            case 0:
                return IeRegulatingStepCommand.StepCommandState.NOT_PERMITTED_A;
            case 1:
                return IeRegulatingStepCommand.StepCommandState.NEXT_STEP_LOWER;
            case 2:
                return IeRegulatingStepCommand.StepCommandState.NEXT_STEP_HIGHER;
            default:
                return IeRegulatingStepCommand.StepCommandState.NOT_PERMITTED_B;
        }
    }

    private Integer decodeQualifier(String string){
        Pattern pattern = Pattern.compile("qualifier: (.*)");
        Matcher matcher = pattern.matcher(string);
        String q = matcher.find() ? matcher.group(1) : "0";
        return (Integer.parseInt(q) << 2);
    }

    private boolean decodeSelected(String string){
        Pattern pattern = Pattern.compile("selected: true", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(string);
        return matcher.find();
    }

    public void decodeMessage(InformationElement[][] elements){
        if (elements[0][0] != null) {
            String elem = elements[0][0].toString();
            Integer val =   decodeQualifier(elem) << 2 |
                    (decodeSelected(elem) ? 0x80 : 0) |
                    (decodeStepState(elem));

            this.setVty(
                    new IeValueWithTransientState(
                            stepHelp(decodeStepState(elem)) == IeRegulatingStepCommand.StepCommandState.NEXT_STEP_HIGHER ?
                                    this.getVty().getValue() + 1 :
                            stepHelp(decodeStepState(elem)) == IeRegulatingStepCommand.StepCommandState.NEXT_STEP_LOWER ?
                                    this.getVty().getValue() - 1 :
                                    this.getVty().getValue(),
                            this.getVty().isTransientState()
                    )
            );

            this.setQualifier(
                    new IeQuality(
                            (val & 0x01) == 0x01 ? true : false,
                            (val & 0x10) == 0x10 ? true : false,
                            (val & 0x20) == 0x20 ? true : false,
                            (val & 0x40) == 0x40 ? true : false,
                            (val & 0x80) == 0x80 ? true : false
                    )
            );

            if(this.time == IOA.StoredInfo.wTime65){
                this.setTime56();
            }

        }else{}
    }
}

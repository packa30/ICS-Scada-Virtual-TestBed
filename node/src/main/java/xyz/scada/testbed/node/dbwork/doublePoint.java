package xyz.scada.testbed.node.dbwork;

import org.openmuc.j60870.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class doublePoint {
    private final TypeId request;
    private final TypeId response;
    private IeDoublePointWithQuality qualifier;
    private final IOA.StoredInfo time;
    private IeTime56 time56;

    doublePoint(IOA.StoredInfo info){
        this.request = TypeId.C_DC_NA_1;
        this.qualifier = new IeDoublePointWithQuality(  IeDoublePointWithQuality.DoublePointInformation.INDETERMINATE_OR_INTERMEDIATE,
                false, false, false, false);
        this.time = info;

        switch (info){
            case wTime65:
                this.time56 = new IeTime56(System.currentTimeMillis());
                this.response = TypeId.M_DP_TB_1;
                break;
            default:
                this.response = TypeId.M_DP_NA_1;
                break;
        }

    }

    public TypeId getRequest() {
        return request;
    }

    public TypeId getResponse() {
        return response;
    }

    public IeDoublePointWithQuality getQualifier() {
        return qualifier;
    }

    public void setQualifier(IeDoublePointWithQuality qualifier) {
        this.qualifier = qualifier;
    }

    public IeTime56 getTime56() {
        return time56;
    }

    public void setTime56() {
        this.time56 = new IeTime56(System.currentTimeMillis());
    }

    public InformationElement[][] doublePointElement(){
        return this.time != IOA.StoredInfo.interrogation ?
                this.time == IOA.StoredInfo.woTime ?
                        new InformationElement[][]{{
                                this.qualifier }} :
                        new InformationElement[][]{{
                                this.qualifier,
                                this.time56 }} :
                null;
    }

    private Integer decodeDoubleState( String string){
        Pattern pattern1 = Pattern.compile("state: NOT_PERMITTED_A", Pattern.CASE_INSENSITIVE);
        Pattern pattern2 = Pattern.compile("state: OFF", Pattern.CASE_INSENSITIVE);
        Pattern pattern3 = Pattern.compile("state: ON", Pattern.CASE_INSENSITIVE);
        Pattern pattern4 = Pattern.compile("state: NOT_PERMITTED_B", Pattern.CASE_INSENSITIVE);
        if (pattern1.matcher(string).find()){ return 0;}
        else if (pattern2.matcher(string).find()){return 1;}
        else if (pattern3.matcher(string).find()){return 2;}
        else if (pattern4.matcher(string).find()){return 3;}
        else{ return 0;}
    }

    private IeDoublePointWithQuality.DoublePointInformation doubleHelp(Integer State){
        switch (State){
            case 0:
                return IeDoublePointWithQuality.DoublePointInformation.INDETERMINATE_OR_INTERMEDIATE;
            case 1:
                return IeDoublePointWithQuality.DoublePointInformation.OFF;
            case 2:
                return IeDoublePointWithQuality.DoublePointInformation.ON;
            default:
                return IeDoublePointWithQuality.DoublePointInformation.INDETERMINATE;
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
                    (decodeDoubleState(elem));
            this.setQualifier(
                    new IeDoublePointWithQuality(
                            doubleHelp(decodeDoubleState(elem)),
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

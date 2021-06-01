package xyz.scada.testbed.node.dbwork;

import org.openmuc.j60870.IeSinglePointWithQuality;
import org.openmuc.j60870.IeTime56;
import org.openmuc.j60870.InformationElement;
import org.openmuc.j60870.TypeId;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class singlePoint {
    private final TypeId request;
    private final TypeId response;
    private IeSinglePointWithQuality qualifier;
    private final IOA.StoredInfo time;
    private IeTime56 time56 = null;

    singlePoint(IOA.StoredInfo info){
        this.qualifier = new IeSinglePointWithQuality(false,false,false,false,false);
        this.time = info;
        this.request = TypeId.C_SC_NA_1;

        switch (info){
            case wTime65:
                this.time56 = new IeTime56(System.currentTimeMillis());
                this.response = TypeId.M_SP_TB_1;
                break;
            default:
                this.response = TypeId.M_SP_NA_1;
                break;
        }
    }

    public TypeId getRequest() {
        return request;
    }

    public TypeId getResponse() {
        return response;
    }

    public IeSinglePointWithQuality getQualifier() {
        return qualifier;
    }

    public void setQualifier(IeSinglePointWithQuality qualifier) {
        this.qualifier = qualifier;
    }

    public IeTime56 getTime56() {
        return time56;
    }

    public void setTime56() {
        this.time56 = new IeTime56(System.currentTimeMillis());
    }

    public InformationElement[][] singlePointElement(){
        return this.time != IOA.StoredInfo.interrogation ?
                this.time == IOA.StoredInfo.woTime ?
                    new InformationElement[][]{{
                        this.qualifier }} :
                    new InformationElement[][]{{
                            this.qualifier,
                            this.time56 }} :
                null;
    }

    private boolean decodeSingleState(String string){
        Pattern pattern = Pattern.compile("state on: true", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(string);
        return matcher.find();
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
                    (decodeSingleState(elem) ? 0x01 : 0);
            this.setQualifier(
                    new IeSinglePointWithQuality(
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

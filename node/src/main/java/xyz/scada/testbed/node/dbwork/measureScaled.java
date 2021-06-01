package xyz.scada.testbed.node.dbwork;

import org.openmuc.j60870.*;

import java.util.regex.Pattern;

public class measureScaled {

    private final TypeId request;
    private final TypeId response;
    private Integer value;
    private Integer qualifier;
    private final IOA.StoredInfo time;
    private IeTime56 time56;

    measureScaled(IOA.StoredInfo info){
        this.request = TypeId.C_SE_NB_1;
        this.value = 0;
        this.qualifier = 0;
        this.time = info;

        switch (info){
            case wTime65:
                this.time56 = new IeTime56(System.currentTimeMillis());
                this.response = TypeId.M_ME_TE_1;
                break;
            default:
                this.response = TypeId.M_ME_NB_1;
                break;
        }
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }

    public Integer getQualifier() {
        return qualifier;
    }

    public void setQualifier(Integer qualifier) {
        this.qualifier = qualifier;
    }

    public TypeId getRequest() {
        return request;
    }

    public TypeId getResponse() {
        return response;
    }

    public IeTime56 getTime56() {
        return time56;
    }

    public void setTime56() {
        this.time56 = new IeTime56(System.currentTimeMillis());
    }

    public InformationElement[][] measureScaledElement(){
        return this.time != IOA.StoredInfo.interrogation ?
                this.time == IOA.StoredInfo.woTime ?
                        new InformationElement[][]{{
                            new IeScaledValue(
                                    this.value),
                            new IeQualifierOfInterrogation(
                                    this.qualifier) }} :
                        new InformationElement[][]{{
                            new IeScaledValue(
                                    this.value),
                            new IeQualifierOfInterrogation(
                                    this.qualifier),
                                this.time56 }} :
                null;
    }
    private Integer decodeScaled(String string){
        Pattern pattern1 = Pattern.compile("Scaled value: ", Pattern.CASE_INSENSITIVE);
        if (pattern1.matcher(string).find()){
            return Integer.parseInt(string.substring(14));
        }
        else{
            return 0;
        }
    }

    public void decodeMessage(InformationElement[][] elements){
        if (elements[0][0] != null){
            String elem = elements[0][0].toString();
            Integer decodedInput = decodeScaled(elem);

            if(decodedInput != 0){
                this.setValue(decodedInput);
            }else{}

            if(this.time == IOA.StoredInfo.wTime65){
                this.setTime56();
            }

        }else{}
    }
}

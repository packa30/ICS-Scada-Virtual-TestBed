package xyz.scada.testbed.node.dbwork;

import org.openmuc.j60870.*;

import java.util.regex.Pattern;

public class measureNormal {

    private final TypeId request;
    private final TypeId response;
    private double value;
    private Integer qualifier;
    private final IOA.StoredInfo time;
    private IeTime56 time56;

    measureNormal(IOA.StoredInfo info){
        this.request = TypeId.C_SE_NA_1;
        this.value = 0.0;
        this.qualifier = 0;
        this.time = info;

        switch (info){
            case wTime65:
                this.time56 = new IeTime56(System.currentTimeMillis());
                this.response = TypeId.M_ME_TD_1;
                break;
            default:
                this.response = TypeId.M_ME_NA_1;
                break;
        }
    }

    public double getValue() {
        return value;
    }

    public void setValue(double value) {
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

    public InformationElement[][] measureNormalElement(){
        return this.time != IOA.StoredInfo.interrogation ?
                this.time == IOA.StoredInfo.woTime ?
                        new InformationElement[][]{{
                            new IeNormalizedValue(
                                    this.value),
                            new IeQualifierOfInterrogation(
                                    this.qualifier) }} :
                        new InformationElement[][]{{
                            new IeNormalizedValue(
                                    this.value),
                            new IeQualifierOfInterrogation(
                                    this.qualifier),
                                this.time56 }} :
                null;
    }

    public double decodeNormalized(String string){
        Pattern pattern1 = Pattern.compile("Normalized value: ", Pattern.CASE_INSENSITIVE);
        if (pattern1.matcher(string).find()){
            return Double.parseDouble(string.substring(18));
        }
        else{
            return 0;
        }
    }

    public void decodeMessage(InformationElement[][] elements){
        if (elements[0][0] != null){
            String elem = elements[0][0].toString();
            Double decodedInput = decodeNormalized(elem);

            if(decodedInput != 0){
                this.setValue(decodedInput);
            }else{}

            if(this.time == IOA.StoredInfo.wTime65){
                this.setTime56();
            }

        }else{}
    }
}

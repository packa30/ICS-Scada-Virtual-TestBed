package xyz.scada.testbed.node.dbwork;

import org.openmuc.j60870.*;

public class IOA {

    public enum  StoredInfo{
        interrogation,
        woTime,
        wTime65,
        hmi
    }

    private final Integer IOA;
    private final int ASdu;
    public interrogation interrogation;

    public singlePoint singlePoint;
    public doublePoint doublePoint;
    public stepPosition stepPosition;
    public bitString bitString;
    public measureNormal measureNormal;
    public measureScaled measureScaled;
    public measureShort measureShort;
    public final StoredInfo info;

    public IOA(Integer value, StoredInfo info, int ASdu){
        this.IOA = value;
        this.ASdu = ASdu;
        this.info = info;

        if(info == StoredInfo.interrogation) {
            this.interrogation = new interrogation();
//        }else if(info == StoredInfo.hmi){
//            this.interrogation = null;
//            this.singlePoint = null;
//            this.doublePoint = null;
//            this.stepPosition = null;
//            this.bitString = null;
//            this.measureNormal = null;
//            this.measureScaled = null;
//            this.measureShort = null;
        }else{
            this.singlePoint = new singlePoint(info);
            this.doublePoint = new doublePoint(info);
            this.stepPosition = new stepPosition(info);
            this.bitString = new bitString(info);
            this.measureNormal = new measureNormal(info);
            this.measureScaled = new measureScaled(info);
            this.measureShort = new measureShort(info);
        }

    }

    public void setInterrogation() {
        this.interrogation = new interrogation();
    }

    public void setSinglePoint(StoredInfo info) {
        this.singlePoint = new singlePoint(info);
    }

    public void setDoublePoint(StoredInfo info) {
        this.doublePoint = new doublePoint(info);
    }

    public void setStepPosition(StoredInfo info) {
        this.stepPosition = new stepPosition(info);
    }

    public void setBitString(StoredInfo info) {
        this.bitString = new bitString(info);
    }

    public void setMeasureNormal(StoredInfo info) {
        this.measureNormal = new measureNormal(info);
    }

    public void setMeasureScaled(StoredInfo info) {
        this.measureScaled = new measureScaled(info);
    }

    public void setMeasureShort(StoredInfo info) {
        this.measureShort = new measureShort(info);
    }

    public Integer getIOA() {
        return IOA;
    }

    public int getASdu() {
        return ASdu;
    }

}

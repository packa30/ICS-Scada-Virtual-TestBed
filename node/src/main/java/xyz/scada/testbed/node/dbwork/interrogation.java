package xyz.scada.testbed.node.dbwork;

import org.openmuc.j60870.TypeId;

public class interrogation {
    private final TypeId typeOf;
    private Integer value;

    interrogation(){
        this.typeOf = TypeId.C_IC_NA_1;
        this.value = 20;
    }

    public TypeId getTypeOf() {
        return typeOf;
    }

    public Integer getValue() {
        return value;
    }

    public void setValue(Integer value) {
        this.value = value;
    }
}
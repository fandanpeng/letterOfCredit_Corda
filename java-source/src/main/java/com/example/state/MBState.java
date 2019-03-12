package com.example.state;

import com.alibaba.fastjson.JSON;
import com.example.LCBaseStruct.LCBaseStruct;
import com.example.contract.MarineBillContract;
import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.CommandAndState;
import net.corda.core.contracts.OwnableState;
import net.corda.core.crypto.NullKeys;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.AnonymousParty;
import org.apache.log4j.Logger;

import java.time.Instant;
import java.util.List;

public class MBState implements OwnableState {
    public static Logger logger1 = Logger.getLogger(MBState.class);
    private LCBaseStruct.MarineBill marineBill;
    private AbstractParty owner;
    public MBState(){

    }
    public MBState(String jsonStrMarineBill, AbstractParty owner){
        this.marineBill = JSON.parseObject(jsonStrMarineBill, LCBaseStruct.MarineBill.class);
        this.owner = owner;
    }
    public MBState(LCBaseStruct.MarineBill marineBill, AbstractParty owner){
        this.marineBill = marineBill;
        this.owner = owner;
    }

    public MBState copy() {
        return new MBState(this.marineBill,this.owner);
    }

    public MBState withoutOwner() {
        return new MBState(this.marineBill, new AnonymousParty(NullKeys.NullPublicKey.INSTANCE));
    }

    public MBState changeOwner(AbstractParty newOwner){
        return  new MBState(this.marineBill,newOwner);
    }

    //@NotNull
    @Override
    //public CommandAndState withNewOwner(@NotNull AbstractParty newOwner) {
    public CommandAndState withNewOwner(AbstractParty newOwner) {
        return new CommandAndState(new MarineBillContract.Commands.Move(), this.changeOwner(newOwner));
    }

    public AbstractParty getOwner() {
        return owner;
    }

    public Instant getIssueDate() {
        return marineBill.date;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MBState state = (MBState) o;

        if (!owner.equals(state.owner)) return false;
        return  (this.marineBill.equals(state.marineBill));
    }

    @Override
    public int hashCode() {
        int result = owner != null ? owner.hashCode() : 0;
        result = 31 * result + (marineBill != null ? marineBill.hashCode() : 0);
        return result;
    }

    //@NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return ImmutableList.of(this.owner);
    }

    public LCBaseStruct.GoodsInfo getGoodsInfo(){
        return this.marineBill.goodsInfo;
    }
    public LCBaseStruct.TransportInfo getTransportInfo(){
        return this.marineBill.transportInfo;
    }


}

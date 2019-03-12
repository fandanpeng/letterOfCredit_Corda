package com.example.state;

//import com.esotericsoftware.kryo.NotNull;
import com.example.LCBaseStruct.LCBaseStruct;
import com.example.contract.CPContract;
import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.*;
import net.corda.core.crypto.NullKeys;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.AnonymousParty;
import net.corda.core.identity.CordaX500Name;
import net.corda.finance.contracts.CommercialPaper;

import java.time.Instant;
import java.util.Currency;
import java.util.List;

//public class State implements OwnableState {
public class CPState implements OwnableState {

    private PartyAndReference issuance;
    private AbstractParty owner;
    private Amount<Issued<Currency>> faceValue;
    private Instant maturityDate;

    public CPState() {
    }  // For serialization

    public CPState(PartyAndReference issuance, AbstractParty owner, Amount<Issued<Currency>> faceValue,
                 Instant maturityDate) {
        this.issuance = issuance;
        this.owner = owner;
        this.faceValue = faceValue;
        this.maturityDate = maturityDate;
    }

    public CPState copy() {
        return new CPState(this.issuance, this.owner, this.faceValue, this.maturityDate);
    }

    public CPState withoutOwner() {
        return new CPState(this.issuance, new AnonymousParty(NullKeys.NullPublicKey.INSTANCE), this.faceValue, this.maturityDate);
    }
    public CPState changeOwner(AbstractParty newOwner){
        return  new CPState(this.issuance,newOwner,this.faceValue,this.maturityDate);
    }

    //@NotNull
    @Override
    //public CommandAndState withNewOwner(@NotNull AbstractParty newOwner) {
    public CommandAndState withNewOwner( AbstractParty newOwner) {
        return new CommandAndState(new CommercialPaper.Commands.Move(), new CPState(this.issuance, newOwner, this.faceValue, this.maturityDate));
    }

    public  PartyAndReference getIssuance() {
        return issuance;
    }

    public AbstractParty getOwner() {
        return owner;
    }

    public AbstractParty getIssuer() {
        return issuance.getParty();
    }

    public Amount<Issued<Currency>> getFaceValue() {
        return faceValue;
    }
    public Amount<Currency> getFaceValue1(){
        return new Amount<Currency>(faceValue.getQuantity(),faceValue.getToken().getProduct());
    }

    public Instant getMaturityDate() {
        return maturityDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CPState state = (CPState) o;

        if (issuance != null ? !issuance.equals(state.issuance) : state.issuance != null) return false;
        if (owner != null ? !owner.equals(state.owner) : state.owner != null) return false;
        if (faceValue != null ? !faceValue.equals(state.faceValue) : state.faceValue != null) return false;
        return !(maturityDate != null ? !maturityDate.equals(state.maturityDate) : state.maturityDate != null);
    }

    @Override
    public int hashCode() {
        int result = issuance != null ? issuance.hashCode() : 0;
        result = 31 * result + (owner != null ? owner.hashCode() : 0);
        result = 31 * result + (faceValue != null ? faceValue.hashCode() : 0);
        result = 31 * result + (maturityDate != null ? maturityDate.hashCode() : 0);
        return result;
    }

    //@NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return ImmutableList.of(this.owner);
    }
}

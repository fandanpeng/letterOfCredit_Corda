package com.example.state;
import com.alibaba.fastjson.JSON;
import com.example.LCBaseStruct.LCBaseStruct;

import com.example.contract.LCContract;
import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.*;
import net.corda.core.crypto.NullKeys;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.AnonymousParty;
import net.corda.core.identity.Party;
import org.apache.log4j.Logger;


import java.time.Instant;
import java.util.Currency;
import java.util.List;
public class LCState implements OwnableState {

    public static Logger logger1 = Logger.getLogger(LCState.class);
    private final LCBaseStruct.LCBaseInfo lcBaseInfo;
    private final AbstractParty issueBank;
    private final AbstractParty applicant;
    private final AbstractParty adviseBank;
    private final AbstractParty beneficiary;
    private final AbstractParty owner;


    public LCState(LCBaseStruct.LCBaseInfo lcInfo, AbstractParty issueBank,
                   AbstractParty applicant, AbstractParty adviseBank, AbstractParty beneficiary, AbstractParty owner){
        this.lcBaseInfo = lcInfo;
        this.issueBank = issueBank;
        this.applicant = applicant;
        this.adviseBank = adviseBank;
        this.beneficiary = beneficiary;
        this.owner = owner;
    }

    public LCState copy() {
        return new LCState(this.lcBaseInfo,this.issueBank,this.applicant, this.adviseBank, this.beneficiary, this.owner);
    }

    public LCState withoutOwner() {
        return new LCState(this.lcBaseInfo,this.issueBank,this.applicant, this.adviseBank, this.beneficiary, new AnonymousParty(NullKeys.NullPublicKey.INSTANCE));
    }

    public LCState changeOwner(AbstractParty newOwner){
        return  new LCState(this.lcBaseInfo,this.issueBank,this.applicant, this.adviseBank, this.beneficiary,newOwner);
    }

    //@NotNull
    @Override
    //public CommandAndState withNewOwner(@NotNull AbstractParty newOwner) {
    public CommandAndState withNewOwner( AbstractParty newOwner) {
        return new CommandAndState(new LCContract.Commands.Move(), this.changeOwner(newOwner));
    }

    public AbstractParty getOwner() {
        return owner;
    }

    public AbstractParty getIssuer() {
        return issueBank;
    }
    public AbstractParty getBenificiary() {

        return beneficiary;
    }
    public AbstractParty getApplicant() {

        return applicant;
    }
    public AbstractParty getAdviseBank() {

        return adviseBank;
    }
    public Amount<Issued<Currency>> getInvoiceValue() {
        return lcBaseInfo.invoiceAmount;
    }




    public Instant getIssueDate() {
        return lcBaseInfo.date;
    }
    public Instant getNotLaterDate() {
        return lcBaseInfo.notLaterDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LCState state = (LCState) o;

        if (!owner.equals(state.owner)) return false;
        return  (lcBaseInfo.equals(state.lcBaseInfo));
    }

    @Override
    public int hashCode() {
        int result = owner != null ? owner.hashCode() : 0;
        result = 31 * result + (lcBaseInfo != null ? lcBaseInfo.hashCode() : 0);
        return result;
    }

    //@NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return ImmutableList.of(this.getIssuer(),this.getApplicant(),this.getBenificiary(),this.getAdviseBank());
    }

    // 申请手续费 0.15%
    public boolean checkServiceChargeApply( Amount<Issued<Currency>> received){
        if(this.lcBaseInfo.invoiceAmount.getToken().getProduct()!= received.getToken().getProduct()){
            logger1.info(String.format("Currency not same, required: %s, received:%s ",
                    this.lcBaseInfo.invoiceAmount.getToken().getProduct(),received.getToken().getProduct() ));
            return false;
        }
        if(this.lcBaseInfo.invoiceAmount.getDisplayTokenSize()!= received.getDisplayTokenSize()){
            logger1.info(String.format("DisplayTokenSize not same, required: %s, received:%s ",
                    this.lcBaseInfo.invoiceAmount.getDisplayTokenSize(),received.getDisplayTokenSize()));
            return false;
        }
        if(this.lcBaseInfo.invoiceAmount.getQuantity()*0.0015 != received.getQuantity()){
            logger1.info(String.format("Charge not right, required: %s, received:%s ",
                    this.lcBaseInfo.invoiceAmount.getQuantity()*0.0015,received.getQuantity()));
            return false;
        }
        return true;
    }
    // 议付手续费0.1%
    public boolean checkServiceChargeAcceptance( Amount<Issued<Currency>> received){
        if(this.lcBaseInfo.invoiceAmount.getToken().getProduct()!= received.getToken().getProduct()){
            logger1.info(String.format("Currency not same, required: %s, received:%s ",
                    this.lcBaseInfo.invoiceAmount.getToken().getProduct(),received.getToken().getProduct() ));
            return false;
        }
        if(this.lcBaseInfo.invoiceAmount.getDisplayTokenSize()!= received.getDisplayTokenSize()){
            logger1.info(String.format("DisplayTokenSize not same, required: %s, received:%s ",
                    this.lcBaseInfo.invoiceAmount.getDisplayTokenSize(),received.getDisplayTokenSize()));
            return false;
        }
        if(this.lcBaseInfo.invoiceAmount.getQuantity()*(1-0.001) != received.getQuantity()){
            logger1.info(String.format("Charge not right, required: %s, received:%s ",
                    this.lcBaseInfo.invoiceAmount.getQuantity()*(1-0.001),received.getQuantity()));
            return false;
        }
        return true;
    }


    public boolean checkLCState(){
        logger1.info("Start CheckLCBaseInfo");
        if(!this.lcBaseInfo.CheckLCBaseInfo()){
            logger1.info("CheckLCBaseInfo fail");
            return false;
        }
        if(this.owner == null ){
            logger1.info("owner == null");
            return false;
        }
        return true;
    }

    public boolean checkMarineBill(MBState MBState){

        logger1.info("Start checkMarineBill");
       if( !this.lcBaseInfo.goodsInfo.equals(MBState.getGoodsInfo())){
            logger1.info("MarineBill goodsInfo not same  ");
            return false;
        }
        if( !this.lcBaseInfo.transportReq.equals(MBState.getTransportInfo())){
            logger1.info("MarineBill TransportInfo not same  ");
            return false;
        }
        return true;
    }



}

package com.example.state;
import com.alibaba.fastjson.JSON;

import com.example.contract.LCContract;
import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.*;
import net.corda.core.crypto.NullKeys;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.AnonymousParty;
import net.corda.core.identity.Party;
import net.corda.core.serialization.CordaSerializable;
import org.apache.log4j.Logger;


import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.List;
public class LCState implements OwnableState {

    public static Logger logger1 = Logger.getLogger(LCState.class);
    private  LCBaseInfo lcBaseInfo;
    private final AbstractParty issueBank;
    private final AbstractParty applicant;
    private final AbstractParty adviseBank;
    private final AbstractParty beneficiary;
    private final AbstractParty owner;


    public LCState(LCBaseInfo lcBaseInfo, AbstractParty issueBank,
                   AbstractParty applicant, AbstractParty adviseBank, AbstractParty beneficiary, AbstractParty owner){
        this.lcBaseInfo = lcBaseInfo;
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
    public LCBaseInfo getLcBaseInfo(){
        return this.lcBaseInfo;
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

    // Apply LC service charge 0.15%
    public Amount<Currency> getApplySerCharge(){
        Currency currency =  this.lcBaseInfo.invoiceAmount.getToken().getProduct();
        double serviceCharge = this.lcBaseInfo.invoiceAmount.getQuantity()*0.0015;
        BigDecimal bigDecimal =  this.lcBaseInfo.invoiceAmount.getDisplayTokenSize();
        return  new Amount<>(Math.round(serviceCharge),bigDecimal,currency);
    }
    public boolean checkServiceChargeApply( Amount<Issued<Currency>> received){
        try {
            logger1.info("Start checkServiceChargeApply");
            if (this.lcBaseInfo.invoiceAmount.getToken().getProduct() != received.getToken().getProduct()) {
                logger1.info(String.format("Currency not same, required: %s, received:%s ",
                        this.lcBaseInfo.invoiceAmount.getToken().getProduct(), received.getToken().getProduct()));
                return false;
            }
            if (this.lcBaseInfo.invoiceAmount.getDisplayTokenSize() != received.getDisplayTokenSize()) {
                logger1.info(String.format("DisplayTokenSize not same, required: %s, received:%s ",
                        this.lcBaseInfo.invoiceAmount.getDisplayTokenSize(), received.getDisplayTokenSize()));
                return false;
            }
            if (Math.round(this.lcBaseInfo.invoiceAmount.getQuantity() * 0.0015) != received.getQuantity()) {
                logger1.info(String.format("Charge not right, required: %s, received:%s ",
                        this.lcBaseInfo.invoiceAmount.getQuantity() * 0.0015, received.getQuantity()));
                return false;
            }
            return true;
        }
        catch (Exception ex) {
            logger1.info(ex.getMessage());
            return false;
        }

    }
    //  Acceptance service charge 0.1%
    public Amount<Currency> getAcceptSerCharge(){
        Currency currency =  this.lcBaseInfo.invoiceAmount.getToken().getProduct();
        double serviceCharge = this.lcBaseInfo.invoiceAmount.getQuantity()*(1-0.001);
        BigDecimal bigDecimal =  this.lcBaseInfo.invoiceAmount.getDisplayTokenSize();
        return  new Amount<>(Math.round(serviceCharge),bigDecimal,currency);
    }

    public boolean checkServiceChargeAcceptance( Amount<Issued<Currency>> pay){
        try {
            logger1.info("Start checkServiceChargeAcceptance");
            if (this.lcBaseInfo.invoiceAmount.getToken().getProduct() != pay.getToken().getProduct()) {
                logger1.info(String.format("Currency not same, required: %s, received:%s ",
                        this.lcBaseInfo.invoiceAmount.getToken().getProduct(), pay.getToken().getProduct()));
                return false;
            }
            if (this.lcBaseInfo.invoiceAmount.getDisplayTokenSize() != pay.getDisplayTokenSize()) {
                logger1.info(String.format("DisplayTokenSize not same, required: %s, received:%s ",
                        this.lcBaseInfo.invoiceAmount.getDisplayTokenSize(), pay.getDisplayTokenSize()));
                return false;
            }
            if (Math.round(this.lcBaseInfo.invoiceAmount.getQuantity() * (1 - 0.001)) != pay.getQuantity()) {
                logger1.info(String.format("Charge not right, required: %s, received:%s ",
                        this.lcBaseInfo.invoiceAmount.getQuantity() * (1 - 0.001), pay.getQuantity()));
                return false;
            }
            return true;
        }
        catch (Exception ex){
            logger1.info(ex.getMessage());
            return false;
        }
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

    @CordaSerializable
    public static class LCBaseInfo {
        public Amount<Issued<Currency>> invoiceAmount;
        public Instant date;
        public String  loadingOn;
        public Instant notLaterDate;
        public String  transportTo;

        public GoodsInfo goodsInfo;
        public TransportRequired transportReq;
        // public DocRequired docRequired;
        public LCBaseInfo() {
            this.goodsInfo = new GoodsInfo();
            this.transportReq = new TransportRequired();
        }
        public LCBaseInfo(Amount<Issued<Currency>> invoiceAmount,Instant date,String  loadingOn,
                          Instant notLaterDate,String transportTo, GoodsInfo goodsInfo,TransportRequired transportReq){
            this.invoiceAmount = invoiceAmount;
            this.date = date;
            this.loadingOn = loadingOn;
            this.notLaterDate = notLaterDate;
            this.transportTo = transportTo;
            this.goodsInfo = goodsInfo;
            this.transportReq = transportReq;
        }

        public LCBaseInfo copy(){
            return new LCBaseInfo(this.invoiceAmount,this.date,this.loadingOn,this.notLaterDate,this.transportTo,this.goodsInfo,this.transportReq);
        }

        public boolean CheckLCBaseInfo() {
            try {
                logger1.info("Start CheckLCBaseInfo");
                if (this.invoiceAmount == null) {
                    logger1.info("invoiceAmount == null");
                    return false;
                } else {
                    if (this.invoiceAmount.getQuantity() <= 0) {
                        logger1.info("invoiceAmount <=0");
                        return false;
                    }
                }


                //if(this.date.isBefore(Instant.now().minusSeconds(100))){
                //   logger1.info("date before now");
                //   return false;
                //}
                if (this.loadingOn == null) {
                    logger1.info("loadingOn == null");
                    return false;
                }
                if (this.notLaterDate.isBefore(Instant.now())) {
                    logger1.info("notLaterDate before now");
                    return false;
                }
                if (this.transportTo == null) {
                    logger1.info("transportTo == null");
                    return false;
                }
                if (!this.goodsInfo.CheckGoodsInfo()) {
                    logger1.info(" CheckGoodsInfo fail");
                    return false;
                }
                if (!this.transportReq.CheckTransRequir()) {
                    logger1.info("transportReq CheckTransRequir fail");
                    return false;
                }
                return true;
            }
            catch (Exception ex)
            {
                logger1.info(ex.getMessage());
                return false;
            }
        }


    }
    @CordaSerializable
    public static class LCBaseInfoWithParticipant extends LCBaseInfo{
        public String issueBankX509Name;
        public String applicantX509Name;
        public String adviseBankX509Name;
        public String beneficiaryX509Name;
        public boolean CheckLCBaseInfoWithParticipant(boolean includeAdviseBankName) {
            try{
                logger1.info("Start CheckLCBaseInfoWithParticipant");
                if(this.invoiceAmount.getQuantity()<=0){
                    logger1.info("invoiceAmount <=0");
                    return false;
                }
                if(this.issueBankX509Name == null) {
                    logger1.info("issueBank == null");
                    return false;
                }

                if(this.applicantX509Name == null) {
                    logger1.info("applyCompany == null");
                    return false;
                }

                if(includeAdviseBankName){
                    if(this.adviseBankX509Name == null) {
                        logger1.info("adviseBank == null");
                        return false;
                    }
                }
                if(this.beneficiaryX509Name == null) {
                    logger1.info("beneficiary == null");
                    return false;
                }

                if(!this.CheckLCBaseInfo()){
                    logger1.info("CheckLCBaseInfo fail");
                    return false;
                }

                return true;
            }
            catch (Exception ex)
            {
                logger1.info(ex.getMessage());
                return false;
            }

        }

    }
    @CordaSerializable
    public static class GoodsInfo{
        public String goodsName;
        public String googdsQuality;
        public String googdsSPEC;
        public int goodsQuantity;
        public Amount<Currency> goodsPrice;

        public GoodsInfo(String goodsName, String googdsQuality,String googdsSPEC, int goodsQuantity, Amount<Currency> goodsPrice){
            this.goodsName = goodsName;
            this.googdsQuality = googdsQuality;
            this.googdsSPEC = googdsSPEC;
            this.goodsQuantity = goodsQuantity;
            this.goodsPrice = goodsPrice;
        }
        public GoodsInfo(){

        }
        boolean CheckGoodsInfo()
        {
            logger1.info("Start CheckGoodsInfo");
            if(this.goodsName == null) {
                logger1.info("goodsName == null");
                return false;
            }
            if(this.googdsQuality == null) {
                logger1.info("googdsQuality == null");
                return false;
            }
            if(this.googdsSPEC == null) {
                logger1.info("googdsSPEC == null");
                return false;
            }
            if(this.goodsQuantity <= 0){
                logger1.info("goodsQuantity <= 0");
                return false;
            }
            if(this.goodsPrice == null){
                logger1.info("goodsPrice == null");
                return false;
            }
            return true;
        }

        @Override
        public boolean equals( Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            GoodsInfo gInfo = (GoodsInfo)o;
            if( this.goodsName == gInfo.goodsName
                    && this.googdsQuality == gInfo.googdsQuality
                    && this.googdsSPEC == gInfo.googdsSPEC
                    && this.goodsQuantity == goodsQuantity
                    && this.goodsPrice.equals(gInfo.goodsPrice))
            {
                return true;
            }
            else{
                return false;
            }

        }

    }
    @CordaSerializable
    public static class TransportRequired{
        public Instant transportInDate;
        public TransportInfo transPortInfo;
        public TransportRequired(Instant transPortInDate, TransportInfo transportInfo){
            this.transportInDate = transPortInDate;
            this.transPortInfo = transportInfo;
        }
        public TransportRequired(){

        }
        public boolean CheckTransRequir(){
            logger1.info("Start CheckTransRequir");
            // if(transportInDate.isBefore()){
            //     logger1.info("transportInDate before now");
            //     return false;
            //}
            if(!transPortInfo.CheckTransportInfo()){
                logger1.info("transportInfo check fail");
                return false;
            }
            return true;
        }


    }
    @CordaSerializable
    public static class TransportInfo{
        public String portOfShippment;
        public String portOfDestination;
        public Amount<Currency> fare;
        public String farePayLocation;
        public TransportInfo(String portOfShippment,String portOfDestination,  Amount<Currency> fare,String farePayLocation){
            this.portOfShippment =portOfShippment;
            this.portOfDestination = portOfDestination;
            this.fare = fare;
            this.farePayLocation = farePayLocation;
        }
        public boolean CheckTransportInfo(){
            logger1.info("Start CheckTransportInfo");
            if(this.portOfShippment == null) {
                logger1.info("portOfShippment == null");
                return false;
            }
            if(this.portOfDestination == null) {
                logger1.info("portOfDestination == null");
                return false;
            }
            if(this.fare.getQuantity() < 0) {
                logger1.info("fare < 0");
                return false;
            }
            if(this.farePayLocation == null) {
                logger1.info("farePayLocation == null");
                return false;
            }
            return true;
        }
        @Override
        public boolean equals( Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TransportInfo tInfo = (TransportInfo)o;
            if( this.portOfShippment == tInfo.portOfShippment
                    && this.portOfDestination == tInfo.portOfDestination
                    && this.farePayLocation == tInfo.farePayLocation
                    && this.fare.equals(tInfo.fare))
            {
                return true;
            }
            else
                return false;
        }

    }

    public static class DocRequired{

    }
    @CordaSerializable
    public static class MarineBill{
        public GoodsInfo goodsInfo;
        public TransportInfo transportInfo;
        public Instant date;
        public MarineBill(GoodsInfo goodsInfo,TransportInfo transportInfo,  Instant date){
            this.goodsInfo = goodsInfo;
            this.transportInfo = transportInfo;
            this.date = date;
        }
        public boolean CheckMarineBill(){
            logger1.info("Start CheckMarineBill");
            if(!goodsInfo.CheckGoodsInfo()){
                logger1.info("goodsInfo check fail");
                return false;
            }
            if(!transportInfo.CheckTransportInfo()){
                logger1.info("transportInfo check fail");
                return false;
            }
            if(this.date.isBefore(Instant.now())){
                logger1.info("MarineBill Date check fail");
                return false;
            }
            return true;
        }
    }

}

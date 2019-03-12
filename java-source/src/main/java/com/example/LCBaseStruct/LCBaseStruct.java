package com.example.LCBaseStruct;

import net.corda.core.contracts.Amount;
import net.corda.core.contracts.Issued;
import net.corda.core.identity.Party;
import net.corda.core.serialization.CordaSerializable;
import org.apache.log4j.Logger;

import java.io.Serializable;
import java.time.Instant;
import java.util.Currency;

public class LCBaseStruct {
    public static Logger logger1 = Logger.getLogger(LCBaseStruct.class);
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

        }
        public boolean CheckLCBaseInfo() {
            logger1.info("Start CheckLCBaseInfo");
            if(this.invoiceAmount.getQuantity()<=0){
                logger1.info("invoiceAmount <=0");
                return false;
            }

            //if(this.date.isBefore(Instant.now().minusSeconds(100))){
             //   logger1.info("date before now");
             //   return false;
            //}
            if(this.loadingOn == null) {
                logger1.info("loadingOn == null");
                return false;
            }
            if(this.notLaterDate.isBefore(Instant.now())){
                logger1.info("notLaterDate before now");
                return false;
            }
            if(this.transportTo == null) {
                logger1.info("transportTo == null");
                return false;
            }
            if(!this.goodsInfo.CheckGoodsInfo()){
                logger1.info(" CheckGoodsInfo fail");
                return false;
            }
            if(!this.transportReq.CheckTransRequir()){
                logger1.info("transportReq CheckTransRequir fail");
                return false;
            }
            return true;
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

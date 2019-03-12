package com.example.LCBaseStruct;

import net.corda.core.contracts.Amount;
import net.corda.core.contracts.Issued;
import net.corda.core.identity.Party;

import java.time.Instant;
import java.util.Currency;

public class LCBaseStruct {
    public static class LCBaseInfo{
        public Amount<Issued<Currency>> invoiceAmount;
        public Party issueBank;
        public Party applyCompany;
        public Party adviseBank;
        public Party benefitiaryCompany;

        public Instant date;
        public String  loadingOn;
        public Instant notLaterDate;
        public String  transportTo;

        public GoodsInfo goodsInfo;
        public TransportRequired transportReq;
       // public DocRequired docRequired;

    }

    public static class GoodsInfo{
        public String goodsName;
        public String googdsQuality;
        public String googdsSPEC;
        public int goodsQuantity;
        public String goodsPrice;

    }
    public static class TransportRequired{
        public Instant transportInDate;
        public TransportInfo transPortInfo;

    }
    public static class TransportInfo{
        public String portOfShippment;
        public String portOfDestination;
        public Amount<Currency> fare;
        public String farePayLocation;

    }

    public static class DocRequired{

    }

    public static class MarineBill{
        GoodsInfo goodsInfo;
        TransportInfo transportInfo;
        Instant Date;
    }



}

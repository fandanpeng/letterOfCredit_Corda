package com.example.contract;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import org.apache.log4j.Logger;

public class MarineBillContract implements Contract {
    public static final String MB_CONTRACT_ID = "com.example.contract.MarineBillContract";
    public static Logger logger1 = Logger.getLogger(LCContract.class);
    @Override
    public void verify(LedgerTransaction tx) {
        try {
            logger1.info("Start MarineBill contract verify");
        }
        catch (Exception ex){
            logger1.info(ex.getMessage());
        }
    }

    public static class Commands implements CommandData {

        public static class Issue extends Commands {
            @Override
            public boolean equals(Object obj) {
                return obj instanceof Issue;
            }
        }

        public static class Move extends Commands {
            @Override
            public boolean equals(Object obj) {
                return obj instanceof Move;
            }
        }


        public static class Redeem extends Commands {
            @Override
            public boolean equals(Object obj) {
                return obj instanceof Redeem;
            }
        }

    }
}

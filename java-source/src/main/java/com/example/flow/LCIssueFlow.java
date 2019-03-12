package com.example.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.alibaba.fastjson.JSON;
import com.example.LCBaseStruct.LCBaseStruct;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.Issued;
import net.corda.core.flows.*;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.utilities.OpaqueBytes;
import net.corda.core.utilities.ProgressTracker;
import net.corda.core.utilities.UntrustworthyData;
import org.apache.log4j.Logger;

import java.time.Instant;
import java.util.Currency;

import static net.corda.core.contracts.ContractsDSL.requireThat;

@InitiatingFlow
@StartableByRPC
public class LCIssueFlow {

    public static Logger logger1 = Logger.getLogger(LCIssueFlow.class);

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {

        private  LCBaseStruct.LCBaseInfoWithParticipant applyLCBaseInfoWithPartis;

        public Initiator(String jsonStr){
            logger1.info(jsonStr);
            try{
                logger1.info(jsonStr);
                this.applyLCBaseInfoWithPartis = new LCBaseStruct.LCBaseInfoWithParticipant();


                //this.applyLCBaseInfoWithPartis = JSON.parseObject(jsonStr, LCBaseStruct.LCBaseInfoWithParticipant.class);
                logger1.info("parseJson success ");
            }
            catch (Exception ex)
            {
                logger1.info("Initiator constructor exception" + ex.getMessage());
                throw ex;
            }

        }
        private final ProgressTracker.Step PREPARING = new ProgressTracker.Step("Gathering the StateAndRef");
        private final ProgressTracker.Step SEND_CPSTATE = new ProgressTracker.Step("Send CPState to issuer.");
        private final ProgressTracker.Step RETURNING = new ProgressTracker.Step("Returning the newly-issued cash state.");
        private final ProgressTracker progressTracker = new ProgressTracker(
                PREPARING,
                SEND_CPSTATE,
                RETURNING
        );
        @Override
        public ProgressTracker getProgressTracker() {
            return progressTracker;
        }


        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            try{
                logger1.info("Start LCIssueFlow Initiator call");

                //test
                LCBaseStruct.LCBaseInfoWithParticipant testLC = new LCBaseStruct.LCBaseInfoWithParticipant();
                testLC.applicantX509Name = getOurIdentity().getName().toString();
                logger1.info("applicantX509Name: "+ testLC.applicantX509Name);
                testLC.issueBankX509Name = "O=PartyB, L=New York, C=US";
                testLC.beneficiaryX509Name = "O=PartyD, L=New York, C=US";


                Byte a = 0;
                OpaqueBytes opaqueBytes =  OpaqueBytes.of(a);
                Issued<Currency> iCurrency = new Issued<>(getOurIdentity().ref(opaqueBytes), Currency.getInstance("USD"));
                testLC.invoiceAmount = new Amount<>(10000 * 100, iCurrency);
                testLC.date = Instant.now();
                testLC.loadingOn = "Xi'an";
                testLC.notLaterDate = Instant.now().plusSeconds(30*60*60*24);
                testLC.transportTo = "Argentina";
                Amount<Currency> shipfare = new Amount<Currency>(100, Currency.getInstance("USD"));
                LCBaseStruct.TransportInfo transportInfo = new  LCBaseStruct.TransportInfo("Xi'an", "Argentina",shipfare,"Xi'an");
                LCBaseStruct.TransportRequired transportRequired = new LCBaseStruct.TransportRequired(testLC.date,transportInfo);
                testLC.transportReq = transportRequired;
                Amount<Currency> goodsPrice = new Amount<Currency>(20, Currency.getInstance("USD"));
                LCBaseStruct.GoodsInfo goodsInfo = new LCBaseStruct.GoodsInfo("Apple","highest","Box",10000,goodsPrice);
                testLC.goodsInfo = goodsInfo;
                logger1.info("assemble lcbaseInfo pass");
                String strjson = JSON.toJSONString(testLC);
                logger1.info(strjson);
                this.applyLCBaseInfoWithPartis = testLC;
                //test end
                logger1.info("requirethat");
                boolean checkRet = this.applyLCBaseInfoWithPartis.CheckLCBaseInfoWithParticipant(false);
                requireThat(require -> {
                    require.using("Should pass CheckLCBaseInfoWithParticipant", checkRet);
                    return null;
                });
                logger1.info("requireThat pass");

                //CordaX500Name issueBankName = CordaX500Name.parse("O=PartyB, L=New York, C=US");
                CordaX500Name issueBankName = CordaX500Name.parse(this.applyLCBaseInfoWithPartis.issueBankX509Name);
                Party issueBank = getServiceHub().getNetworkMapCache().getPeerByLegalName(issueBankName);
                logger1.info(issueBank.getName());
                FlowSession sessionWithIsuuer = initiateFlow(issueBank);
                logger1.info("sessionWithIsuuer: " + sessionWithIsuuer.toString());


                //UntrustworthyData<LCBaseStruct.LCBaseInfoWithParticipant> replyWrapLCInfo = sessionWithIsuuer.sendAndReceive(LCBaseStruct.LCBaseInfoWithParticipant.class,this.applyLCBaseInfoWithPartis);
                sessionWithIsuuer.send(this.applyLCBaseInfoWithPartis,true);
                logger1.info("send success");
                UntrustworthyData<String> adviseBankNameWrap =sessionWithIsuuer.receive(String.class,true);
                logger1.info(adviseBankNameWrap.toString());
                String adviseBankName = adviseBankNameWrap.unwrap(ad->ad);
                logger1.info(" Received adviseBankName: " + adviseBankName);
                this.applyLCBaseInfoWithPartis.adviseBankX509Name = adviseBankName;

                logger1.info("replyFullLCInfo: " + JSON.toJSONString(this.applyLCBaseInfoWithPartis) );

                return null;
            }
            catch (Exception ex)
            {
                logger1.info("Initiator call: " + ex.getMessage());
                throw ex;
            }
        }
    }

    @StartableByRPC
    @InitiatedBy(LCIssueFlow.Initiator.class)
    public static class Responser extends FlowLogic<SignedTransaction> {
        private final FlowSession otherPartySession;

        public Responser(FlowSession otherPartySession) {
            logger1.info("Responser constructor");
            logger1.info(otherPartySession.toString());
            this.otherPartySession = otherPartySession;
        }
        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {
            try{
                logger1.info("Start LCIssueFlow Responser call");

                UntrustworthyData<LCBaseStruct.LCBaseInfoWithParticipant> aa = otherPartySession.receive(LCBaseStruct.LCBaseInfoWithParticipant.class,true);
                LCBaseStruct.LCBaseInfoWithParticipant applyLCInfoWithPartis = aa.unwrap(( LCBaseStruct.LCBaseInfoWithParticipant tt)->tt);
                logger1.info("receive and unwrap success");

                //CordaX500Name adviceBankName = CordaX500Name.parse("O=PartyC, L=Paris,C=FR");
                //Party adviceBank = getServiceHub().getNetworkMapCache().getPeerByLegalName(adviceBankName);
                applyLCInfoWithPartis.adviseBankX509Name = "O=PartyC, L=Paris,C=FR";

                logger1.info( "response LC info with adviseBank: "+ JSON.toJSONString(applyLCInfoWithPartis));

                otherPartySession.send(applyLCInfoWithPartis);
                return null;
            }
            catch (Exception ex)
            {
                logger1.info("Responser call: " + ex.getMessage());
                throw ex;
            }
        }
    }

}

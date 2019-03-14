package com.example.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.alibaba.fastjson.JSON;
import com.example.state.LCState;
import com.example.contract.LCContract;
import com.example.state.LCState;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.Issued;
import net.corda.core.contracts.TimeWindow;
import net.corda.core.flows.*;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.OpaqueBytes;
import net.corda.core.utilities.ProgressTracker;
import net.corda.core.utilities.UntrustworthyData;
import net.corda.finance.contracts.asset.Cash;
import org.apache.log4j.Logger;

import java.security.PublicKey;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalUnit;
import java.util.Currency;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.example.contract.LCContract.LC_CONTRACT_ID;
import static net.corda.core.contracts.ContractsDSL.requireThat;

@InitiatingFlow
@StartableByRPC
public class LCIssueFlow {

    public static Logger logger1 = Logger.getLogger(LCIssueFlow.class);

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction> {

        private  LCState.LCBaseInfoWithParticipant applyLCBaseInfoWithPartis;

        public Initiator(String jsonStr){
            logger1.info(jsonStr);
            try{
                logger1.info(jsonStr);
                this.applyLCBaseInfoWithPartis = new LCState.LCBaseInfoWithParticipant();


                //this.applyLCBaseInfoWithPartis = JSON.parseObject(jsonStr, LCState.LCBaseInfoWithParticipant.class);
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
                LCState.LCBaseInfoWithParticipant testLC = new LCState.LCBaseInfoWithParticipant();
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
                LCState.TransportInfo transportInfo = new  LCState.TransportInfo("Xi'an", "Argentina",shipfare,"Xi'an");

                Instant transPortInDate =testLC.date.plusSeconds(7*24*60*60);
                LCState.TransportRequired transportRequired = new LCState.TransportRequired(transPortInDate,transportInfo);
                testLC.transportReq = transportRequired;
                Amount<Currency> goodsPrice = new Amount<Currency>(20, Currency.getInstance("USD"));
                LCState.GoodsInfo goodsInfo = new LCState.GoodsInfo("Apple","highest","Box",10000,goodsPrice);
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

                UntrustworthyData<LCState.LCBaseInfoWithParticipant> replyLCBaseInfowWrap = sessionWithIsuuer.sendAndReceive(LCState.LCBaseInfoWithParticipant.class,this.applyLCBaseInfoWithPartis,true);
                logger1.info(replyLCBaseInfowWrap.toString());
                // todo if unwrap the untrusted worth Data ,there will be a exception when start the flow
                //this.applyLCBaseInfoWithPartis.adviseBankX509Name = replyAdvisBankName.unwrap((String tt)->tt);
                this.applyLCBaseInfoWithPartis.adviseBankX509Name = "O=PartyC, L=Paris,C=FR";
                logger1.info("replyFullLCInfo: " + JSON.toJSONString(this.applyLCBaseInfoWithPartis) );

                // start define lcState


                CordaX500Name cordaX500Name = CordaX500Name.parse(this.applyLCBaseInfoWithPartis.applicantX509Name);
                Party appliciant = getServiceHub().getNetworkMapCache().getPeerByLegalName(cordaX500Name);
                cordaX500Name = CordaX500Name.parse(this.applyLCBaseInfoWithPartis.issueBankX509Name);
                Party issurer = getServiceHub().getNetworkMapCache().getPeerByLegalName(cordaX500Name);
                cordaX500Name = CordaX500Name.parse(this.applyLCBaseInfoWithPartis.adviseBankX509Name);
                Party advisebank = getServiceHub().getNetworkMapCache().getPeerByLegalName(cordaX500Name);
                cordaX500Name = CordaX500Name.parse(this.applyLCBaseInfoWithPartis.beneficiaryX509Name);
                Party beneficiary = getServiceHub().getNetworkMapCache().getPeerByLegalName(cordaX500Name);
                LCState.LCBaseInfo lcBaseInfo1 =  this.applyLCBaseInfoWithPartis;
                logger1.info("lcBaseInfo1: " + JSON.toJSONString(lcBaseInfo1) );

                LCState lcStateOutput = new LCState(lcBaseInfo1.copy(),appliciant,issueBank,advisebank,beneficiary,issueBank );
                logger1.info("lcStateOutput.LcBaseInfo: " + JSON.toJSONString(lcStateOutput.getLcBaseInfo()) );

                final Command<LCContract.Commands.Issue> txCommand = new Command<>(
                        new LCContract.Commands.Issue(),
                        ImmutableList.of(lcStateOutput.getApplicant().getOwningKey(),lcStateOutput.getIssuer().getOwningKey(),lcStateOutput.getOwner().getOwningKey())
                );
                logger1.info("txCommand: "+ txCommand.toString());
                TimeWindow timeWindow = TimeWindow.withTolerance(getServiceHub().getClock().instant(), Duration.ofSeconds(100));
                logger1.info("timeWindow: " + timeWindow.toString());


                final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
                TransactionBuilder txBuilder = new TransactionBuilder(notary);
                txBuilder.addOutputState(lcStateOutput,LC_CONTRACT_ID);
                txBuilder.addCommand(txCommand);
                txBuilder.setTimeWindow(timeWindow);
                Set<Party> sets = new HashSet<>();
                sets.add(getOurIdentity());
                //sets.add((Party)lcStateOutput.getApplicant());
                List<PublicKey> cashSigningKeys = Cash.generateSpend(getServiceHub(), txBuilder,
                        lcStateOutput.getApplySerCharge(), getOurIdentityAndCert(),lcStateOutput.getIssuer(), sets).getSecond();
                logger1.info("Start verify ");
                txBuilder.verify(getServiceHub());
                logger1.info("verify pass");

                cashSigningKeys.add(getOurIdentity().getOwningKey());
                final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder,cashSigningKeys);
                logger1.info("signInitialTransaction pass");
                final SignedTransaction fullySignedTx = subFlow(
                        new CollectSignaturesFlow(partSignedTx, ImmutableSet.of(sessionWithIsuuer), CollectSignaturesFlow.Companion.tracker()));
                logger1.info("CollectSignaturesFlow pass");

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

                UntrustworthyData<LCState.LCBaseInfoWithParticipant> aa = otherPartySession.receive(LCState.LCBaseInfoWithParticipant.class,true);
                LCState.LCBaseInfoWithParticipant applyLCInfoWithPartis = aa.unwrap(( LCState.LCBaseInfoWithParticipant tt)->tt);
                //CordaX500Name adviceBankName = CordaX500Name.parse("O=PartyC, L=Paris,C=FR");
                //Party adviceBank = getServiceHub().getNetworkMapCache().getPeerByLegalName(adviceBankName);
                applyLCInfoWithPartis.adviseBankX509Name = "O=PartyC, L=Paris,C=FR";
                otherPartySession.send(applyLCInfoWithPartis);
                logger1.info("Send pass" );


                class SignTxFlow extends SignTransactionFlow {
                    private SignTxFlow(FlowSession otherPartyFlow, ProgressTracker progressTracker) {
                        super(otherPartyFlow, progressTracker);
                    }

                    @Override
                    @Suspendable
                    protected void checkTransaction(SignedTransaction stx) {
                        //Amount<Issued<Currency>> received = sumCashBy(stx.getInputs(), input.getOwner());
                        //stx.verify(getServiceHub(),false);
                        requireThat(require -> {
                            //require.using("the received amount equals the face value", received == input.getFaceValue());
                            return null;
                        });
                    }
                }

                logger1.info("new SignTransactionFlow success");
                SignedTransaction signedTx = subFlow(new SignTxFlow(otherPartySession, SignTransactionFlow.Companion.tracker()));


                logger1.info("run SignTransactionFlow success");
                return waitForLedgerCommit(signedTx.getId());
            }
            catch (Exception ex)
            {
                logger1.info("Responser call: " + ex.getMessage());
                throw ex;
            }
        }
    }

}

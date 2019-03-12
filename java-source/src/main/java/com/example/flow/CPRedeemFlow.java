package com.example.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.example.contract.CPContract;
import com.example.state.CPState;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.corda.core.contracts.*;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import net.corda.finance.contracts.asset.Cash;
import org.apache.log4j.Logger;

import java.time.Duration;
import java.util.*;

import static net.corda.core.contracts.ContractsDSL.requireThat;
import static net.corda.finance.utils.StateSumming.sumCashBy;

@StartableByRPC
@InitiatingFlow
public class CPRedeemFlow {

    public static Logger logger1 = Logger.getLogger(CPRedeemFlow.class);
    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction>{
        private final String txHash;
        private final int index;
        //private final Party issuer;

        public Initiator(String txHash, int index, Party issuer)
        {
            logger1.info("Initiator constructor");
            this.txHash = txHash;
            this.index = index;
           // this.issuer = issuer;
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
            try {
                logger1.info("Start CPRedeemFlow Initiator call");
                progressTracker.setCurrentStep(PREPARING);
                StateRef stateRef = new StateRef(SecureHash.parse(this.txHash), 0);
                StateAndRef<CPState> stateAndRef = getServiceHub().toStateAndRef(stateRef);
                CPState inputCPState = stateAndRef.getState().getData();
                logger1.info("inputCPState: " + inputCPState.toString());
                requireThat(require -> {
                    require.using("CP redeem can only be initiated by the current owner", getOurIdentity().equals(inputCPState.getOwner()));
                    return null;
                });

                List<StateAndRef<CPState>> lstStateAndRef = new ArrayList<>();
                lstStateAndRef.add(stateAndRef);
                logger1.info(lstStateAndRef.toString());

                progressTracker.setCurrentStep(SEND_CPSTATE);
               // FlowSession issuerPartySession = initiateFlow(this.issuer);
                FlowSession issuerPartySession = initiateFlow((Party) inputCPState.getIssuer());
                logger1.info("initiateFlow success");
                logger1.info(issuerPartySession.toString());
                subFlow(new SendStateAndRefFlow(issuerPartySession, lstStateAndRef));
                logger1.info("SendStateAndRefFlow pass");



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
                SignedTransaction signedTx = subFlow(new SignTxFlow(issuerPartySession, SignTransactionFlow.Companion.tracker()));


                logger1.info("run SignTransactionFlow success");
                logger1.info(signedTx.toString());
                return waitForLedgerCommit(signedTx.getId());
            }
            catch (Exception ex)
            {
                logger1.info("Initiator.Call exception: " + ex.getMessage());
                throw ex;
            }
        }
    }
    @StartableByRPC
    @InitiatedBy(Initiator.class)
    public static class Responser extends FlowLogic<SignedTransaction> {

        private final FlowSession otherPartyFlow;

        public Responser(FlowSession otherPartyFlow) {
            logger1.info("Responser constructor");
            logger1.info(otherPartyFlow.toString());
            this.otherPartyFlow = otherPartyFlow;
        }
        @Suspendable
        @Override
        public SignedTransaction call() throws FlowException {

            try {
                logger1.info("Start CPRedeemFlow Responser call");
                final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
                StateAndRef<CPState> stateAndRef = subFlow(new ReceiveStateAndRefFlow<CPState>(otherPartyFlow)).get(0);
                logger1.info("stateAndRef: " + stateAndRef.toString());
                CPState inputCP = stateAndRef.getState().getData();
                final Command<CPContract.Commands.Redeem> txCommand = new Command<>(
                        new CPContract.Commands.Redeem(),
                        ImmutableList.of(inputCP.getOwner().getOwningKey()));
                logger1.info("txCommand: "+ txCommand.toString());
                TimeWindow timeWindow = TimeWindow.withTolerance(getServiceHub().getClock().instant(), Duration.ofSeconds(100));
                logger1.info("timeWindow: " + timeWindow.toString());

                TransactionBuilder txBuilder = new TransactionBuilder(notary);
                txBuilder.addInputState(stateAndRef);
                txBuilder.addCommand(txCommand);
                txBuilder.setTimeWindow(timeWindow);

                Set<Party> sets = new HashSet<>();
                sets.add(getOurIdentity());
                List<java.security.PublicKey> cashSigningKeys = Cash.generateSpend(getServiceHub(), txBuilder, inputCP.getFaceValue1(), getOurIdentityAndCert(), inputCP.getOwner(), sets).getSecond();

                logger1.info("txBuilder after cash spend: "+ txBuilder.toString());

                txBuilder.verify(getServiceHub());
                logger1.info("verify pass");
                cashSigningKeys.add(getOurIdentity().getOwningKey());
                final SignedTransaction partSignedTx = getServiceHub().signInitialTransaction(txBuilder,cashSigningKeys);
                logger1.info("signInitialTransaction pass");

                final SignedTransaction fullySignedTx = subFlow(
                        new CollectSignaturesFlow(partSignedTx, ImmutableSet.of(otherPartyFlow), CollectSignaturesFlow.Companion.tracker()));
                logger1.info("CollectSignaturesFlow pass");
                logger1.info("fullySignedTx: " +fullySignedTx.toString() );
                return subFlow(new FinalityFlow(fullySignedTx));
            }
            catch (Exception ex) {
                logger1.info("Responser.Call exception: " + ex.getMessage());
                throw ex;
            }
        }

    }
}
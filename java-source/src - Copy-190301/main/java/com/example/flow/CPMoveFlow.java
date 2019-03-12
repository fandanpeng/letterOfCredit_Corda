package com.example.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.example.contract.CPContract;
import com.example.state.CPState;
import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.StateRef;
import net.corda.core.contracts.TimeWindow;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;
import org.apache.log4j.Logger;

import java.time.Duration;

import static com.example.contract.CPContract.CP_CONTRACT_ID;
import static net.corda.core.contracts.ContractsDSL.requireThat;

@InitiatingFlow
@StartableByRPC
public class CPMoveFlow  {

    public static Logger logger1 = Logger.getLogger(CPMoveFlow.class);

    @InitiatingFlow
    @StartableByRPC
    public static class Initiator extends FlowLogic<SignedTransaction>{
        private final String txHash;
        private final int index;
        private final Party newOwner;
        public Initiator(String txHash, int index, Party newOwner){
            this.txHash = txHash;
            this.index = index;
            this.newOwner = newOwner;
        }
        public  void logInfo(String info){
            //String nodeName = getOurIdentity().toString();
            //nodeName ="[" + nodeName + "]";
            //String logInfos = nodeName + info;
            logger1.info(info);
        }

        private final ProgressTracker.Step PREPARING = new ProgressTracker.Step("Gathering the required inputs");
        private final ProgressTracker.Step BUILD_INITIAL_TRANSACTION = new ProgressTracker.Step("Build initial Tx");
        private final ProgressTracker.Step VERIFYING_TRANSACTION = new ProgressTracker.Step("Verifying contract constraints");
        private final ProgressTracker.Step SIGNING_TRANSACTION = new ProgressTracker.Step("Signing transaction with our private key.");
        private final ProgressTracker.Step RETURNING = new ProgressTracker.Step("Returning the moved CPState.");
        private final ProgressTracker progressTracker = new ProgressTracker(
                PREPARING,
                BUILD_INITIAL_TRANSACTION,
                VERIFYING_TRANSACTION,
                SIGNING_TRANSACTION,
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
                logInfo("Start CPMoveFlow Initiator call");
                progressTracker.setCurrentStep(PREPARING);
                Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);

                StateRef inputStateRef = new StateRef(SecureHash.parse(this.txHash), this.index);
                StateAndRef<CPState> inputStateAndRef = getServiceHub().toStateAndRef(inputStateRef);
                CPState inputCPState = inputStateAndRef.getState().getData();
                requireThat(require -> {
                    require.using("CP move can only be initiated by the current owner", getOurIdentity().equals(inputCPState.getOwner()));
                    return null;
                });
                logInfo("inputCPState: " + inputCPState.toString());
                CPState outputCPState = inputCPState.changeOwner(this.newOwner);
                logInfo("outputCPState: " + inputCPState.toString());
                final Command<CPContract.Commands.Move> txCommand = new Command<>(new CPContract.Commands.Move(),
                        ImmutableList.of(inputCPState.getOwner().getOwningKey()));
                logInfo("txCommand: " + txCommand.toString());
                TimeWindow timeWindow = TimeWindow.withTolerance(getServiceHub().getClock().instant(), Duration.ofSeconds(100));
                logInfo("timeWindow: " + timeWindow.toString());

                progressTracker.setCurrentStep(BUILD_INITIAL_TRANSACTION);
                TransactionBuilder txBuilder = new TransactionBuilder(notary);
                txBuilder.addInputState(inputStateAndRef);
                txBuilder.addOutputState(outputCPState, CP_CONTRACT_ID);
                txBuilder.addCommand(txCommand);
                txBuilder.setTimeWindow(getServiceHub().getClock().instant(), Duration.ofSeconds(100));

                progressTracker.setCurrentStep(VERIFYING_TRANSACTION);
                txBuilder.verify(getServiceHub());
                logInfo("CPMoveFlow initiator verify pass");

                progressTracker.setCurrentStep(SIGNING_TRANSACTION);
                SignedTransaction signedTransaction = getServiceHub().signInitialTransaction(txBuilder);
                logInfo("Signed tx: " + signedTransaction.toString());

                progressTracker.setCurrentStep(RETURNING);
                return subFlow(new FinalityFlow(signedTransaction));
            }
            catch (Exception ex)
            {
                logInfo(ex.getMessage());
                throw ex;
            }
        }
    }
}

package com.example.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.example.contract.CPContract;
import com.example.state.CPState;
import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.Issued;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.OpaqueBytes;
import net.corda.core.utilities.ProgressTracker;
import org.apache.log4j.Logger;

import java.time.Duration;
import java.time.Instant;
import java.util.Currency;

import static com.example.contract.CPContract.CP_CONTRACT_ID;

@StartableByRPC
@InitiatingFlow
public class CPIssueFlow extends FlowLogic<SignedTransaction> {
    private final Integer amount;
    //private final Integer month;
    private final String  strCurrency;
    private Instant maturityDate;
    public static Logger logger1 = Logger.getLogger(CPIssueFlow.class);


    public CPIssueFlow(Integer amount, String strCurrency, Integer month) throws FlowException {
        logger1.info("CPIssueFlow constructor");
        this.amount = amount;
        this.strCurrency = strCurrency;
        Instant now = Instant.now();
        this.maturityDate   = now.plusSeconds(month*30*24*60*60);
        logger1.info("maturityDate: " + this.maturityDate.toString());
    }

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }
    private final ProgressTracker.Step PREPARING = new ProgressTracker.Step("Gathering the required inputs");
    private final ProgressTracker.Step ISSUING = new ProgressTracker.Step("Issuing cash.");
    private final ProgressTracker.Step RETURNING = new ProgressTracker.Step("Returning the newly-issued cash state.");
    private final ProgressTracker progressTracker = new ProgressTracker(
            PREPARING,
            ISSUING,
            RETURNING
    );

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {

        try {
            logger1.info("CPIssueFlow call start");
            progressTracker.setCurrentStep(PREPARING);

            Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
            logger1.info(notary.toString());
            Party me = getOurIdentity();
            logger1.info(me.toString());

            Currency currency = Currency.getInstance(this.strCurrency);

            Byte a = 0;
            OpaqueBytes opaqueBytes =  OpaqueBytes.of(a);
            Issued<Currency> iCurrency = new Issued<>(me.ref(opaqueBytes), currency);

            logger1.info(iCurrency.toString());
           final Amount<Issued<Currency>> faceValue = new Amount<>(this.amount * 100, iCurrency);
            logger1.info(faceValue.toString());

            progressTracker.setCurrentStep(ISSUING);
            CPState commercialPaper = new CPState(me.ref(opaqueBytes), me, faceValue, this.maturityDate);
            logger1.info("commercialPaper: " + commercialPaper.toString());

            final Command<CPContract.Commands.Issue> txCommand = new Command<>(
                    new CPContract.Commands.Issue(),
                    ImmutableList.of(commercialPaper.getIssuance().getParty().getOwningKey(), commercialPaper.getOwner().getOwningKey()));

            final TransactionBuilder txBuilder = new TransactionBuilder(notary);
            txBuilder.addOutputState(commercialPaper, CP_CONTRACT_ID);
            txBuilder.addCommand(txCommand);
           // Duration fff = new Duration(100,0);
            txBuilder.setTimeWindow(Instant.now(),Duration.ofSeconds(100));
            logger1.info(txBuilder.toString());

            txBuilder.verify(getServiceHub());

            final SignedTransaction signedTx = getServiceHub().signInitialTransaction(txBuilder);

            progressTracker.setCurrentStep(RETURNING);
            logger1.info(signedTx.toString());
            return subFlow(new FinalityFlow(signedTx));
        }
        catch (Exception ex)
        {
            logger1.info("CPIssueFlow call exception: " + ex.getMessage());
            throw ex;
        }

    }
}

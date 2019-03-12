package com.example.flow;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.contracts.Amount;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.Party;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.core.utilities.OpaqueBytes;
import net.corda.core.utilities.ProgressTracker;
import net.corda.core.utilities.ProgressTracker.Step;
import net.corda.finance.contracts.asset.Cash;
import net.corda.finance.flows.AbstractCashFlow;
import net.corda.finance.flows.CashIssueFlow;
import org.apache.log4j.Logger;

import java.util.Currency;

@StartableByRPC
@InitiatingFlow
public class SelfIssueCashFlow extends FlowLogic<Cash.State> {

    private final Integer amount;
    private final String  strCurrency;
    public static Logger logger1 = Logger.getLogger(SelfIssueCashFlow.class);


    public SelfIssueCashFlow(Integer amount, String strCurrency) throws FlowException {
        logger1.info("SelfIssueCashFlow constructor");
        logger1.info(amount.toString());

        logger1.info("amount:" +  amount.toString());
        logger1.info(strCurrency);

        this.amount = amount ;
        logger1.info("this.amount: " + this.amount.toString());

        this.strCurrency = strCurrency;
        logger1.info("this.strCurrency: " + this.strCurrency);



    }


    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }
    private final Step PREPARING = new Step("Gathering the required inputs");
    private final Step ISSUING = new Step("Issuing cash.");
    private final Step RETURNING = new Step("Returning the newly-issued cash state.");
    private final ProgressTracker progressTracker = new ProgressTracker(
            PREPARING,
            ISSUING,
            RETURNING
    );

    @Suspendable
    @Override
    public Cash.State  call() throws FlowException {

        logger1.info("SelfIssueCashFlow call start");
        progressTracker.setCurrentStep(PREPARING);
        logger1.info("PREPARING");
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
        logger1.info(notary.toString());

       // Party me = getOurIdentity();
        Byte a = 0;
        logger1.info(a.toString());
        OpaqueBytes issuerRef =  OpaqueBytes.of(a);

        logger1.info(issuerRef.toString());

        Currency currency = Currency.getInstance(this.strCurrency);
        logger1.info(currency.toString());

        Amount<Currency> amount1 = new Amount<Currency>(this.amount*100,currency);
        logger1.info(amount1.toString());

        progressTracker.setCurrentStep(ISSUING);
        logger1.info("ISSUING");

        AbstractCashFlow.Result  cashIssueSubflowResult = subFlow( new CashIssueFlow(amount1,issuerRef,notary));
        logger1.info(cashIssueSubflowResult.toString());

        progressTracker.setCurrentStep(RETURNING);
        logger1.info("RETURNING");

        try {
            LedgerTransaction cashIssueTx = cashIssueSubflowResult.getStx().toLedgerTransaction(getServiceHub(),false);
            logger1.info(cashIssueTx.toString());

            Cash.State aa = (Cash.State)cashIssueTx.getOutputStates().get(0);
            logger1.info(aa.toString());
            return aa;
        }
        catch (Exception ex)
        {
            logger1.info(ex.getMessage());
            return null;
        }


    }


}

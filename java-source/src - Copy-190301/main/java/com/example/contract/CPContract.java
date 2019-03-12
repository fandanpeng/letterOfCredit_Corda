package com.example.contract;

import com.example.state.CPState;
import net.corda.core.contracts.*;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.finance.contracts.CommercialPaper;
import org.apache.log4j.Logger;

import java.time.Instant;
import java.util.Currency;
import java.util.List;
import static net.corda.finance.utils.StateSumming.sumCashBy;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

public class CPContract implements Contract {

    public static final String CP_CONTRACT_ID = "com.example.contract.CPContract";
    public static Logger logger1 = Logger.getLogger(CPContract.class);
    @Override
    public void verify(LedgerTransaction tx) {
        try {
            logger1.info("Start CP contract verify");
            List<LedgerTransaction.InOutGroup<CPState, CPState>> groups = tx.groupStates(CPState.class, CPState::withoutOwner);
            CommandWithParties<Commands> cmd = requireSingleCommand(tx.getCommands(), Commands.class);
            logger1.info(cmd.toString());
            TimeWindow timeWindow = tx.getTimeWindow();
            logger1.info(timeWindow.toString());

            for (LedgerTransaction.InOutGroup group : groups) {
                logger1.info(group.toString());
                List<CPState> inputs = group.getInputs();
                List<CPState> outputs = group.getOutputs();

                if (cmd.getValue() instanceof Commands.Move) {
                    logger1.info("Commands.Move");
                    CPState input = inputs.get(0);
                    requireThat(require -> {
                        require.using("the transaction is signed by the owner of the CP", cmd.getSigners().contains(input.getOwner().getOwningKey()));
                        require.using("the state is propagated", outputs.size() == 1);
                        // Don't need to check anything else, as if outputs.size == 1 then the output is equal to
                        // the input ignoring the owner field due to the grouping.
                        return null;
                    });

                } else if (cmd.getValue() instanceof Commands.Redeem) {
                    logger1.info("Commands.Redeem");
                    // Redemption of the paper requires movement of on-ledger cash.
                    CPState input = inputs.get(0);
                    Amount<Issued<Currency>> received = sumCashBy(tx.getOutputStates(), input.getOwner());
                    logger1.info("received: " + received.toString());
                    Amount<Issued<Currency>> CPFaceValue = input.getFaceValue();

                    logger1.info("CPFaceValue: " + CPFaceValue.toString());


                    if (timeWindow == null) throw new IllegalArgumentException("Redemptions must be timestamped");
                    Instant time = timeWindow.getFromTime();
                    requireThat(require -> {
                        //require.using("the paper must have matured", time.isAfter(input.getMaturityDate()));
                        require.using("the received amount equals the face value", received.compareTo(CPFaceValue)==0);
                        require.using("the paper must be destroyed", outputs.isEmpty());
                        require.using("the transaction is signed by the owner of the CP", cmd.getSigners().contains(input.getOwner().getOwningKey()));
                        return null;
                    });
                } else if (cmd.getValue() instanceof Commands.Issue) {
                    logger1.info("Commands.Issue");
                    CPState output = outputs.get(0);
                    if (timeWindow == null) throw new IllegalArgumentException("Issuances must have a time-window");
                    Instant time = timeWindow.getUntilTime();
                    requireThat(require -> {
                        // Don't allow people to issue commercial paper under other entities identities.
                        require.using("output states are issued by a command signer", cmd.getSigners().contains(output.getIssuance().getParty().getOwningKey()));
                        require.using("output values sum to more than the inputs", output.getFaceValue().getQuantity() > 0);
                        require.using("the maturity date is not in the past", time.isBefore(output.getMaturityDate()));
                        // Don't allow an existing CP state to be replaced by this issuance.
                        require.using("can't reissue an existing state", inputs.isEmpty());
                        return null;
                    });
                } else {
                    throw new IllegalArgumentException("Unrecognised command");
                }
            }
        }
        catch (Exception ex) {
            logger1.info("Verify exception :" + ex.getMessage());
            throw ex;
        }


    }

    public static class Commands implements CommandData {

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

        public static class Issue extends Commands {
            @Override
            public boolean equals(Object obj) {
                return obj instanceof Issue;
            }
        }
    }
}

package com.example.contract;

import com.example.state.LCState;
import com.example.state.MBState;
import net.corda.core.contracts.Contract;

import net.corda.core.contracts.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.transactions.LedgerTransaction;
import net.corda.finance.contracts.CommercialPaper;
import org.apache.log4j.Logger;

import java.time.Instant;
import java.util.Currency;
import java.util.List;
import java.util.stream.Collectors;

import static net.corda.finance.utils.StateSumming.sumCashBy;

import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;
public class LCContract implements Contract {

    public static final String LC_CONTRACT_ID = "com.example.contract.LCContract";
    public static Logger logger1 = Logger.getLogger(LCContract.class);
    @Override
    public void verify(LedgerTransaction tx) {
        try {
            logger1.info("Start LC contract verify");
            List<LedgerTransaction.InOutGroup<LCState, LCState>> groups = tx.groupStates(LCState.class, LCState::withoutOwner);


            CommandWithParties<Commands> cmd = requireSingleCommand(tx.getCommands(), Commands.class);
            logger1.info(cmd.toString());
            TimeWindow timeWindow = tx.getTimeWindow();
            logger1.info(timeWindow.toString());

            for (LedgerTransaction.InOutGroup group : groups) {
                logger1.info(group.toString());
                List<LCState> inputs = group.getInputs();
                List<LCState> outputs = group.getOutputs();

                if (cmd.getValue() instanceof Commands.Issue) {
                    logger1.info("Commands.Issue");
                    // Redemption of the paper requires movement of on-ledger cash.
                    LCState output = outputs.get(0);
                    Amount<Issued<Currency>> received = sumCashBy(tx.getOutputStates(), output.getIssuer());
                    logger1.info("received: " + received.toString());


                    if (timeWindow == null) throw new IllegalArgumentException("Redemptions must be timestamped");
                    requireThat(require -> {
                        require.using("checkServiceCharge should pass",output.checkServiceChargeApply(received) );
                        require.using("checkLCState should pass",output.checkLCState() );
                        require.using("the transaction is signed by all participant", cmd.getSigners().containsAll(output.getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList())));
                        return null;
                    });

                }
                else if (cmd.getValue() instanceof Commands.Move) {
                    logger1.info("Commands.Move");
                    LCState input = inputs.get(0);
                    LCState output = outputs.get(0);
                    requireThat(require -> {
                        require.using("the transaction is signed by the owner of the LC", cmd.getSigners().contains(input.getOwner().getOwningKey()));
                        require.using("the state is propagated", outputs.size() == 1);
                        require.using("checkLCState should pass",output.checkLCState() );
                        require.using("the transaction is signed by all participant", cmd.getSigners().containsAll(output.getParticipants().stream().map(AbstractParty::getOwningKey).collect(Collectors.toList())));
                        // Don't need to check anything else, as if outputs.size == 1 then the output is equal to
                        // the input ignoring the owner field due to the grouping.
                        return null;
                    });
                }
                else if (cmd.getValue() instanceof Commands.Acceptance) {
                    logger1.info("Commands.Redeem");
                    // Redemption of the paper requires movement of on-ledger cash.
                    LCState input = inputs.get(0);
                    LCState output = outputs.get(0);
                    Amount<Issued<Currency>> received = sumCashBy(tx.getOutputStates(), input.getBenificiary());
                    logger1.info("received: " + received.toString());
                    if (timeWindow == null) throw new IllegalArgumentException("Redemptions must be timestamped");
                    Instant now = Instant.now();
                    requireThat(require -> {
                        require.using("Acceptance not later than NotLaterDate", now.isBefore(input.getNotLaterDate()));
                        require.using("Acceptance cash should minus 0.1% service charge", output.checkServiceChargeAcceptance(received));
                        require.using("the transaction is signed by all participant", cmd.getSigners().containsAll(output.getParticipants()));
                        return null;
                    });
                    // check marineBill
                    MBState marineBill = tx.inputsOfType(MBState.class).get(0);
                    requireThat(require ->{
                        require.using("need corresponding marineBill", input.checkMarineBill(marineBill));
                        return null;
                    });



                }
                else if (cmd.getValue() instanceof Commands.Reimburse) {

                }
                else if (cmd.getValue() instanceof Commands.RedeemBill) {

                }
                else {
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

        public static class Acceptance extends Commands {
            @Override
            public boolean equals(Object obj) {
                return obj instanceof Acceptance;
            }
        }
        public static class Reimburse extends Commands {
            @Override
            public boolean equals(Object obj) {
                return obj instanceof Reimburse;
            }
        }

        public static class RedeemBill extends Commands {
            @Override
            public boolean equals(Object obj) {
                return obj instanceof RedeemBill;
            }
        }

    }


}

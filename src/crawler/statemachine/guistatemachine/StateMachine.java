package crawler.statemachine.guistatemachine;

import crawler.actions.Action;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class StateMachine<
        St extends Enum<St>,
        Ev extends Enum<Ev>> {
    private final Map<Transition<St, Ev>, Action> mappedActions;
    private final ExecutorService serviceForActionsExecution;

    private St currentState;

    public StateMachine(St initState, Map<Transition<St, Ev>, Action> mappedActions) {
        this.serviceForActionsExecution = Executors.newFixedThreadPool(2);
        this.currentState = initState;
        this.mappedActions = mappedActions;

//        FOR LOGGING
        printTransitions();
    }

    public synchronized void handleEvent(Ev event) {
        correspondingTransition(currentState, event)
                .ifPresent(tr -> {
                    currentState = tr.targetState;

                    //                   FOR LOGGING
                    System.out.println(tr);
                    System.out.println();

                    executeActions(mappedActions, tr);
                });
    }

    private Optional<Transition<St, Ev>> correspondingTransition(St sourceState, Ev event) {
        Transition<St, Ev> out = null;
        final Set<Transition<St, Ev>> transitions = mappedActions.keySet();
        for (Transition<St, Ev> cur : transitions) {
            if (cur.sourceState.equals(sourceState) && cur.event.equals(event)) {
                out = cur;
                break;
            }
        }
        return Optional.ofNullable(out);
    }

    private void executeActions(Map<Transition<St, Ev>, Action> mappedActionsForTransitions,
                                Transition<St, Ev> transition) {
        final Action act = mappedActionsForTransitions.get(transition);
        if (act != null) {
            serviceForActionsExecution.execute(act::execute);
        }
    }

    private void printTransitions() {
        int i = 1;
        for (Transition<St, Ev> t : mappedActions.keySet()) {
            System.out.println(i + ": " + t);
            i++;
        }
    }
}
package crawler.actions;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class InParallel implements Action {
    private final Collection<Action> actions;
    private final ExecutorService executorService;
    private final int timeLimit;
    private final TimeUnit timeUnit;

    public InParallel(Collection<Action> actionsToExecute, int threadsNumber, int timeLimit, TimeUnit timeUnit) {
        this.actions = actionsToExecute;
        this.executorService = Executors.newFixedThreadPool(threadsNumber);
        this.timeLimit = timeLimit;
        this.timeUnit = timeUnit;
    }

    @Override
    public void execute() {
        try {
            final Set<Callable<Void>> callables = actions.stream()
                    .map(action ->
                                 (Callable<Void>) () -> {
                                     action.execute();
                                     return null;
                                 })
                    .collect(Collectors.toSet());
            executorService.invokeAll(callables, timeLimit, timeUnit);
            executorService.shutdown();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}

package crawler.actions;

@FunctionalInterface
public interface Action {
    void execute();

    default Action then(Action nextAction) {
        final Action firstAction = this;
        return new Action() {
            @Override
            public void execute() {
                firstAction.execute();
                nextAction.execute();
            }

            @Override
            public String toString() {
                return firstAction + " -> " + nextAction;
            }
        };
    }
}

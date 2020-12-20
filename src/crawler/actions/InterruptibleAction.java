package crawler.actions;

public interface InterruptibleAction extends Action {
    void interrupt();

    default InterruptibleAction then(InterruptibleAction next) {
        final InterruptibleAction prev = this;
        return new InterruptibleAction() {
            @Override
            public void execute() {
                prev.execute();
                next.execute();
            }

            @Override
            public void interrupt() {
                prev.interrupt();
                next.interrupt();
            }
        };
    }
}

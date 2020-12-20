package crawler.statemachine.guistatemachine;

public class Transition<St extends Enum<St>, Ev extends Enum<Ev>> {
    public final St sourceState;
    public final St targetState;
    public final Ev event;

    public Transition(St sourceState, Ev event, St targetState) {
        this.sourceState = sourceState;
        this.event = event;
        this.targetState = targetState;
    }

    @Override
    public String toString() {
        return sourceState + " --- " + event + " --> " + targetState;
    }
}

package crawler.statemachine.guistatemachine;

@FunctionalInterface
public interface SomeContext<T> {
    T context();
}

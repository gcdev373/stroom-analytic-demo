package stroom.analytics.demo.eventgen.beans;

import java.util.Arrays;
import java.util.Objects;

public class State {
    private float initialLikelihood;
    private String name;
    private Transition [] transitions;

    public float getInitialLikelihood() {
        return initialLikelihood;
    }

    public String getName() {
        return name;
    }

    public Transition[] getTransitions() {
        return transitions;
    }

    @Override
    public String toString() {
        return "State{" +
                "initialLikelihood=" + initialLikelihood +
                ", name='" + name + '\'' +
                ", transitions=" + Arrays.toString(transitions) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        State state = (State) o;
        return name.equals(state.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}

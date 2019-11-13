package stroom.analytics.demo.eventgen.beans;

import java.util.Arrays;
import java.util.Objects;

public class Identity {
    private String name;

    private int count;

    private Affinity[] affinities;

    private State [] states;

    public String getName() {
        return name;
    }

    public Affinity[] getAffinities() {
        return affinities;
    }

    public State[] getStates() {
        return states;
    }

    public int getCount() {
        return count;
    }

    @Override
    public String toString() {
        return "Identity{" +
                "name='" + name + '\'' +
                ", count=" + count +
                ", affinities=" + Arrays.toString(affinities) +
                ", states=" + Arrays.toString(states) +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Identity identity = (Identity) o;
        return name.equals(identity.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    public State getState (String name){
        if (states == null)
            return null;
        for (int s = 0; s < states.length; s++){
            if (states[s].getName().equals(name))
                return states[s];
        }
        return null;
    }
}

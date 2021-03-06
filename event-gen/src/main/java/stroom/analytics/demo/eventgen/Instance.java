package stroom.analytics.demo.eventgen;

import stroom.analytics.demo.eventgen.beans.Type;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class Instance {
    private final Type type;

    private final String name;

    private String state;


    public Instance(Type type, String name){
        this.type = type;
        this.name = name;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Map<Type, Instance> getAffinities() {
        return affinities;
    }

    private Map<Type, Instance> affinities = new HashMap<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Instance instance = (Instance) o;
        return type.equals(instance.type) &&
                name.equals(instance.name);
    }

    public Type getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, name);
    }

    @Override
    public String toString() {
        return "Instance{" +
                "type=" + type.getName() +
                ", name='" + name + '\'' +
                ", state='" + state + '\'' +
                '}';
    }
}

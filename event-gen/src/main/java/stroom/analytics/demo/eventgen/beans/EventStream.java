package stroom.analytics.demo.eventgen.beans;

import java.util.Arrays;

public class EventStream {
    private String name;
    private String stroomFeed;
    private int latencySecs;
    private float fractionDelayed;

    private String [] identifiedObjects;

    public String getName() {
        return name;
    }

    public String getStroomFeed() {
        return stroomFeed;
    }

    public int getLatencySecs() {
        return latencySecs;
    }

    public String[] getIdentifiedObjects() {
        return identifiedObjects;
    }

    public float getFractionDelayed() {
        return fractionDelayed;
    }

    @Override
    public String toString() {
        return "EventStream{" +
                "name='" + name + '\'' +
                ", stroomFeed='" + stroomFeed + '\'' +
                ", latencySecs=" + latencySecs +
                ", fractionDelayed=" + fractionDelayed +
                ", identifiedObjects=" + Arrays.toString(identifiedObjects) +
                '}';
    }
}

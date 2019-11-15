package stroom.analytics.demo.eventgen.beans;

public class Transition {

    private String name; // e.g. logon
    private int halfLifeSecs;
    private String to; //Statename
    private String eventStream; // Where to record the transition

    private String recordSpecialEventAs; //For recording special/rare events that may be identified later

    private String schedule;

    private boolean perfectAffinity;

    public String getName() {
        return name;
    }

    public String getTo() {
        return to;
    }

    public String getEventStream() {
        return eventStream;
    }

    public int getHalfLifeSecs() {
        return halfLifeSecs;
    }

    public String getRecordSpecialEventAs() { return recordSpecialEventAs; }

    public String getSchedule() {
        return schedule;
    }

    public boolean isPerfectAffinity() {
        return perfectAffinity;
    }

    @Override
    public String toString() {
        return "Transition{" +
                "name='" + name + '\'' +
                ", halfLifeSecs=" + halfLifeSecs +
                ", to='" + to + '\'' +
                ", eventStream='" + eventStream + '\'' +
                '}';
    }
}

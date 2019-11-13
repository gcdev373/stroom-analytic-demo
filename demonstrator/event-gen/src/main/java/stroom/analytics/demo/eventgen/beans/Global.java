package stroom.analytics.demo.eventgen.beans;

import java.util.Arrays;

public class Global {
    private String tempDirectory;

    private int randomSeed;

    private int runDays;

    private Identity [] identities;

    private EventStream [] streams;

    public int getRandomSeed() {
        return randomSeed;
    }

    public Identity[] getIdentities() {
        return identities;
    }

    public EventStream[] getStreams() {
        return streams;
    }

    public int getRunDays(){
        return runDays;
    }

    public String getTempDirectory() {
        return tempDirectory;
    }

    @Override
    public String toString() {
        return "Global{" +
                "randomSeed=" + randomSeed +
                ", runDays=" + runDays +
                ", identities=" + Arrays.toString(identities) +
                ", streams=" + Arrays.toString(streams) +
                '}';
    }
}

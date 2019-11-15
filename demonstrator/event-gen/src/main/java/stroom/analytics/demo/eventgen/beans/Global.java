package stroom.analytics.demo.eventgen.beans;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Arrays;
import java.util.Date;

public class Global {
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private Date startAt;

    private String timezone;

    private String workingDirectory;

    private int randomSeed;

    private int runDays;

    private String specialEventFile;

    private Type[] types;

    private EventStream [] streams;

    private Schedule [] schedules;

    public int getRandomSeed() {
        return randomSeed;
    }

    public Type[] getTypes() {
        return types;
    }

    public EventStream[] getStreams() {
        return streams;
    }

    public Schedule[] getSchedules(){
        return schedules;
    }

    public int getRunDays(){
        return runDays;
    }

    public String getWorkingDirectory() {
        return workingDirectory;
    }

    public Date getStartAt() {
        return startAt;
    }

    public String getSpecialEventFile() {
        return specialEventFile;
    }

    @Override
    public String toString() {
        return "Global{" +
                "randomSeed=" + randomSeed +
                ", runDays=" + runDays +
                ", identities=" + Arrays.toString(types) +
                ", streams=" + Arrays.toString(streams) +
                '}';
    }

    public String getTimezone() {
        return timezone;
    }

}

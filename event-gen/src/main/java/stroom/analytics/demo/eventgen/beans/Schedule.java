package stroom.analytics.demo.eventgen.beans;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.HashMap;
import java.util.Map;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class Schedule {
    public final static String[] DAY_NAMES= {"monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday"};
    private String name;
    private String monday,tuesday,wednesday,thursday,friday,saturday,sunday;

    @JsonIgnore
    private Map<String, float []> parsedDays = new HashMap<>();

    public String getName() {
        return name;
    }


    public float getValsForHourOfWeek (int dayNum, int hourNum){
        dayNum--; //Convert to zero based range
        String dayName = DAY_NAMES[dayNum];
        switch (dayNum){
            case 0: return getVal(dayName, monday, hourNum);
            case 1: return getVal(dayName, tuesday, hourNum);
            case 2: return getVal(dayName, wednesday, hourNum);
            case 3: return getVal(dayName, thursday, hourNum);
            case 4: return getVal(dayName, friday, hourNum);
            case 5: return getVal(dayName, saturday, hourNum);
            case 6: return getVal(dayName, sunday, hourNum);
        }
        throw new IllegalArgumentException("There is no day of week with number " + dayNum);
    }

    private float getVal (String dayName, String csvVals, int hourNum){
        if (parsedDays.containsKey(dayName))
            return parsedDays.get(dayName)[hourNum];

        String [] tokens = csvVals.split(",\\s?");

        if (tokens.length != 24){
            throw new IllegalArgumentException("Only " + tokens.length + " hours found for day " + dayName +
                    " in schedule " + name + " 24 are requird");
        }

        float [] parsedHours = new float[24];
        for (int t = 0; t < 24; t++){
            try {
                parsedHours[t] = Float.parseFloat(tokens[t]);
            } catch (NumberFormatException ex){
                throw new IllegalArgumentException("Bad hour definition for hour " + t + " of day " + dayName +
                        " in schedule " + name + " floating point number format required, but got " + tokens[t]);
            }
        }
        parsedDays.put(dayName, parsedHours);

        return parsedHours[hourNum];
    }
}

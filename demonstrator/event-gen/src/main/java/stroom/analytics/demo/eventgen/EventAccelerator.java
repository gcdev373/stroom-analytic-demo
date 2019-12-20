package stroom.analytics.demo.eventgen;

import java.io.*;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.TemporalAmount;
import java.util.Date;
import java.util.stream.Collectors;

/**
 * Creates files containing batches of events out of EventGen output, that is suitable for feeding to Stroom as hourly batches
 * The event time is manipulated as if the events were generated from a point a short time in the future
 * but at an accelerated rate to they were originally.
 */
public class EventAccelerator {

    private String inputFilenames [] = null;
    private String outputPath = "./eventAccelerator";

    private Instant oldestTimestamp;

    private Instant startTime = Instant.now().plus(Duration.ofMinutes(15));

    private int numberOfSecondsPerHour = 10;

    public EventAccelerator (String [] inputs) throws IOException {
        inputFilenames = inputs;
        oldestTimestamp = openInputs().minus(Duration.ofSeconds(10));

        createOutputDirectory();
    }

    //Check that all inputs exist and read the first event from each to determine the starting hour (first offset)
    private Instant openInputs () throws IOException {
            Instant oldestTimestamp = Instant.now();
        for (String filename : inputFilenames) {
            File input = new File(filename);

            BufferedReader reader = new BufferedReader(new InputStreamReader( new FileInputStream(input)));

            String [] tokens = reader.lines().findFirst().get().split(",");

            reader.close();
            Instant thisTimestamp = Instant.parse(tokens[0]);
            if (thisTimestamp.isBefore(oldestTimestamp))
                oldestTimestamp=thisTimestamp;
        }
        return oldestTimestamp;
    }

    private void createOutputDirectory (){
        File file = new File (outputPath);
        if(!file.exists())
            file.mkdirs();
    }

   public void generate () throws IOException {
        for (String file : inputFilenames){
            processFile(file);
        }
    }

    private Instant acceleratedInstant(Instant original){
        return Instant.ofEpochMilli(startTime.toEpochMilli() +
                ((original.toEpochMilli() - oldestTimestamp.toEpochMilli()) * numberOfSecondsPerHour / 3600));
    }

    private void processFile (String filename) throws IOException {
        System.out.println ("Processing input file " + filename + " into output directory " + outputPath);

        SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

        int hourCounter = 1;

        Instant maxTimestampForThisBatch = oldestTimestamp.plus(Duration.ofHours(1));

        Instant acceleratedMaxTimestampForThisBatch = acceleratedInstant(maxTimestampForThisBatch);

        File input = new File(filename);

        BufferedReader reader = new BufferedReader(new InputStreamReader( new FileInputStream(input)));

        BufferedWriter outputWriter = null;

        String line;
        while ((line = reader.readLine()) != null){
            String[] tokens = line.split(",");

            Instant acceleratedEventTime = acceleratedInstant (Instant.parse(tokens[0]));

//            System.out.println ("" + tokens[0] + " accelerated to " + acceleratedEventTime );

//            System.out.println ("Batch " + hourCounter + " maxEventTime " + maxTimestampForThisBatch + " accelerated to " + acceleratedMaxTimestampForThisBatch);

            while (acceleratedEventTime.isAfter(acceleratedMaxTimestampForThisBatch)){
                if (outputWriter != null) {
                    outputWriter.close();
                    outputWriter = null;
                }
                maxTimestampForThisBatch = maxTimestampForThisBatch.plus(Duration.ofHours(1));
                acceleratedMaxTimestampForThisBatch = acceleratedInstant(maxTimestampForThisBatch);
                hourCounter++;
            }
            if (outputWriter == null){
                outputWriter = new BufferedWriter(new FileWriter(outputPath + "/" + hourCounter + "." + filename));
            }
            outputWriter.write(isoFormat.format(new Date(acceleratedEventTime.toEpochMilli())));
            outputWriter.write(",");

            for (int t = 1; t < tokens.length; t++){
                outputWriter.write(tokens[t]);
                if (t + 1 < tokens.length)
                    outputWriter.write(",");
            }
            outputWriter.newLine();
        }

        outputWriter.close();



        reader.close();
    }

    public static void main (String [] args){
        if (args.length == 0){
            System.err.println ("Usage: java stroom.analytics.demo.eventgen.EventAccelerator <file1> ...[filen]");
            System.exit(1);
        }
        try{
            EventAccelerator instance = new EventAccelerator(args);

            instance.generate();

    } catch (FileNotFoundException e) {
        System.err.println ("Cannot open input file");
        e.printStackTrace();
    } catch (IOException e) {

        System.err.println ("Cannot read input file");
        e.printStackTrace();

    }
    }
}

import java.util.List;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Random;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.Comparator;


public class Solution {
    static int numberOfThreads;
    static Logger logger = Logger.getLogger(Solution.class.getName());
    static PriorityBlockingQueue<Integer> priorityQueueLowTemp;
    static PriorityBlockingQueue<Integer> priorityQueueHighTemp;
    static PriorityBlockingQueue<TenMinuteInterval> priorityQueueTenMinuteRunning;

    public static void main(String[] args) {
        performReadings();
    }

    // Method to handle each sensor (threads) readings
    public static void readerSimulator() {
        // Generate a random number between -70 and 101 to represent the temperature reading for a one minute interval
        Random r = new Random();
        int high = 101;
        int low = -70;

        // Keep track of minute count and a running list of temps for each sensor
        int minuteCount = 0;
        List<Integer> running = new ArrayList<>();

        // Continue thread for one hour
        while (minuteCount <= 60) {
            // Generate a random reading
            int currentReading = r.nextInt(high - low) + low;

            // Add reading to high and low priority queue as well as running list
            running.add(currentReading);
            priorityQueueHighTemp.add(currentReading);
            priorityQueueLowTemp.add(currentReading);

            // After tenth minute start measuring ten minute diff and adding to ten minute diff priority queue
            if (minuteCount >= 10) {
                int diff = Math.abs(running.get(minuteCount-10) - running.get(minuteCount));
                TenMinuteInterval interval = new TenMinuteInterval(minuteCount-9, minuteCount, diff);
                priorityQueueTenMinuteRunning.add(interval);
            }
            minuteCount += 1;
        }
    }

    public static void performReadings() {
        numberOfThreads = 8;

        // Initialize three block free priority queues (thread safe version of regular priority queues) to handle the sensor values
        // using the corresponding comparator functions (highest at top of queue for high and highest diff and lowest for low)
        priorityQueueLowTemp = new PriorityBlockingQueue<>(100, new LowComparator());
        priorityQueueHighTemp = new PriorityBlockingQueue<>(100, new HighComparator());
        priorityQueueTenMinuteRunning = new PriorityBlockingQueue<>(100, new TenMinuteComparator());

        System.out.println("Begginning collecting readings...");

        // Start threads to perform computation
        Long startTime = System.currentTimeMillis();

        Thread[] threads = new Thread[numberOfThreads];
        for (int i = 0; i < numberOfThreads; ++i) {
            threads[i] = new Thread(Solution::readerSimulator);
            threads[i].start();
        }

        try {
            for (int i = 0; i < numberOfThreads; ++i) {
                threads[i].join();
            }
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, e.getMessage());
            Thread.currentThread().interrupt();
        }

        Long endTime = System.currentTimeMillis();

        System.out.println("Finished collecting readings in " + (endTime - startTime) + " ms");

        // Report on results by polling the priority queues
        List<String> top5Temps = new ArrayList<>();
        List<String> bottom5Temps = new ArrayList<>();

        for (int i = 0; i < 5; ++i) {
            top5Temps.add(String.valueOf(priorityQueueHighTemp.poll()));
            bottom5Temps.add(String.valueOf(priorityQueueLowTemp.poll()));
        }

        TenMinuteInterval biggestDifferenceInterval = priorityQueueTenMinuteRunning.poll();
        System.out.println("Top 5 Temps");
        System.out.println(String.join(", ", top5Temps));
        System.out.println("Bottom 5 Temps");
        System.out.println(String.join(", ", bottom5Temps));
        System.out.println("Biggest Difference");
        System.out.println("Between minute " + biggestDifferenceInterval.start + " to " + biggestDifferenceInterval.end + " where difference was " + biggestDifferenceInterval.diff);
    }
}

// Custom data structure for ten minute interval readings so we can compare on diff for purpose of priority queue
// but also store the starting and ending minute
class TenMinuteInterval {
    int start;
    int end;
    int diff;

    public TenMinuteInterval(int start, int end, int diff) {
        this.start = start;
        this.end = end;
        this.diff = diff;
    }
}

// Comparator class for low priorty queue that places lowest value at top
class LowComparator implements Comparator<Integer>{
    public int compare(Integer reading1, Integer reading2) {
        if (reading1 < reading2)
            return -1;
        else if (reading1 > reading2)
            return 1;
        return 0;
    }
}

// Comparator class for high priorty queue that places highest value at top
class HighComparator implements Comparator<Integer>{
    public int compare(Integer reading1, Integer reading2) {
        if (reading1 < reading2)
            return 1;
        else if (reading1 > reading2)
            return -1;
        return 0;
    }
}

// Comparator class for ten minute priorty queue that places highest ten minute differential value at top
class TenMinuteComparator implements Comparator<TenMinuteInterval>{
    public int compare(TenMinuteInterval reading1, TenMinuteInterval reading2) {
        if (reading1.diff < reading2.diff)
            return 1;
        else if (reading1.diff > reading2.diff)
            return -1;
        return 0;
    }
}
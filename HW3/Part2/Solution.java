import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicMarkableReference;
import java.util.logging.Level;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
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

    public static void readerSimulator() {
        Random r = new Random();
        int high = 101;
        int low = -70;
        int minuteCount = 0;
        List<Integer> running = new ArrayList<>();
        while (minuteCount <= 60) {
            int currentReading = r.nextInt(high - low) + low;
            running.add(currentReading);
            priorityQueueHighTemp.add(currentReading);
            priorityQueueLowTemp.add(currentReading);
            if (minuteCount >= 10) {
                int diff = Math.abs(running.get(minuteCount-10) - running.get(minuteCount));
                TenMinuteInterval interval = new TenMinuteInterval(minuteCount-9, minuteCount, diff);
                priorityQueueTenMinuteRunning.add(interval);
            }
            minuteCount += 1;
        }
    }

    public static void performReadings() {
        // Initialize the array to keep track of whos eaten the cupcake and initialize the participants (threads)
        numberOfThreads = 8;

        priorityQueueLowTemp = new PriorityBlockingQueue<Integer>(100, new LowComparator());
        priorityQueueHighTemp = new PriorityBlockingQueue<Integer>(100, new HighComparator());
        priorityQueueTenMinuteRunning = new PriorityBlockingQueue<TenMinuteInterval>(100, new TenMinuteComparator());

        System.out.println("Begginning Collecting Readings...");

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

        System.out.println("Finished Collecting Readings");

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

class LowComparator implements Comparator<Integer>{
    public int compare(Integer reading1, Integer reading2) {
        if (reading1 < reading2)
            return -1;
        else if (reading1 > reading2)
            return 1;
        return 0;
    }
}

class HighComparator implements Comparator<Integer>{
    public int compare(Integer reading1, Integer reading2) {
        if (reading1 < reading2)
            return 1;
        else if (reading1 > reading2)
            return -1;
        return 0;
    }
}

class TenMinuteComparator implements Comparator<TenMinuteInterval>{
    public int compare(TenMinuteInterval reading1, TenMinuteInterval reading2) {
        if (reading1.diff < reading2.diff)
            return 1;
        else if (reading1.diff > reading2.diff)
            return -1;
        return 0;
    }
}
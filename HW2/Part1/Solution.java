import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Random;


public class Solution {
    static int numberOfThreads;
    static final AtomicBoolean cupcake = new AtomicBoolean(false);
    static final AtomicInteger numberOfTimesCupcakeGone = new AtomicInteger(0);
    static final int N = 100;
    static Logger logger = Logger.getLogger(Solution.class.getName());
    static boolean[] gottenCupcake;
    static AtomicInteger currentPersonInMaze = new AtomicInteger(0);

    public static void main(String[] args) {
        winningProtocol(N);
    }

    public static void participantSimulation() {
        // If the cupcake is there and this person has not gotten the cupcake, get the cupcake
        // and persist it into the array.
        if (cupcake.get() && !gottenCupcake[currentPersonInMaze.get()]) {
            cupcake.set(false);
            gottenCupcake[currentPersonInMaze.get()] = true;
        }
    }

    public static void counterSimulation() {
        // If the cupcake isn't there, replace it since your the counter and increment the count in
        // your head
        if (!cupcake.get()) {
            numberOfTimesCupcakeGone.getAndIncrement();
            cupcake.set(true);
        }
    }

    public static void winningProtocol(int numberOfParticipants) {
        // Initialize the array to keep track of whos eaten the cupcake and initialize the participants (threads)
        numberOfThreads = numberOfParticipants;

        gottenCupcake = new boolean[numberOfParticipants + 1];
        Arrays.fill(gottenCupcake, false);

        Thread[] threads = new Thread[numberOfThreads];
        for (int i = 0; i < numberOfThreads - 1; ++i) {
            threads[i] = new Thread(Solution::participantSimulation);
        }

        // Initialize the counter (thread)
        threads[numberOfThreads - 1] = new Thread(Solution::counterSimulation);

        // Randomly pick people to go through the maze until everyone has eaten the cupcake
        while (numberOfTimesCupcakeGone.get() < N - 1) {
            Random r = new Random();
            int high = 100;
            int low = 0;
            int result = r.nextInt(high - low) + low;
            currentPersonInMaze.set(result);
            threads[result].run();
        }
    }
}
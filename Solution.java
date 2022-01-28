import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Solution {
    static boolean[] primes;
    static long maximum;
    static int numberOfThreads;
    static int numberOfTopPrimes;
    static final AtomicInteger sharedCount = new AtomicInteger(1);
    static final String OUTPUT_FILE_STRING = "primes.txt";
    static Logger logger = Logger.getLogger(Solution.class.getName());

    public static void main(String[] args) {
       findPrimes(100_000_000, 8, 10);
    }

    /*
        Implementation of the sieve of eratosthenes algorithm to find prime numbers up to a
        given limit. Works by going sequentially from 2 to the limit and at each iteration,
        marking every multiple of the current number as not prime in a boolean array. We only
        need to consider the values up to the square root of the limit, since by that point all
        future values will have been multiples of this subset of values and thus already be marked.
    */
    public static void sieveOfEratosthenes() {
        int i = 2;

        // Continue to go from i, i+1, i+2, ... until i > sqrt(maximum)
        while (i * i <= maximum) {
            // Get the value of our shared counts between threads so all threads can be constantly
            // for the efficiency improvement.
            i = sharedCount.getAndIncrement();

            // If the current number is prime, mark every multiple of that number as not prime.
            if (primes[i]) {
                for (int j = i * i; j <= maximum; j += i) {
                    primes[j] = false;
                }
            }
        }
    }

    /*
        Wrapper function to spin up the threads that will run sieve, then retrieve the prime sum,
        prime counts and the top prime values.
    */
    public static void findPrimes(long max, int numOfThreads, int numOfTopPrimes) {
        maximum = max;
        numberOfThreads = numOfThreads;
        numberOfTopPrimes = numOfTopPrimes;

        // Initialize a boolean array with all values set to prime so we can sequentially
        // mark them as not prime when running sieve.
        primes = new boolean[(int)max + 1];
        Arrays.fill(primes, true);

        primes[0] = false;
        primes[1] = false;

        Long startTime = System.currentTimeMillis();

        // Initialize a list of threads and set all of them to run the sieve algorithm.
        Thread[] threads = new Thread[numberOfThreads];

        for (int i = 0; i < numberOfThreads; ++i) {
            threads[i] = new Thread(Solution::sieveOfEratosthenes);
            threads[i].start();
        }

        // Join each thread we've created to run the sieve code to the main thread so the
        // main thread can wait for these threads to terminate before running the rest
        // of the code.
        try {
            for (int i = 0; i < numberOfThreads; ++i) {
                threads[i].join();
            }
        } catch (InterruptedException e) {
            logger.log(Level.SEVERE, e.getMessage());
            Thread.currentThread().interrupt();
        }

        Long endTime = System.currentTimeMillis();

        long primesSum = 0;
        long primesCount = 0;
        ArrayList<String> topPrimes = new ArrayList<>();

        // Iterate backwards through our boolean array and everytime we arrive at a true value
        // increment the prime sum and prime count. Also grab the last ten prime numbers to
        // display them in our output.
        for (int i = primes.length - 1; i >= 0; --i) {
            if (primes[i]) {
                primesSum += i;
                primesCount++;
        
                if (numberOfTopPrimes >= 0) {
                    topPrimes.add(String.valueOf(i));
                    --numberOfTopPrimes;
                }
            }
        }

        // Reverse the list of top primes since we grabbed it from the back of the boolean array and thus it
        // will be backwards.
        Collections.reverse(topPrimes);

        String outputString = (endTime - startTime) + "ms " + primesCount + " " + primesSum + "\n" + String.join(", ", topPrimes);

        // Open the file primes.txt and write the output.
        try(BufferedWriter writer = new BufferedWriter(new FileWriter(OUTPUT_FILE_STRING))) {
            writer.write(outputString);
        } catch (IOException e) {
            logger.log(Level.SEVERE, e.getMessage());
            Thread.currentThread().interrupt();
        }
    }
}
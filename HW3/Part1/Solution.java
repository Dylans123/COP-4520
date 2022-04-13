import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicMarkableReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Random;


public class Solution {
    static int numberOfThreads;
    static int[] presents; 
    static PresentList presentLinkedList;
    static final AtomicInteger thankYouCount = new AtomicInteger(0);
    static final AtomicInteger addCounter = new AtomicInteger(0);
    static final AtomicInteger topOuts = new AtomicInteger(-1);
    static final AtomicBoolean timeToRemove = new AtomicBoolean(false);
    static boolean[] thankYouWritten;
    static final int N = 500000;
    static final int CHUNK_SIZE = 5000;
    static Logger logger = Logger.getLogger(Solution.class.getName());

    public static void main(String[] args) {
        writeThankYouNotes(N);
    }

    // Method to represent the work done by each of the servants (threads)
    public static void servantSimulation() {
        // Generate a random number to decide to check if list contains or add to list when not removing
        Random r = new Random();
        int high = 3;
        int low = 1;

        // Continue to run the thread while we haven't satisfied writing a thank you note for every present
        while (thankYouCount.get() < N) {
            // Whether to check if a present is contained or add to list
            int result = r.nextInt(high - low) + low;

            // Keep atomic counter to know how many have been added and removed and when we're 5000 into adding new elements to linked
            // list start removing and reset counter to save on memory
            if (addCounter.get() - thankYouCount.get() >= CHUNK_SIZE) {
                timeToRemove.set(!timeToRemove.get());
                addCounter.set(0);
                continue;
            }

            // Increment counter determining what present we're currently looking at
            int count = addCounter.getAndIncrement();

            // Handle race case where one thread is done before others, just keep continuing until lagging thread catches up
            if (count > N) {
                continue;
            }

            // If we've alternated to removing start removing, otherwise add if the thank you note hasn't been written or check if its contained randomly.
            int present = presents[count];
            if (timeToRemove.get()) {
                boolean wasRemoved = presentLinkedList.remove(present);
                if (wasRemoved) {
                    thankYouWritten[present] = true;
                    thankYouCount.getAndIncrement();
                }
            } else if (!timeToRemove.get() && result % 2 == 0) {
                if (!thankYouWritten[present]) {
                    presentLinkedList.add(present);
                }
            } else {
                presentLinkedList.contains(present);
            }
        }
    }

    public static void writeThankYouNotes(int numberOfPresents) {
        numberOfThreads = 4;
        presents = new int[N+1];

        // Initialize lock free linked list data structure to store presents
        presentLinkedList = new PresentList();

        // Initialize presents and whether we've written a thank you note or not for that present as arrays
        thankYouWritten = new boolean[N+1];
        Arrays.fill(thankYouWritten, false);

        for (int i = 0; i < numberOfPresents; ++i) {
            presents[i] = i;
        }

        Long startTime = System.currentTimeMillis();

        // Run four threads to handle computation
        Thread[] threads = new Thread[numberOfThreads];
        for (int i = 0; i < numberOfThreads; ++i) {
            threads[i] = new Thread(Solution::servantSimulation);
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

        // Log end time and results
        Long endTime = System.currentTimeMillis();

        System.out.println(thankYouCount.get() + " thank you notes have been written in " + (endTime - startTime) + "ms.");
        System.out.println("There are " + presentLinkedList.getLength() + " presents left in the list to write thank you notes for.");
    }
}

// Interface for lock free linked list
interface PresentSet {
    public boolean add(int x);
    public boolean remove(int x);
    public boolean contains(int x);
}   

// Class for finding in window
class Window {
    public Node pred;
    public Node curr;
    Window(Node pred, Node curr) {
        this.pred = pred; this.curr = curr;
    }
}

// Class for node in linked list
class Node {
    public int key;
    public AtomicMarkableReference<Node> next;

    Node(int key) {
        this.key = key;
        this.next = new AtomicMarkableReference<Node>(null, false);
    }
}

// Implementation of lock free linked list from textbook
class PresentList implements PresentSet {
    public Node head;
    public AtomicInteger length;

    public PresentList() {
        this.length = new AtomicInteger(0);
        this.head  = new Node(Integer.MIN_VALUE);
        Node tail = new Node(Integer.MAX_VALUE);
        while (!head.next.compareAndSet(null, tail, false, false));
    }

    public int getLength() {
        return this.length.get();
    }

    public boolean remove(int key) {
        boolean snip;
        while (true) {
            // find predecessor and curren entries
            Window window = find(head, key);
            Node pred = window.pred, curr = window.curr;
            // is the key present?
            if (curr.key != key) {
                return false;
            } else {
                // snip out matching node
                Node succ = curr.next.getReference();
                snip = curr.next.attemptMark(succ, true);
                if (!snip) {
                    continue;
                }
                pred.next.compareAndSet(curr, succ, false, false);
                this.length.getAndDecrement();
                return true;
            }
        }
    }

    public boolean add(int key) {
        while (true) {
            // find predecessor and curren entries
            Window window = find(head, key);
            Node pred = window.pred, curr = window.curr;
            // is the key present?
            if (curr.key == key) {
                return false;
            } else {
                // splice in new node
                Node node = new Node(key);
                node.next = new AtomicMarkableReference<Node>(curr, false);
                if (pred.next.compareAndSet(curr, node, false, false)) {
                    this.length.getAndIncrement();
                    return true;
                }
            }
        }
    }

    public boolean contains(int key) {
        Window window = find(head, key);
        Node curr = window.curr;
        return (curr.key == key);
    }
    
    public Window find(Node head, int key) {
        Node pred = null, curr = null, succ = null;
        boolean[] marked = {false}; // is curr marked?
        boolean snip;
        retry: while (true) {
            pred = head;
            curr = pred.next.getReference();
            while (true) {
                succ = curr.next.get(marked);
                while (marked[0]) {           // replace curr if marked
                    snip = pred.next.compareAndSet(curr, succ, false, false);
                    if (!snip) continue retry;
                    curr = pred.next.getReference();
                    succ = curr.next.get(marked);
                }
                if (curr.key >= key) {
                    return new Window(pred, curr);
                }
                pred = curr;
                curr = succ;
            }
        }
    }
       
}
   
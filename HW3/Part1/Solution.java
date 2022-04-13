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


public class Solution {
    static int numberOfThreads;
    static ConcurrentLinkedQueue<Present> presents; 
    static PresentList presentLinkedList;
    static final AtomicInteger thankYouCount = new AtomicInteger(0);
    static final AtomicInteger atomicCounter = new AtomicInteger(0);
    static boolean[] thankYouWritten;
    static final int N = 500000;
    static Logger logger = Logger.getLogger(Solution.class.getName());

    public static void main(String[] args) {
        writeThankYouNotes(N);
    }

    public static void servantSimulation() {
        while (thankYouCount.get() < N) {
            Present present = presents.peek();
            int count = atomicCounter.getAndIncrement();
            if (thankYouCount.get() % 10000 == 0 && thankYouCount.get() != 0) {
                System.out.println(thankYouCount.get());
                System.out.println(presents.size());
                System.out.println(presentLinkedList.getLength());
            }
            if (count % 3 == 0) {
                boolean wasRemoved = presentLinkedList.remove(present);
                if (wasRemoved) {
                    presents.poll();
                    thankYouWritten[present.numberOfPresent] = true;
                    thankYouCount.getAndIncrement();
                }
            } else if (count % 2 == 0) {
                // if (!thankYouWritten[present.numberOfPresent]) {
                presentLinkedList.add(present);
                // }
            } else {
                presentLinkedList.contains(present);
            }
        }
    }

    public static void writeThankYouNotes(int numberOfPresents) {
        // Initialize the array to keep track of whos eaten the cupcake and initialize the participants (threads)
        numberOfThreads = 4;
        presents = new ConcurrentLinkedQueue<>();
        presentLinkedList = new PresentList();
        thankYouWritten = new boolean[N];
        Arrays.fill(thankYouWritten, false);

        for (int i = 1; i < numberOfPresents + 1; ++i) {
            Present present = new Present(i);
            presents.add(present);
        }

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

        System.out.println("done");
    }
}

class Present {
    public int numberOfPresent;

    public Present(int numberOfPresent) {
        this.numberOfPresent = numberOfPresent;
    }
}

interface PresentSet {
    public boolean add(Present x);
    public boolean remove(Present x);
    public boolean contains(Present x);
}   

class Window {
    public Node pred;
    public Node curr;
    Window(Node pred, Node curr) {
        this.pred = pred; this.curr = curr;
    }
}

class Node {
    public Present item;
    public int key;
    public AtomicMarkableReference<Node> next;

    Node(Present item) {      // usual constructor
        this.item = item;
        this.key = item.numberOfPresent;
        this.next = new AtomicMarkableReference<Node>(null, false);
    }
}

class PresentList implements PresentSet {
    public Node head;
    public int length;

    public PresentList() {
        this.head  = new Node(new Present(Integer.MIN_VALUE));
        Node tail = new Node(new Present(Integer.MAX_VALUE));
        while (!head.next.compareAndSet(null, tail, false, false));
    }

    public int getLength() {
        return this.length;
    }

    public boolean remove(Present item) {
        int key = item.numberOfPresent;
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
                this.length -= 1;
                return true;
            }
        }
    }

    public boolean add(Present item) {
        int key = item.numberOfPresent;
        boolean splice;
        while (true) {
            // find predecessor and curren entries
            Window window = find(head, key);
            Node pred = window.pred, curr = window.curr;
            // is the key present?
            if (curr.key == key) {
                return false;
            } else {
                // splice in new node
                Node node = new Node(item);
                node.next = new AtomicMarkableReference<Node>(curr, false);
                if (pred.next.compareAndSet(curr, node, false, false)) {
                    this.length +=  1;
                    return true;
                }
            }
        }
    }

    public boolean contains(Present item) {
        int key = item.numberOfPresent;
        Window window = find(head, key);
        Node pred = window.pred, curr = window.curr;
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
   
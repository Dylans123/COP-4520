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
    static int[] presents; 
    static PresentList presentLinkedList;
    static final AtomicInteger thankYouCount = new AtomicInteger(0);
    static final AtomicInteger atomicCounter = new AtomicInteger(0);
    static final AtomicInteger topOuts = new AtomicInteger(-1);
    static final AtomicBoolean timeToRemove = new AtomicBoolean(false);
    static boolean[] thankYouWritten;
    static final int N = 500000;
    static Logger logger = Logger.getLogger(Solution.class.getName());

    public static void main(String[] args) {
        writeThankYouNotes(N);
    }

    public static void servantSimulation() {
        Random r = new Random();
        int high = 4;
        int low = 1;
        while (thankYouCount.get() < N) {
            // if (presentLinkedList.getLength() > 1000) {
            //     // System.out.println("setting it");
            //     high = 3;
            //     atomicCounter.set(0);
            // } else {
            //     high = 4;
            // }
            int result = r.nextInt(high - low) + low;
            // System.out.println(result);
            // int topVal = topOuts.get();
            // System.out.println(atomicCounter.get());
            if (atomicCounter.get() - thankYouCount.get() >= 5000) {
                timeToRemove.set(!timeToRemove.get());
                atomicCounter.set(0);
                // System.out.println("doing it");
                // System.out.println(topVal+1);
                // System.out.println(atomicCounter.get());
                continue;
            }
            int count = atomicCounter.getAndIncrement();
            // if (count > N) {
            //     continue;
            // }
            int present = presents[count];
            // int passNum = topOuts.get() / N;
            // if (presentLinkedList.getLength() % 501 == 0 && presentLinkedList.getLength() != 0) {
            //     System.out.println("==========");
            //     System.out.println(thankYouCount.get());
            //     System.out.println(presentLinkedList.getLength());
            //     System.out.println("==========");
            // }
            // if (thankYouCount.get() % 501 == 0 && thankYouCount.get() != 0) {
            //     System.out.println("==========");
            //     System.out.println(thankYouCount.get());
            //     System.out.println(presentLinkedList.getLength());
            //     System.out.println("==========");
            // }
            if (timeToRemove.get() && result % 2 == 1) {
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
        // Initialize the array to keep track of whos eaten the cupcake and initialize the participants (threads)
        numberOfThreads = 4;
        presents = new int[N+1];
        presentLinkedList = new PresentList();
        thankYouWritten = new boolean[N+1];
        Arrays.fill(thankYouWritten, false);

        for (int i = 0; i < numberOfPresents; ++i) {
            presents[i] = i;
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

// class Present {
//     public int numberOfPresent;

//     public Present(int numberOfPresent) {
//         this.numberOfPresent = numberOfPresent;
//     }
// }

interface PresentSet {
    public boolean add(int x);
    public boolean remove(int x);
    public boolean contains(int x);
}   

class Window {
    public Node pred;
    public Node curr;
    Window(Node pred, Node curr) {
        this.pred = pred; this.curr = curr;
    }
}

class Node {
    public int key;
    public AtomicMarkableReference<Node> next;

    Node(int key) {      // usual constructor
        this.key = key;
        this.next = new AtomicMarkableReference<Node>(null, false);
    }
}

class PresentList implements PresentSet {
    public Node head;
    public int length;

    public PresentList() {
        this.head  = new Node(Integer.MIN_VALUE);
        Node tail = new Node(Integer.MAX_VALUE);
        while (!head.next.compareAndSet(null, tail, false, false));
    }

    public int getLength() {
        return this.length;
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
                this.length -= 1;
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
                    this.length +=  1;
                    return true;
                }
            }
        }
    }

    public boolean contains(int key) {
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
   
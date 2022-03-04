import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;


public class Solution {
    static int numberOfThreads;
    static final int N = 100;
    static Logger logger = Logger.getLogger(Solution.class.getName());
    static AtomicInteger currentlySeeingVase = new AtomicInteger(0);

    public static void main(String[] args) {
        winningProtocol(N);
    }

    public static void winningProtocol(int numberOfParticipants) {
        // Initialize the CLHLock queue
        numberOfThreads = numberOfParticipants;
        CLHLock queue = new CLHLock(N);

        // While everyone hasn't seen the vase
        while (queue.peopleWhoHaveSeenVase.get() < N) {
            // If the queue is locked unlock it (essentially switch whos seeing the vase)
            QNode head = queue.myNode.get();
            if (head.locked) {
                queue.unlock();
            }

            // Generare a random number to represent the next person whos seeing the vase
            Random r = new Random();
            int high = 100;
            int low = 0;
            int result = r.nextInt(high - low) + low;

            // Lock that person there so he can see the vase
            queue.lock(result);
        }

        queue.unlock();
    }
    
}

class CLHLock implements Lock {
    AtomicReference<QNode> tail;
    ThreadLocal<QNode> myPred;
    ThreadLocal<QNode> myNode;
    final AtomicInteger peopleWhoHaveSeenVase = new AtomicInteger(0);
    boolean[] seenVase;

    // Initialize the lock with curent and predecesor node, while also keeping a boolean array
    // to represent whos seen the vase
    public CLHLock(int numberOfParticipants) {
        myNode = new ThreadLocal<QNode>() {
            public QNode initialValue() {
                return new QNode();
            };
        };
        myPred = new ThreadLocal<QNode>() {
            public QNode initialValue() {
                return new QNode();
            }
        };
        tail = new AtomicReference<QNode>(myPred.get());

        seenVase = new boolean[numberOfParticipants + 1];
        Arrays.fill(seenVase, false);
    }

    // Lock the queue so no one else can see the vase
    public void lock(int number) {
        if (!seenVase[number]) {
            peopleWhoHaveSeenVase.getAndIncrement();
            seenVase[number] = true;
        }

        QNode qnode = myNode.get();
        qnode.locked = true;
        QNode pred = tail.getAndSet(qnode);
        myPred.set(pred);
        while (pred.locked) {

        }
    }

    // Unlock the queue to switch whose seeing the vase
    public void unlock() {
        QNode qnode = myNode.get();
        qnode.locked = false;
        myNode.set(myPred.get());
    }

    @Override
    public void lock() {
        // TODO Auto-generated method stub
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        // TODO Auto-generated method stub
        
    }

    @Override
    public boolean tryLock() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) throws InterruptedException {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Condition newCondition() {
        // TODO Auto-generated method stub
        return null;
    }
}

// Class to represent a node in the queue
class QNode {
    public boolean locked = false;
    public QNode next = null;
}
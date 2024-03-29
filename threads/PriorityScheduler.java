package nachos.threads;

import nachos.machine.*;

import java.util.*;

/**
 * A scheduler that chooses threads based on their priorities.
 *
 * <p>
 * A priority scheduler associates a priority with each thread. The next thread
 * to be dequeued is always a thread with priority no less than any other
 * waiting thread's priority. Like a round-robin scheduler, the thread that is
 * dequeued is, among all the threads of the same (highest) priority, the
 * thread that has been waiting longest.
 *
 * <p>
 * Essentially, a priority scheduler gives access in a round-robin fashion to
 * all the highest-priority threads, and ignores all other threads. This has
 * the potential to
 * starve a thread if there's always a thread waiting with higher priority.
 *
 * <p>
 * A priority scheduler must partially solve the priority inversion problem; in
 * particular, priority must be donated through locks, and through joins.
 */
public class PriorityScheduler extends Scheduler {
    /**
     * Allocate a new priority scheduler.
     */
    public PriorityScheduler() {
    }
    
    /**
     * Allocate a new priority thread queue.
     *
     * @param	transferPriority	<tt>true</tt> if this queue should
     *					transfer priority from waiting threads
     *					to the owning thread.
     * @return	a new priority thread queue.
     */
    public ThreadQueue newThreadQueue(boolean transferPriority) {
	return new PriorityQueue(transferPriority);
    }

    /**
     * getPriority()
     */
    public int getPriority(KThread thread) {
	Lib.assertTrue(Machine.interrupt().disabled());
		       
	return getThreadState(thread).getPriority();
    }

    /**
     * getEffectivePriority()
     */
    public int getEffectivePriority(KThread thread) {
	Lib.assertTrue(Machine.interrupt().disabled());
		       
	return getThreadState(thread).getEffectivePriority();
    }

    /**
     * setPriority()
     */
    public void setPriority(KThread thread, int priority) {
	Lib.assertTrue(Machine.interrupt().disabled());
		       
	Lib.assertTrue(priority >= priorityMinimum &&
		       priority <= priorityMaximum);
	
	getThreadState(thread).setPriority(priority);
    }

    /**
     * The self test 
     */
    public static void selfTest() {
      PrioritySchedulerTest.runTest(); 
    }

    /**
     * The default priority for a new thread. Do not change this value.
     */
    public static final int priorityDefault = 1;
    /**
     * The minimum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMinimum = 0;
    /**
     * The maximum priority that a thread can have. Do not change this value.
     */
    public static final int priorityMaximum = 7;    

    /**
     * Return the scheduling state of the specified thread.
     *
     * @param	thread	the thread whose scheduling state to return.
     * @return	the scheduling state of the specified thread.
     */
    protected ThreadState getThreadState(KThread thread) {
	if (thread.schedulingState == null) {
	    thread.schedulingState = new ThreadState(thread);
        }

	return (ThreadState) thread.schedulingState;
    }

    /**
     * A <tt>ThreadQueue</tt> that sorts threads by priority. Only
     * parts of this are implemented and you should complete it, by adding
     * code wherever you see fit and whichever method you see fit.
     */
    protected class PriorityQueue extends ThreadQueue {
      
	PriorityQueue(boolean transferPriority) {
	    this.transferPriority = transferPriority;
	}

        /**
         * The thread declares its intent to wait for access to the
         * "resource" guarded by this priority queue. This method is only called
         * if the thread cannot immediately obtain access.
         *
         * @param       thread       The thread 
         *
         * @see nachos.threads.ThreadQueue#waitForAccess
         */

	public void waitForAccess(KThread thread) {
	    Lib.assertTrue(Machine.interrupt().disabled());
      associatedThread = thread;
      waitQueue.add(associatedThread);
	}

        /* print(): Prints the priority queue, for potential debugging 
         */
	public void print() {
      for (Iterator i=waitQueue.iterator(); i.hasNext(); ) {
        System.out.print((KThread) i.next() + " ");
      }
    }

        /**
         * The specified thread has received exclusive access, without using
         * <tt>waitForAccess()</tt> or <tt>nextThread()</tt>. Assert that no
         * threads are waiting for access.
         */
	public void acquire(KThread thread) {
	    Lib.assertTrue(Machine.interrupt().disabled());
      Lib.assertTrue(waitQueue.size()==0);
      associatedThread = thread;
      getThreadState(associatedThread).acquire(this); //for Q2
	}

        /**
         * Select the next thread in the ThreadQueue
         */
	public KThread nextThread() {
	    Lib.assertTrue(Machine.interrupt().disabled());
      if (waitQueue.isEmpty()){
        associatedThread=null;
        return null;
      }
      associatedThread=waitQueue.peek();
      getThreadState(associatedThread).acquire(this); //for Q2
      return waitQueue.poll();  //return next thread
	}

	/**
	 * Return the next thread that <tt>nextThread()</tt> would return,
	 * without modifying the state of this queue.
	 *
	 * @return	the next thread that <tt>nextThread()</tt> would
	 *		return.
	 */
	protected KThread pickNextThread() {
        Lib.assertTrue(Machine.interrupt().disabled());
        if (waitQueue.isEmpty())
          return null;
        return waitQueue.peek();  //dont change the queue
	}
	
    /**
     * <tt>true</tt> if this queue should transfer priority from waiting
     * threads to the owning thread.
     */
    public boolean transferPriority;
    
    //create waitQueue, let pq + comparator class 
    //sort threads automatically by priority
    private Queue<KThread> waitQueue = new java.util.PriorityQueue<KThread>(1, new comparePriority());
    
    //compares priorities to for pq to sort threads
    public class comparePriority implements Comparator<KThread> {
      public int compare(KThread left, KThread right) {
          int leftP=getThreadState(left).getEffectivePriority();
          int rightP=getThreadState(right).getEffectivePriority();
          if(leftP<rightP)
            return -1;
          if(leftP>rightP)
            return 1;
          return 0;
        }
      }
    public KThread associatedThread = null;
    }

    /**
     * The scheduling state of a thread. This should include the thread's
     * priority, its effective priority, any objects it owns, and the queues
     * it's waiting for, etc. This is a convenience class so that
     * no modification to the KThread class are needed for a new scheduler.
     * Each scheduler keeps track of scheduler specific KThread information
     * in its own declaration of the ThreadState class.
     *
     * @see	nachos.threads.KThread#schedulingState
     */ 
    protected class ThreadState {

	/**
	 * Allocate a new <tt>ThreadState</tt> object and associate it with the
	 * specified thread.
	 *
	 * @param	thread	the thread this state belongs to.
	 */
	public ThreadState(KThread thread) {
	    this.thread = thread;
            this.priority = priorityDefault;
	}

	/**
	 * Return the priority of the associated thread.
	 *
	 * @return	the priority of the associated thread.
	 */
	public int getPriority() {
	    return priority;
	}

	/**
	 * Set the priority of the associated thread to the specified value.
	 *
	 * @param	priority	the new priority.
	 */
	public void setPriority(int priority) {
	    this.priority = priority;
	}

	/**
	 * Return the effective priority of the associated thread.
	 *
	 * @return	the effective priority of the associated thread.
	 */
	public int getEffectivePriority() {

    return this.priority;
	}

	/** The thread with which this object is associated. */	   
	protected KThread thread;
	/** The priority of the associated thread. */
	protected int priority;
  
      //need methods below? according to berkley nachos api doc

      /**
       * description from javadoc of nachos api:
       * Called when the associated thread has acquired access to 
       * whatever is guarded by waitQueue. This can occur either as a result 
       * of acquire(thread) being invoked on waitQueue (where thread is the 
       * associated thread), or as a result of nextThread() being invoked on 
       * waitQueue.
       *
       * @param	waitQueue	the PriorityScheduler's PriorityQueue
       */
  public void acquire(PriorityQueue waitQueue) {
        Lib.assertTrue(Machine.interrupt().disabled());

        

      }
      
      /**
       * description from javadoc of nachos api:
       * Called when waitForAccess(thread) (where thread is the 
       * associated thread) is invoked on the specified priority queue. This 
       * method is only called if the associated thread cannot immediately 
       * obtain access.
       *
       * @param	waitQueue the PriorityScheduler's PriorityQueue
       */
      public void waitForAccess(PriorityQueue waitQueue) {
        Lib.assertTrue(Machine.interrupt().disabled());
        
        
        
      }
    public LinkedList<PriorityQueue> queueList = new LinkedList<PriorityQueue>();
    public int EffectivePriority;
    }
}

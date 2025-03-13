/**
 * Coorindate the passage of time in a simulation.
 * Implements a monitor pattern for synchronised time access.
 * Used to avoid busy waiting in simulation components.
 */
public class TimeManager implements Runnable {
    @GuardedBy("this")
    private long currentTick = 0; // Protected by intrinsic lock
    
    private final int tickTimeMs;
    
    // Volatile to ensure visibility across threads without full synchronisations
    private volatile boolean running = true;
    
    /**
     * Creates a time manager with the specified tick duration.
     * 
     * @param tickTimeMs Duration of each tick in milliseconds
     */
    public TimeManager(int tickTimeMs) {
        this.tickTimeMs = tickTimeMs;
    }
    
    @Override
    public void run() {
        while (running) {
            try {
                // Sleep to simulate the passage of time
                Thread.sleep(tickTimeMs);
                incrementTick();
            } catch (InterruptedException e) {
                // Preserve interrupt status for proper shutdown
                Thread.currentThread().interrupt();
                running = false;
            }
        }
    }
    
    /**
     * Increments the current tick and notifies waiting threads
     * synchronised to ensure atomic update and consistent view
     */
    private synchronized void incrementTick() {
        currentTick++;
        // notify all waiting threads to prevent starvation
        notifyAll();
    }
    
    /**
     * Gets  current tick value.
     * Synchronised to ensure consistent view.
     * 
     * @return Current tick
     */
    public synchronized long getCurrentTick() {
        return currentTick;
    }
    
    /**
     * Waits until the specified number of ticks have passed.
     * "wait" part of monitor pattern.
     * avoid busy waiting in simulation components.
     * 
     * @param ticks Number of ticks to wait
     * @throws InterruptedException if thread is interrupted while waiting
     */
    public synchronized void waitTicks(long ticks) throws InterruptedException {
        if (ticks <= 0) return;
        
        long targetTick = currentTick + ticks;
        
        while (currentTick < targetTick) {
            if (!running) throw new InterruptedException("Time manager stopped");
            wait(); // Release lock and wait until notified
        }
    }
    
    public void stop() {
        running = false;
    }
}

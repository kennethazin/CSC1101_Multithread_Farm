public class TimeManager implements Runnable {
    private long currentTick = 0;
    private final int tickTimeMs;
    private volatile boolean running = true;
    
    public TimeManager(int tickTimeMs) {
        this.tickTimeMs = tickTimeMs;
    }
    
    @Override
    public void run() {
        while (running) {
            try {
                Thread.sleep(tickTimeMs);
                incrementTick();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                running = false;
            }
        }
    }
    
    private synchronized void incrementTick() {
        currentTick++;
        notifyAll(); // Notify threads waiting for ticks
    }
    
    public synchronized long getCurrentTick() {
        return currentTick;
    }
    
    public synchronized void waitTicks(long ticks) throws InterruptedException {
        if (ticks <= 0) return;
        
        long targetTick = currentTick + ticks;
        
        while (currentTick < targetTick) {
            if (!running) throw new InterruptedException("Time manager stopped");
            wait();
        }
    }
    
    public void stop() {
        running = false;
    }
}

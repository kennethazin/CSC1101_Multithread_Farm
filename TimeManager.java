import java.util.concurrent.atomic.AtomicLong;

public class TimeManager implements Runnable {
    private final AtomicLong currentTick = new AtomicLong(0);
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
                currentTick.incrementAndGet();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                running = false;
            }
        }
    }
    
    public long getCurrentTick() {
        return currentTick.get();
    }
    
    public void waitTicks(long ticks) throws InterruptedException {
        if (ticks <= 0) return;
        
        long startTick = currentTick.get();
        long targetTick = startTick + ticks;
        
        while (currentTick.get() < targetTick) {
            if (!running) throw new InterruptedException("Time manager stopped");
            Thread.sleep(1);
        }
    }
    
    public void stop() {
        running = false;
    }
}

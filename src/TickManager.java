public class TickManager {
    private static int tick = 0;

    public static synchronized void incrementTick() {
        tick++;
    }

    public static synchronized int getTick() {
        return tick;
    }

    public static void startTicking(int tickDuration) {
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(tickDuration);
                    incrementTick();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }).start();
    }
}

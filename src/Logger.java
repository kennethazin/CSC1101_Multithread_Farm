public class Logger {
    public static synchronized void log(String message) {
        System.out.println("[Tick " + TickManager.getTick() + "] " + message);
    }
}

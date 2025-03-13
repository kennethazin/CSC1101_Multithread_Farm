import java.util.Map;

public class Logger {
    private static final Object lock = new Object();
    
    public static void log(String message) {
        synchronized (lock) {
            System.out.println(message);
        }
    }
    
    public static void logDelivery(long tick, long threadId, Map<AnimalType, Integer> animalCounts) {
        String message = tick + " " + threadId + " Deposit_of_animals : ";
        
        boolean first = true;
        for (Map.Entry<AnimalType, Integer> entry : animalCounts.entrySet()) {
            AnimalType type = entry.getKey();
            int count = entry.getValue();
            
            if (count > 0) {
                if (!first) {
                    message += " ";
                }
                message += type.toString() + "=" + count;
                first = false;
            }
        }
        
        log(message);
    }
    
    public static void logFarmerCollection(long tick, long threadId, int farmerId, long waitedTicks, Map<AnimalType, Integer> animalCounts) {
        String message = tick + " " + threadId + " farmer=" + farmerId + 
                         " collected_animals waited_ticks=" + waitedTicks + ": ";
        
        boolean first = true;
        for (Map.Entry<AnimalType, Integer> entry : animalCounts.entrySet()) {
            if (entry.getValue() > 0) {
                if (!first) {
                    message += " ";
                }
                message += entry.getKey().toString() + "=" + entry.getValue();
                first = false;
            }
        }
        
        log(message);
    }
    
    public static void logBuyerCollection(long tick, long threadId, int buyerId, String fieldType, long waitedTicks) {
        log(tick + " " + threadId + " buyer=" + buyerId + 
            " collected_from_field=" + fieldType + 
            " waited_ticks=" + waitedTicks);
    }
    
    public static void logFarmerAction(long tick, long threadId, int farmerId, String action, String fieldType, int count) {
        log(tick + " " + threadId + " farmer=" + farmerId + 
            " " + action + " : " + fieldType + "=" + count);
    }
    
    public static void logFarmerReturn(long tick, long threadId, int farmerId) {
        log(tick + " " + threadId + " farmer=" + farmerId + " returned_to_enclosure");
    }
}

import java.util.Map;

/**
 * Thread-safe logging utility 
 * Provides synchronised methods to ensure log messages do not write over each other.
 */
public class Logger {
    // Lock object for synchronising console output across threads
    @GuardedBy("lock")
    private static final Object lock = new Object();
    
    /**
     * Logs a message to console in a thread-safe manner.
     * Uses intrinsic lock to prevent output interleaving from multiple threads.
     * 
     * @param message The message to log
     */
    public static void log(String message) {
        synchronized (lock) { // Critical section to ensure atomic console writes
            System.out.println(message);
        }
    }
    
    /**
     * Logs the delivery of animals to the farm.
     * 
     * @param tick the current simulation tick
     * @param threadId the ID of the thread performing the action
     * @param animalCounts A map of animal types and their respective counts
     */
    public static void logDelivery(long tick, long threadId, Map<AnimalType, Integer> animalCounts) {
        String message = tick + " " + threadId + " animal_delivery : ";
        
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
    
    /**
     * Logs the collection of animals by a farmer.
     * 
     * @param tick The current simulation tick
     * @param threadId The ID of the thread performing the action
     * @param farmerId The ID of the farmer
     * @param waitedTicks The number of ticks the farmer waited
     * @param animalCounts A map of animal types and their respective counts
     */
    public static void logFarmerCollection(long tick, long threadId, int farmerId, long waitedTicks, Map<AnimalType, Integer> animalCounts) {
        String message = tick + " " + threadId + " farmer=" + farmerId + " collected_animals waited_ticks=" + waitedTicks + ": ";
        
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
    
    /**
     * Logs the collection of items by a buyer from a field.
     * 
     * @param tick The current simulation tick
     * @param threadId The ID of the thread performing the action
     * @param buyerId The ID of the buyer
     * @param fieldType The type of field from which items are collected
     * @param waitedTicks The number of ticks the buyer waited
     */
    public static void logBuyerCollection(long tick, long threadId, int buyerId, String fieldType, long waitedTicks) {
        log(tick + " " + threadId + " buyer=" + buyerId + 
            " collected_from_field=" + fieldType + 
            " waited_ticks=" + waitedTicks);
    }
    
    /**
     * Logs an action performed by a farmer.
     * 
     * @param tick The current simulation tick
     * @param threadId The ID of the thread performing the action
     * @param farmerId The ID of the farmer
     * @param action The action performed by the farmer
     * @param fieldType The type of field involved in the action
     * @param count The number of items involved in the action
     */
    public static void logFarmerAction(long tick, long threadId, int farmerId, String action, String fieldType, int count) {
        log(tick + " " + threadId + " farmer=" + farmerId + 
            " " + action + " : " + fieldType + "=" + count);
    }
    
    /**
     * Logs the return of a farmer to the enclosure.
     * 
     * @param tick The current simulation tick
     * @param threadId The ID of the thread performing the action
     * @param farmerId The ID of the farmer
     */
    public static void logFarmerReturn(long tick, long threadId, int farmerId) {
        log(tick + " " + threadId + " farmer=" + farmerId + " returned_to_enclosure");
    }
}


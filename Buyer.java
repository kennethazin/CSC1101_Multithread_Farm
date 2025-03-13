import java.util.Random;

/**
 * Represents a buyer who purchases animals from fields.
 * This acts like a consumer in the consumer-producer pattern
 */
public class Buyer implements Runnable {
    private static final int BUY_INTERVAL_TICKS_AVG = 10;
    private static final int COLLECTION_TIME = 1;
    
    private final Farm farm; // Shared resource
    private final TimeManager timeManager; // Shared resource
    private final AnimalType preferredType;
    private static int nextId = 1; // Static counter for generating IDs
    private final int id;
    private final Random random = new Random();
    
    /**
     * Creates a buyer with specific animal type preference.
     * 
     * @param farm Shared farm instance
     * @param timeManager Shared time manager
     * @param preferredType The animal type this buyer purchases
     */
    public Buyer(Farm farm, TimeManager timeManager, AnimalType preferredType) {
        this.farm = farm;
        this.timeManager = timeManager;
        this.preferredType = preferredType;
        // Thread-safe assignment of ID 
        synchronized (Buyer.class) {
            this.id = nextId++;
        }
    }
    
    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                // Random wait time with average of BUY_INTERVAL_TICKS_AVG
                // this is so that buyers don't all try to access resources simultaneously
                long waitTime = Math.round(2.0 * random.nextDouble() * BUY_INTERVAL_TICKS_AVG);
                timeManager.waitTicks(waitTime);
                
                // Choose animal type to buy - each buyer specialises in one type
                AnimalType typeToBuy = preferredType;
                Field field = farm.getField(typeToBuy);
                
                // Record start time for waiting
                long startWaitTick = timeManager.getCurrentTick();
                
                // Take animal from field 
                // Thread-safe due to synchronization in Field class
                Animal animal = field.takeAnimal();
                
                // Calculate wait time
                long waitedTicks = timeManager.getCurrentTick() - startWaitTick;
                
                // Wait for collection time to simulate processing
                timeManager.waitTicks(COLLECTION_TIME);
                
                // Log the purchase
                Logger.logBuyerCollection(timeManager.getCurrentTick(),
                                        Thread.currentThread().getId(), id, 
                                        typeToBuy.toString(), waitedTicks);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

import java.util.Random;

/**
 * Represents a buyer who purchases animals from fields.
 * This acts like a consumer in the consumer-producer pattern
 */
public class Buyer implements Runnable {

    private final Farm farm; // Shared resource
    private final TimeManager timeManager; // Shared resource
    private final AnimalType preferredType;
    private static int nextId = 1; // Static counter for generating IDs
    private final int id;
    private final Random random = new Random();

    /**
     * Creates a buyer with specific animal type preference.
     * 
     * @param farm          Shared farm instance
     * @param timeManager   Shared time manager
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
                // this is so that buyers don't all try to access resources simultaneously
                timeManager.waitTicks(10);

                // 10% chance of buying an animal
                if (Math.random() < 0.10) {
                    AnimalType typeToBuy = preferredType;
                    Field field = farm.getField(typeToBuy);

                    // Record start time for waiting
                    long startWaitTick = timeManager.getCurrentTick();

                    // Take animal from field
                    // Thread-safe due to synchronization in Field class
                    Animal animal = field.takeAnimal();

                    if (animal != null) {
                        // Calculate wait time
                        long waitedTicks = timeManager.getCurrentTick() - startWaitTick;

                        // Wait for collection time to simulate processing
                        timeManager.waitTicks(1);

                        // Log the purchase
                        Logger.logBuyerCollection(timeManager.getCurrentTick(),
                                Thread.currentThread().threadId(), id,
                                typeToBuy.toString(), waitedTicks);
                    }
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

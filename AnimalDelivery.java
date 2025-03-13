import java.util.*;

/**
 * this is responsible for periodically delivering animals to the farm enclosure.
 * producer role in producer-consumer pattern.
 */
public class AnimalDelivery implements Runnable {
    private static final int DELIVERY_INTERVAL_TICKS = 100;
    private static final int ANIMALS_PER_DELIVERY = 10;
    
    private final Farm farm; // Shared resource across threads
    private final TimeManager timeManager; // Shared clock
    private final Random random = new Random();
    @GuardedBy("this") 
    private long lastDeliveryTick = 0;
    
    /**
     * Constructor for animal delivery service.
     * 
     * @param farm The farm to deliver animals to (shared resource)
     * @param timeManager The shared time manager
     */
    public AnimalDelivery(Farm farm, TimeManager timeManager) {
        this.farm = farm;
        this.timeManager = timeManager;
    }
    
    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                long currentTick = timeManager.getCurrentTick();
                
                // Check if its time for a delivery (every 100 ticks)
                // This is a non-blocking check to prevent busy waiting
                if (currentTick - lastDeliveryTick >= DELIVERY_INTERVAL_TICKS) {
                    deliverAnimals();
                    lastDeliveryTick = currentTick;
                }
                
                Thread.sleep(10);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Delivers animals to the enclosure.
     * Synchronize keyword used to safely modify shared state.
     */
    private void deliverAnimals() {
        // Generate a random distribution of animals (total: ANIMALS_PER_DELIVERY)
        Map<AnimalType, Integer> animalCounts = generateRandomAnimalCounts();
        
        // Add animals to enclosure - this call is thread-safe due to synchronization in Farm
        farm.addAnimalsToEnclosure(animalCounts, timeManager.getCurrentTick());
    }
    
    private Map<AnimalType, Integer> generateRandomAnimalCounts() {
        Map<AnimalType, Integer> counts = new HashMap<>();
        AnimalType[] types = AnimalType.values();
        
        // Initialise all counts to 0
        for (AnimalType type : types) {
            counts.put(type, 0);
        }
        
        // Randomly assign animals
        for (int i = 0; i < ANIMALS_PER_DELIVERY; i++) {
            AnimalType type = types[random.nextInt(types.length)];
            counts.put(type, counts.get(type) + 1);
        }
        
        return counts;
    }
}

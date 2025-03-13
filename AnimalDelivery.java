import java.util.*;

/**
 * Responsible for periodically delivering animals to the farm enclosure.
 * Implements a producer role in the producer-consumer pattern.
 */
public class AnimalDelivery implements Runnable {
    private static final int ANIMALS_PER_DELIVERY = 10;

    private final Farm farm; // Shared resource across threads
    private final TimeManager timeManager; // Shared clock
    private final Random random = new Random();
    
    /**
     * Constructor for animal delivery service.
     * 
     * @param farm        The farm to deliver animals to (shared resource)
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

                // Each tick has a 1% (0.01) probability of spawning a delivery
                if (Math.random() < 0.01) {
                    deliverAnimals();
                }

                // Avoid busy waiting: wait a short period before checking again
                Thread.sleep(10);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Delivers animals to the enclosure.
     * Uses synchronized methods in the Farm class for thread safety.
     */
    private void deliverAnimals() {
        // Generate a random distribution of animals (total: ANIMALS_PER_DELIVERY)
        Map<AnimalType, Integer> animalCounts = generateRandomAnimalCounts();

        // Add animals to the enclosure (thread-safe)
        farm.addAnimalsToEnclosure(animalCounts, timeManager.getCurrentTick());
    }

    /**
     * Generates a random distribution of animals totaling ANIMALS_PER_DELIVERY.
     * 
     * @return Map of AnimalType to counts.
     */
    private Map<AnimalType, Integer> generateRandomAnimalCounts() {
        Map<AnimalType, Integer> counts = new HashMap<>();
        AnimalType[] types = AnimalType.values();

        // Initialize all animal types with 0
        for (AnimalType type : types) {
            counts.put(type, 0);
        }

        // Distribute 10 animals randomly among available types
        for (int i = 0; i < ANIMALS_PER_DELIVERY; i++) {
            AnimalType type = types[random.nextInt(types.length)];
            counts.put(type, counts.get(type) + 1);
        }

        return counts;
    }
}

import java.util.*;

public class AnimalDelivery implements Runnable {
    private static final int DELIVERY_INTERVAL_TICKS = 100;
    private static final int ANIMALS_PER_DELIVERY = 10;
    
    private final Farm farm;
    private final TimeManager timeManager;
    private final Random random = new Random();
    private long lastDeliveryTick = 0;
    
    public AnimalDelivery(Farm farm, TimeManager timeManager) {
        this.farm = farm;
        this.timeManager = timeManager;
    }
    
    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                long currentTick = timeManager.getCurrentTick();
                
                // Check if it's time for a delivery (every 100 ticks)
                if (currentTick - lastDeliveryTick >= DELIVERY_INTERVAL_TICKS) {
                    deliverAnimals();
                    lastDeliveryTick = currentTick;
                }
                
                // Small sleep to avoid consuming too much CPU
                Thread.sleep(10);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private void deliverAnimals() {
        // Generate random distribution of animals (total: ANIMALS_PER_DELIVERY)
        Map<AnimalType, Integer> animalCounts = generateRandomAnimalCounts();
        
        // Add animals to farm enclosure
        farm.addAnimalsToEnclosure(animalCounts, timeManager.getCurrentTick());
    }
    
    private Map<AnimalType, Integer> generateRandomAnimalCounts() {
        Map<AnimalType, Integer> counts = new HashMap<>();
        AnimalType[] types = AnimalType.values();
        
        // Initialize all counts to 0
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

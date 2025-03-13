import java.util.*;

public class Farmer implements Runnable {
    private final int id;
    private final Farm farm;
    private final TimeManager timeManager;
    private static final int MAX_ANIMALS = 10;
    private static final int TRAVEL_TIME = 10;
    private static final int STOCKING_TIME_PER_ANIMAL = 1;
    
    public Farmer(int id, Farm farm, TimeManager timeManager) {
        this.id = id;
        this.farm = farm;
        this.timeManager = timeManager;
    }
    
    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                // Wait for animals in the enclosure
                while (farm.isEnclosureEmpty()) {
                    Thread.sleep(100);
                }
                
                // Record wait time
                long startWaitTick = timeManager.getCurrentTick();
                
                // Collect animals from enclosure (up to MAX_ANIMALS)
                Map<AnimalType, List<Animal>> collectedAnimals = farm.collectAnimalsFromEnclosure(MAX_ANIMALS);
                
                // Count collected animals
                int totalCollected = 0;
                Map<AnimalType, Integer> animalCounts = new HashMap<>();
                for (Map.Entry<AnimalType, List<Animal>> entry : collectedAnimals.entrySet()) {
                    int count = entry.getValue().size();
                    if (count > 0) {
                        animalCounts.put(entry.getKey(), count);
                        totalCollected += count;
                    }
                }
                
                if (totalCollected == 0) {
                    continue;
                }
                
                // Log collection
                long waitedTicks = timeManager.getCurrentTick() - startWaitTick;
                Logger.logFarmerCollection(timeManager.getCurrentTick(), Thread.currentThread().getId(), 
                                           id, waitedTicks, animalCounts);
                
                // Now stock the fields with the collected animals
                stockFields(collectedAnimals);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private void stockFields(Map<AnimalType, List<Animal>> collectedAnimals) throws InterruptedException {
        // Sort by number of animals to stock most populated fields first
        List<Map.Entry<AnimalType, List<Animal>>> sortedAnimals = new ArrayList<>(collectedAnimals.entrySet());
        sortedAnimals.sort((e1, e2) -> Integer.compare(e2.getValue().size(), e1.getValue().size()));
        
        // Remove empty lists
        sortedAnimals.removeIf(entry -> entry.getValue().isEmpty());
        
        if (sortedAnimals.isEmpty()) {
            return;
        }
        
        // Current location (0 = enclosure, other values = field index)
        String currentLocation = "enclosure";
        int remainingAnimals = 0;
        
        for (AnimalType type : AnimalType.values()) {
            remainingAnimals += collectedAnimals.get(type).size();
        }
        
        for (Map.Entry<AnimalType, List<Animal>> entry : sortedAnimals) {
            AnimalType type = entry.getKey();
            List<Animal> animals = entry.getValue();
            
            if (animals.isEmpty()) {
                continue;
            }
            
            Field field = farm.getField(type);
            int animalsToStock = animals.size();
            
            // Try to lock the field for stocking
            while (!field.lockForStocking()) {
                Thread.sleep(10);
            }
            
            try {
                // Calculate travel time based on current location and remaining animals
                int travelTime;
                if (currentLocation.equals("enclosure")) {
                    travelTime = TRAVEL_TIME + remainingAnimals;
                } else {
                    travelTime = TRAVEL_TIME + remainingAnimals;
                }
                
                // Wait for travel time
                timeManager.waitTicks(travelTime);
                
                // Log beginning of stocking
                Logger.logFarmerAction(timeManager.getCurrentTick(), Thread.currentThread().getId(),
                                      id, "began_stocking_field", type.toString(), animalsToStock);
                
                // Stock the field
                int stockedCount = 0;
                for (Animal animal : animals) {
                    if (field.addAnimal(animal)) {
                        stockedCount++;
                        timeManager.waitTicks(STOCKING_TIME_PER_ANIMAL);
                    } else {
                        break;  // Field is full
                    }
                }
                
                // Log end of stocking
                Logger.logFarmerAction(timeManager.getCurrentTick(), Thread.currentThread().getId(),
                                      id, "finished_stocking_field", type.toString(), stockedCount);
                
                // Update remaining animals and current location
                remainingAnimals -= stockedCount;
                currentLocation = type.toString();
            } finally {
                field.unlockStocking();
            }
        }
        
        // Return to enclosure if we're not there already
        if (!currentLocation.equals("enclosure")) {
            int returnTime = TRAVEL_TIME + remainingAnimals;
            timeManager.waitTicks(returnTime);
            Logger.logFarmerReturn(timeManager.getCurrentTick(), Thread.currentThread().getId(), id);
        }
    }
}

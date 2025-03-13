import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Represents a farmer who collects animals from enclosure and stocks fields
 * Acts as both consumer (from enclosure) and producer (to fields)
 * coordinaties between multiple shared resources
 */
public class Farmer implements Runnable {

    private final int id;
    private final Farm farm; // Shared resource
    private final TimeManager timeManager; // Shared resource
    private static final int MAX_ANIMALS = 10;
    private static final int TRAVEL_TIME = 10;
    private static final int STOCKING_TIME_PER_ANIMAL = 1;
    private static final int MIN_TICKS_BEFORE_BREAK = 200;
    private static final int MAX_TICKS_BEFORE_BREAK = 300;
    private static final int BREAK_DURATION = 150;

    private long lastBreakTick;
    private int ticksUntilNextBreak;

    /**
     * Creates a farmer with specified ID.
     * 
     * @param id          Unique farmer ID
     * @param farm        Shared farm instance
     * @param timeManager Shared time manager
     */
    public Farmer(int id, Farm farm, TimeManager timeManager) {
        this.id = id;
        this.farm = farm;
        this.timeManager = timeManager;
        this.lastBreakTick = 0;
        // Random break times help prevent farmers from synchronising
        // which could cause monopolisation of resources
        this.ticksUntilNextBreak = generateBreakInterval();
    }

    /**
     * Generates a random interval between breaks
     * Uses ThreadLocalRandom which is thread-safe
     * 
     * @return Random tick count until next break
     */
    private int generateBreakInterval() {
        return ThreadLocalRandom.current().nextInt(MIN_TICKS_BEFORE_BREAK, MAX_TICKS_BEFORE_BREAK + 1);
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                long currentTick = timeManager.getCurrentTick();

                // Check if its time for a break
                // Farmer breaks prevent monopolisation of resources
                if (currentTick - lastBreakTick >= ticksUntilNextBreak) {
                    // Take a break - this releases locks allowing other farmers to work
                    Logger.logFarmerAction(currentTick, Thread.currentThread().threadId(), id, "started_break", "rest",
                            0);
                    timeManager.waitTicks(BREAK_DURATION);

                    // Log the end of break
                    currentTick = timeManager.getCurrentTick();
                    Logger.logFarmerAction(currentTick, Thread.currentThread().threadId(), id, "finished_break", "rest",
                            0);

                    // reset break timer with randomisation to avoid synchronisation
                    lastBreakTick = currentTick;
                    ticksUntilNextBreak = generateBreakInterval();
                }

                // Wait for animals in the enclosure - blocking operation
                long startWaitTick = timeManager.getCurrentTick();
                farm.waitForAnimals();

                // Collect animals from enclosure (up to MAX_ANIMALS)
                // Thread-safe operation due to synchronisation in Farm
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
                Logger.logFarmerCollection(timeManager.getCurrentTick(), Thread.currentThread().threadId(), id,
                        waitedTicks, animalCounts);

                // Stock the fields with the collected animals
                stockFields(collectedAnimals);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Stocks fields with collected animals.
     * Demonstrates complex resource management and coordination.
     * Uses resource ordering and non-blocking attempts to prevent deadlocks.
     * 
     * @param collectedAnimals Map of animal types to lists of animals to stock
     * @throws InterruptedException if thread is interrupted while waiting
     */
    private void stockFields(Map<AnimalType, List<Animal>> collectedAnimals) throws InterruptedException {
        // Sort by no. of animals to stock most populated fields first
        // This prevents starvation of fields with many animals
        List<Map.Entry<AnimalType, List<Animal>>> sortedAnimals = new ArrayList<>(collectedAnimals.entrySet());
        sortedAnimals.sort((e1, e2) -> Integer.compare(e2.getValue().size(), e1.getValue().size()));

        // Remove empty lists for efficiency
        sortedAnimals.removeIf(entry -> entry.getValue().isEmpty());

        if (sortedAnimals.isEmpty()) {
            return;
        }

        // Track current location for simulation accuracy
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

            // Try to lock the field for stocking - non-blocking to prevent deadlock
            while (!field.lockForStocking()) {
                // Small sleep to avoid busy waiting and CPU thrashing
                Thread.sleep(10);
            }

            try {
                // Calculate and wait for travel time
                int travelTime = TRAVEL_TIME + remainingAnimals;
                timeManager.waitTicks(travelTime);

                // Log beginning of stocking
                Logger.logFarmerAction(timeManager.getCurrentTick(), Thread.currentThread().threadId(), id,
                        "began_stocking_field", type.toString(), animalsToStock);

                // Stock the field
                int stockedCount = 0;
                for (Animal animal : animals) {
                    // Thread-safe operation due to the synchronisation in Field
                    if (field.addAnimal(animal)) {
                        stockedCount++;
                        // Simulate time to stock each animal
                        timeManager.waitTicks(STOCKING_TIME_PER_ANIMAL);
                    } else {
                        break;// Field is full
                    }
                }

                // Log end of stocking
                Logger.logFarmerAction(timeManager.getCurrentTick(), Thread.currentThread().threadId(), id,
                        "finished_stocking_field", type.toString(), stockedCount);

                // Update remaining animals and current location
                remainingAnimals -= stockedCount;
                currentLocation = type.toString();
            } finally {
                // Always unlock the field, even if an exception occurs
                // This prevents deadlock if there is an error
                field.unlockStocking();
            }
        }

        // Return to enclosure if were not there already
        if (!currentLocation.equals("enclosure")) {
            int returnTime = TRAVEL_TIME + remainingAnimals;
            timeManager.waitTicks(returnTime);
            Logger.logFarmerReturn(timeManager.getCurrentTick(), Thread.currentThread().threadId(), id);
        }
    }
}

import java.util.*;

/**
 * This coordinates for the farm simulation.
 * Manages enclosure (shared resource) and fields.
 * Uses monitor pattern for thread-safe access to shared resources.
 */
public class Farm {
    @GuardedBy("this") 
    private final Map<AnimalType, Field> fields = new HashMap<>();
    
    @GuardedBy("this")
    private final List<Animal> enclosure = new ArrayList<>();
    
    /**
     * Creates farm with a field capacity.
     * Initialises fields for each animal
     * 
     * @param fieldCapacity max capacity of each field
     */
    public Farm(int fieldCapacity) {
        // Initialise fields with initial animals (default is 5)
        for (AnimalType type : AnimalType.values()) {
            fields.put(type, new Field(type, FarmSimulation.INITIAL_ANIMALS_PER_FIELD, fieldCapacity));
        }
    }
    
    /**
     * Adds animals to the enclosure and notifies waiting farmers
     * Thread safe (synchronized) when modifying the enclosure.
     * Implements the "signal" part of the monitor pattern.
     * 
     * @param animalCounts Map of animal types and counts to add
     * @param tick Current simulation tick for logging
     */
    public synchronized void addAnimalsToEnclosure(Map<AnimalType, Integer> animalCounts, long tick) {
        // Log the delivery
        Logger.logDelivery(tick, Thread.currentThread().getId(), animalCounts);
        
        // Add the animals to the enclosure - critical section
        for (Map.Entry<AnimalType, Integer> entry : animalCounts.entrySet()) {
            AnimalType type = entry.getKey();
            int count = entry.getValue();
            
            for (int i = 0; i < count; i++) {
                enclosure.add(new Animal(type));
            }
        }
        
        // Notify ALL waiting farmers that animals are available
        // Using notifyAll instead of notify to prevent starvation
        notifyAll();
    }
    
    /**
     * Collects animals from the enclosure up to the max count.
     * Thread-safe using mutual exclusion via synchronization.
     * 
     * @param maxCount Maximum number of animals to collect
     * @return Map of animal types to lists of collected animals
     */
    public synchronized Map<AnimalType, List<Animal>> collectAnimalsFromEnclosure(int maxCount) {
        Map<AnimalType, List<Animal>> collected = new HashMap<>();
        for (AnimalType type : AnimalType.values()) {
            collected.put(type, new ArrayList<>());
        }
        
        int count = 0;
        while (!enclosure.isEmpty() && count < maxCount) {
            Animal animal = enclosure.remove(0); // Removes and returns the first element
            collected.get(animal.getType()).add(animal);
            count++;
        }
        
        return collected;
    }
    
    /**
     * Gets a field for the specified animal type.
     * Thread-safe because fields map is final and initialised in constructor
     * 
     * @param type The animal type
     * @return The corresponding field
     */
    public Field getField(AnimalType type) {
        return fields.get(type);
    }
    
    /**
     * Checks if the enclosure is empty
     * Synchronised to ensure consistent view of enclosure state
     * 
     * @return True if enclosure is empty
     */
    public synchronized boolean isEnclosureEmpty() {
        return enclosure.isEmpty();
    }
    
    /**
     * Gets the current number of animals in the enclosure.
     * 
     * @return Number of animals in the enclosure
     */
    public synchronized int getEnclosureSize() {
        return enclosure.size();
    }
    
    /**
     * Waits until animals are available in the enclosure.
     * "wait" part of monitor
     * Thread releases lock and enters wait set until notified.
     * 
     * @throws InterruptedException if thread is interrupted while waiting
     */
    public synchronized void waitForAnimals() throws InterruptedException {
        // Use while loop to guard against sudden wakeups
        while (enclosure.isEmpty()) {
            wait(); // Release lock and wait to be notified
        }
    }
}

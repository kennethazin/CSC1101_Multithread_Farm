import java.util.ArrayList;
import java.util.List;

/**
 * Represents a field for a specific animal type.
 * Implements monitor pattern for thread-safe access to animals
 * Supports multiple producers (farmers) and consumers (buyers)
 */
public class Field {
    private final AnimalType animalType;
    
    @GuardedBy("this")
    private final List<Animal> animals;// protected by intrinsic lock
    
    private final int capacity;
    
    @GuardedBy("this")
    private boolean beingStocked = false;// Flag for exclusive access
    
    /**
     * Creates a field for a specific animal type with initial animals.
     * 
     * @param animalType Type of animal for this field
     * @param initialCount Initial number of animals
     * @param capacity max field capacity
     */
    public Field(AnimalType animalType, int initialCount, int capacity) {
        this.animalType = animalType;
        this.capacity = capacity;
        this.animals = new ArrayList<>();
        
        // Add initial animals
        for (int i = 0; i < initialCount; i++) {
            animals.add(new Animal(animalType));
        }
    }
    
    /**
     * Gets the animal type for this field.
     * Thread-safe because animalType is final.
     * 
     * @return the animal type
     */
    public AnimalType getAnimalType() {
        return animalType;
    }
    
    /**
     * Gets the current number of animals in the field.
     * Synchronized to ensure consistent view of animal list.
     * 
     * @return no. of animals currently in the field
     */
    public synchronized int getCurrentCount() {
        return animals.size();
    }
    
    /**
     * Checks if the field is at maximum capacity.
     * Synchronised to ensure consistent view of animal list.
     * 
     * @return True if field is full
     */
    public synchronized boolean isFull() {
        return animals.size() >= capacity;
    }
    
    /**
     * Checks if the field is empty.
     * Synchronized to ensure consistent view of animal list.
     * 
     * @return True if field is empty
     */
    public synchronized boolean isEmpty() {
        return animals.isEmpty();
    }
    
    /**
     * Attempts to lock the field for stocking.
     * mutual exclusion lock.
     * Uses a flag to indicate lock state.
     * 
     * @return True if lock was acquired, false otherwise
     */
    public synchronized boolean lockForStocking() {
        if (beingStocked) {
            return false; // Non-blocking - returns immediately if already locked
        }
        beingStocked = true;
        return true;
    }
    
    /**
     * unlock the field after stocking and notifies waiting buyers.
     * "signal" part of monitor pattern.
     */
    public synchronized void unlockStocking() {
        beingStocked = false;
        // Notify all waiting buyers - prevents starvation
        notifyAll();
    }
    
    /**
     * check if the field is currently being stocked.
     * 
     * @return True if field is being stocked
     */
    public synchronized boolean isBeingStocked() {
        return beingStocked;
    }
    
    /**
     * Takes an animal from the field, waiting if necessary.
     * "wait" part of monitor pattern.
     * Used by buyers in consumer role.
     * 
     * @return The removed animal
     * @throws InterruptedException if thread is interrupted while waiting
     */
    public synchronized Animal takeAnimal() throws InterruptedException {
        while (isEmpty() || beingStocked) {
            wait(); // Release lock and wait until notified
        }
        // Critical section - modify shared state
        Animal animal = animals.remove(0);
        return animal;
    }
    
    /**
     * Adds an animal to the field if capacity allows and type matches
     * Thread-safe method implementing guarded action
     * Used by farmers in producer role
     * 
     * @param animal The animal to add
     * @return True if animal was added, false otherwise
     */
    public synchronized boolean addAnimal(Animal animal) {
        // Guard condition checks
        if (animal.getType() != this.animalType || isFull()) {
            return false;
        }
        // Critical section - modify shared state
        animals.add(animal);
        // Notify waiting buyers - signal part of monitor pattern
        notifyAll();
        return true;
    }
    
    /**
     * Gets the number of spaces available in the field.
     * Synchronised to ensure consistent view of animal list.
     * 
     * @return Available spaces in the field
     */
    public synchronized int getAvailableSpace() {
        return capacity - animals.size();
    }
    
    @Override
    public String toString() {
        return animalType.toString();
    }
}

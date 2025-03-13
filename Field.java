import java.util.ArrayList;
import java.util.List;

public class Field {
    private final AnimalType animalType;
    private final List<Animal> animals;
    private final int capacity;
    private boolean beingStocked = false;
    
    public Field(AnimalType animalType, int initialCount, int capacity) {
        this.animalType = animalType;
        this.capacity = capacity;
        // Don't pre-allocate the entire capacity which could cause OutOfMemoryError
        this.animals = new ArrayList<>();
        
        // Add initial animals
        for (int i = 0; i < initialCount; i++) {
            animals.add(new Animal(animalType));
        }
    }
    
    public AnimalType getAnimalType() {
        return animalType;
    }
    
    public synchronized int getCurrentCount() {
        return animals.size();
    }
    
    public synchronized boolean isFull() {
        return animals.size() >= capacity;
    }
    
    public synchronized boolean isEmpty() {
        return animals.isEmpty();
    }
    
    public synchronized boolean lockForStocking() {
        if (beingStocked) {
            return false;
        }
        beingStocked = true;
        return true;
    }
    
    public synchronized void unlockStocking() {
        beingStocked = false;
        notifyAll(); // Notify buyers that might be waiting
    }
    
    public synchronized boolean isBeingStocked() {
        return beingStocked;
    }
    
    public synchronized Animal takeAnimal() throws InterruptedException {
        while (isEmpty() || beingStocked) {
            wait();
        }
        Animal animal = animals.remove(0);
        return animal;
    }
    
    public synchronized boolean addAnimal(Animal animal) {
        if (animal.getType() != this.animalType || isFull()) {
            return false;
        }
        animals.add(animal);
        notifyAll(); // Notify waiting buyers
        return true;
    }
    
    public synchronized int getAvailableSpace() {
        return capacity - animals.size();
    }
    
    @Override
    public String toString() {
        return animalType.toString();
    }
}

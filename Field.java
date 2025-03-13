import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

public class Field {
    private final AnimalType animalType;
    private final LinkedBlockingQueue<Animal> animals;
    private final ReentrantLock stockingLock = new ReentrantLock();
    private final int capacity;
    
    public Field(AnimalType animalType, int initialCount, int capacity) {
        this.animalType = animalType;
        this.capacity = capacity;
        this.animals = new LinkedBlockingQueue<>(capacity);
        
        // Add initial animals
        for (int i = 0; i < initialCount; i++) {
            try {
                animals.put(new Animal(animalType));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    public AnimalType getAnimalType() {
        return animalType;
    }
    
    public int getCurrentCount() {
        return animals.size();
    }
    
    public boolean isFull() {
        return animals.size() >= capacity;
    }
    
    public boolean isEmpty() {
        return animals.isEmpty();
    }
    
    public boolean lockForStocking() {
        return stockingLock.tryLock();
    }
    
    public void unlockStocking() {
        stockingLock.unlock();
    }
    
    public boolean isBeingStocked() {
        return stockingLock.isLocked();
    }
    
    public Animal takeAnimal() throws InterruptedException {
        return animals.take();
    }
    
    public boolean addAnimal(Animal animal) {
        if (animal.getType() != this.animalType || isFull()) {
            return false;
        }
        return animals.offer(animal);
    }
    
    public int getAvailableSpace() {
        return capacity - animals.size();
    }
    
    @Override
    public String toString() {
        return animalType.toString();
    }
}

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantLock;

public class Farm {
    private final Map<AnimalType, Field> fields = new ConcurrentHashMap<>();
    private final BlockingQueue<Animal> enclosure = new LinkedBlockingQueue<>();
    private final ReentrantLock enclosureLock = new ReentrantLock();
    
    public Farm(int fieldCapacity) {
        // Initialize fields with initial animals
        for (AnimalType type : AnimalType.values()) {
            fields.put(type, new Field(type, FarmSimulation.INITIAL_ANIMALS_PER_FIELD, fieldCapacity));
        }
    }
    
    public void addAnimalsToEnclosure(Map<AnimalType, Integer> animalCounts, long tick) {
        enclosureLock.lock();
        try {
            // Log the deposit
            Logger.logDelivery(tick, Thread.currentThread().getId(), animalCounts);
            
            // Add the animals to the enclosure
            for (Map.Entry<AnimalType, Integer> entry : animalCounts.entrySet()) {
                AnimalType type = entry.getKey();
                int count = entry.getValue();
                
                for (int i = 0; i < count; i++) {
                    enclosure.add(new Animal(type));
                }
            }
        } finally {
            enclosureLock.unlock();
        }
    }
    
    public Map<AnimalType, List<Animal>> collectAnimalsFromEnclosure(int maxCount) {
        Map<AnimalType, List<Animal>> collected = new HashMap<>();
        for (AnimalType type : AnimalType.values()) {
            collected.put(type, new ArrayList<>());
        }
        
        int totalCollected = 0;
        
        enclosureLock.lock();
        try {
            Iterator<Animal> it = enclosure.iterator();
            while (it.hasNext() && totalCollected < maxCount) {
                Animal animal = it.next();
                collected.get(animal.getType()).add(animal);
                it.remove();
                totalCollected++;
            }
        } finally {
            enclosureLock.unlock();
        }
        
        return collected;
    }
    
    public Field getField(AnimalType type) {
        return fields.get(type);
    }
    
    public boolean isEnclosureEmpty() {
        return enclosure.isEmpty();
    }
    
    public int getEnclosureSize() {
        return enclosure.size();
    }
}

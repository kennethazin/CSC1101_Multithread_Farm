import java.util.*;

public class Farm {
    private final Map<AnimalType, Field> fields = new HashMap<>();
    private final List<Animal> enclosure = new ArrayList<>();
    
    public Farm(int fieldCapacity) {
        // Initialize fields with initial animals
        for (AnimalType type : AnimalType.values()) {
            fields.put(type, new Field(type, FarmSimulation.INITIAL_ANIMALS_PER_FIELD, fieldCapacity));
        }
    }
    
    public synchronized void addAnimalsToEnclosure(Map<AnimalType, Integer> animalCounts, long tick) {
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
        
        // Notify any waiting farmers that animals are available
        notifyAll();
    }
    
    public synchronized Map<AnimalType, List<Animal>> collectAnimalsFromEnclosure(int maxCount) {
        Map<AnimalType, List<Animal>> collected = new HashMap<>();
        for (AnimalType type : AnimalType.values()) {
            collected.put(type, new ArrayList<>());
        }
        
        int totalCollected = 0;
        
        Iterator<Animal> it = enclosure.iterator();
        while (it.hasNext() && totalCollected < maxCount) {
            Animal animal = it.next();
            collected.get(animal.getType()).add(animal);
            it.remove();
            totalCollected++;
        }
        
        return collected;
    }
    
    public Field getField(AnimalType type) {
        return fields.get(type);
    }
    
    public synchronized boolean isEnclosureEmpty() {
        return enclosure.isEmpty();
    }
    
    public synchronized int getEnclosureSize() {
        return enclosure.size();
    }
    
    // Added waitForAnimals method for farmers to wait when enclosure is empty
    public synchronized void waitForAnimals() throws InterruptedException {
        while (enclosure.isEmpty()) {
            wait();
        }
    }
}

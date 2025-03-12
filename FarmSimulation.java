import java.util.*;
import java.util.concurrent.locks.*;

public class FarmSimulation {
    public static void main(String[] args) {
        Farm farm = new Farm(3, 3, 0.01, 200, 300, 150);
        farm.runSimulation();
    }
}
// Farm class
class Farm {
    private static final int TICK_DURATION = 10; // ms per tick
    private static final int ENCLOSURE_CAPACITY = 50;
    private static final int FIELD_CAPACITY = 10;

    private final int numFarmers;
    private final int numBuyers;
    private final double deliveryProbability;
    public final int minBreakTicks;
    private final int maxBreakTicks;
    private final int breakDuration;

    private final Map<String, Queue<String>> enclosure = new HashMap<>();
    private final Map<String, Field> fields = new HashMap<>();
    private final Lock lock = new ReentrantLock(true);
    private final Condition notEmpty = lock.newCondition();
    private final Random random = new Random();
    private int tick = 0;

    // Movement constants
    private static final int BASE_MOVE_TIME = 10; 

    public Farm(int numFarmers, int numBuyers, double deliveryProbability, int minBreakTicks, int maxBreakTicks, int breakDuration) {
        this.numFarmers = numFarmers;
        this.numBuyers = numBuyers;
        this.deliveryProbability = deliveryProbability;
        this.minBreakTicks = minBreakTicks;
        this.maxBreakTicks = maxBreakTicks;
        this.breakDuration = breakDuration;
        
        for (String animalType : new String[] {"COW", "PIG", "SHEEP", "LLAMA", "CHICKEN"}) {
            enclosure.put(animalType, new LinkedList<>());
        }
        
        for (String animalType : new String[] {"COW", "PIG", "SHEEP", "LLAMA", "CHICKEN"}) {
            Queue<String> queue = new LinkedList<>();
            Condition cond = lock.newCondition();
            fields.put(animalType, new Field(queue, cond, animalType));
            
            for (int i = 0; i < 5; i++) {
                queue.add(animalType);
            }
        }
    }
    // starts farm simulation and create/starts threads for farmers, buyers and delivery
    public void runSimulation() {
        for (int i = 1; i <= numFarmers; i++) { // start farm threads
            new Thread(new Farmer(i, this)).start();
        }
        for (int i = 1; i <= numBuyers; i++) { // start buyer threads
            new Thread(new Buyer(i, this)).start();
        }
        new Thread(this::deliverAnimals).start(); // start delivery thread
    }

    private void deliverAnimals() {
        while (true) { 
            sleep(100); 
            lock.lock(); // acquire lock
            try {
                if (random.nextDouble() < deliveryProbability) { 
                    // Create a random distribution of 10 animals
                    Map<String, Integer> animalCounts = generateRandomAnimalDistribution(10);
                    
                    String output = "";
                    for (Map.Entry<String, Integer> entry : animalCounts.entrySet()) {
                        String animalType = entry.getKey();
                        int count = entry.getValue();
                        
                        if (count > 0) {
                            Queue<String> animalQueue = enclosure.get(animalType);
                            for (int i = 0; i < count; i++) {
                                animalQueue.add(animalType);
                            }
                            output += animalType.toLowerCase() + "=" + count + " ";
                        }
                    }
                    
                    System.out.printf("%d Deposit_of_animals : %s\n", tick, output.trim());
                    notEmpty.signalAll(); // signal the waiting threads
                } 
            } finally {
                lock.unlock(); // release lock  
            }
        }
    }

    // Generate a random distribution of animals totaling the specified count
    private Map<String, Integer> generateRandomAnimalDistribution(int totalCount) {
        String[] animalTypes = {"COW", "PIG", "SHEEP", "LLAMA", "CHICKEN"};
        Map<String, Integer> distribution = new HashMap<>();
        
        // Initialise all animal types with 0
        for (String type : animalTypes) {
            distribution.put(type, 0);
        }
        
        // Randomly distribute animals
        for (int i = 0; i < totalCount; i++) {
            String animalType = animalTypes[random.nextInt(animalTypes.length)];
            distribution.put(animalType, distribution.get(animalType) + 1);
        }
        
        return distribution;
    }

    public Map<String, Integer> takeFromEnclosure(int farmerId) {
        Map<String, Integer> takenAnimals = new HashMap<>();
        lock.lock();
        try {
            // Wait until there's at least one animal in the enclosure
            boolean hasAnimals = false;
            int waitStart = tick; // Track wait time
            
            while (!hasAnimals) {
                for (Queue<String> queue : enclosure.values()) {
                    if (!queue.isEmpty()) {
                        hasAnimals = true;
                        break;
                    }
                }
                
                if (!hasAnimals) {
                    System.out.printf("%d farmer=%d Waiting for animals in enclosure...\n", tick, farmerId);
                    notEmpty.await();
                    
                    // Check again after being signaled
                    hasAnimals = false;
                    for (Queue<String> queue : enclosure.values()) {
                        if (!queue.isEmpty()) {
                            hasAnimals = true;
                            break;
                        }
                    }
                }
            }
            
            // Calculate waited ticks
            int waitedTicks = tick - waitStart;
            
            // Take up to 10 animals from the enclosure
            int totalTaken = 0;
            
            String collectedReport = "";
            for (String animalType : new String[] {"COW", "PIG", "SHEEP", "LLAMA", "CHICKEN"}) {
                Queue<String> queue = enclosure.get(animalType);
                int count = 0;
                
                while (!queue.isEmpty() && totalTaken < 10) {
                    queue.poll();
                    count++;
                    totalTaken++;
                }
                
                if (count > 0) {
                    takenAnimals.put(animalType, count);
                    collectedReport += animalType.toLowerCase() + "=" + count + " ";
                }
                
                if (totalTaken >= 10) {
                    break;
                }
            }
            
            if (totalTaken > 0) {
                System.out.printf("%d farmer=%d collected_animals waited_ticks=%d: %s\n", 
                    tick, farmerId, waitedTicks, collectedReport.trim());
            }
            
            return takenAnimals;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return takenAnimals;
        } finally {
            lock.unlock();
        }
    }

    public void stockAnimal(int farmerId, Map<String, Integer> animals) {
        if (animals.isEmpty()) {
            return;
        }
        
        // Calculate total animals being moved
        int totalAnimals = 0;
        for (int count : animals.values()) {
            totalAnimals += count;
        }
        
        // Prioritise animals based on field status and buyer waiting
        Map<String, Integer> prioritisedAnimals = prioritiseAnimalStocking(animals);
        
        // First move: simulate movement from enclosure to first field
        int currentLocation = 0; // 0 = enclosure, 1,2,3,4,5 = fields
        int remainingAnimals = totalAnimals;
        
        for (Map.Entry<String, Integer> entry : prioritisedAnimals.entrySet()) {
            String animalType = entry.getKey();
            int count = entry.getValue();
            
            if (count <= 0) continue;
            
            // Calculate move time: 
            // BASE_MOVE_TIME (10) + 1 tick per animal being moved
            int moveTime = BASE_MOVE_TIME + count;
            
            // Update current location
            currentLocation = getFieldIndex(animalType) + 1;
            
            // Simulate movement
            System.out.printf("%d farmer=%d moved_to_field=%s : %s=%d\n",
                    tick, farmerId, animalType.toLowerCase(), animalType.toLowerCase(), count);
            sleep(moveTime);
            
            // Now attempt to stock the field
            stockField(farmerId, animalType, count);
            
            // Update remaining animals
            remainingAnimals -= count;
        }
        
        // Return to enclosure
        if (currentLocation != 0) {
            int returnTime = BASE_MOVE_TIME;
            System.out.printf("%d farmer=%d Returning to enclosure\n", tick, farmerId);
            sleep(returnTime);
        }
    }

    // Method to prioritise which animals to stock first based on field status
    private Map<String, Integer> prioritiseAnimalStocking(Map<String, Integer> animals) {
        lock.lock();
        try {
            // Create a list of animal types sorted by priority
            List<Map.Entry<String, Integer>> fieldPriorities = new ArrayList<>();
            
            // Check each field
            for (Map.Entry<String, Field> entry : fields.entrySet()) {
                String animalType = entry.getKey();
                Field field = entry.getValue();
                
                // Only consider fields for which we have animals to stock
                if (!animals.containsKey(animalType) || animals.get(animalType) <= 0) {
                    continue;
                }
                
                // Calculate priority based on:
                // 1. Field emptiness (empty fields get higher priority)
                // 2. Field capacity remaining (fields with more space get higher priority)
                int priority = 0;
                
                // Empty field gets high priority
                if (field.animals.isEmpty()) {
                    priority += 100;
                }
                
                // Add remaining capacity as priority
                priority += (FIELD_CAPACITY - field.animals.size()) * 10;
                
                // Store the priority
                fieldPriorities.add(new AbstractMap.SimpleEntry<>(animalType, priority));
            }
            
            // Sort by priority (descending)
            fieldPriorities.sort((a, b) -> b.getValue() - a.getValue());
            
            // Create a new ordered map based on priority
            Map<String, Integer> prioritisedAnimals = new LinkedHashMap<>();
            for (Map.Entry<String, Integer> entry : fieldPriorities) {
                String animalType = entry.getKey();
                prioritisedAnimals.put(animalType, animals.get(animalType));
            }
            
            // If any animals are left, add them at the end
            for (Map.Entry<String, Integer> entry : animals.entrySet()) {
                if (!prioritisedAnimals.containsKey(entry.getKey())) {
                    prioritisedAnimals.put(entry.getKey(), entry.getValue());
                }
            }
            
            return prioritisedAnimals;
        } finally {
            lock.unlock();
        }
    }

    private int getFieldIndex(String animalType) {
        String[] types = {"COW", "PIG", "SHEEP", "LLAMA", "CHICKEN"};
        for (int i = 0; i < types.length; i++) {
            if (types[i].equals(animalType)) {
                return i;
            }
        }
        return -1;
    }
    
    private void stockField(int farmerId, String animalType, int count) {
        lock.lock();
        try {
            Field field = fields.get(animalType);
            if (field != null) {
                System.out.printf("%d farmer=%d began_stocking_field : %s=%d\n", 
                    tick, farmerId, animalType.toLowerCase(), count);
                
                // Stock the animals, respecting field capacity
                int stocked = 0;
                
                // Calculate total animals we can stock (limited by field capacity)
                int toStock = Math.min(count, FIELD_CAPACITY - field.animals.size());
                
                if (toStock > 0) {
                    // Stocking all animals at once, taking 1 tick per animal
                    sleep(toStock); // Takes 1 tick per animal to stock
                    
                    for (int i = 0; i < toStock; i++) {
                        field.animals.add(animalType);
                        stocked++;
                    }
                }
                
                System.out.printf("%d farmer=%d finished_stocking_field : %s=%d\n", 
                    tick, farmerId, animalType.toLowerCase(), stocked);
                
                // Signal waiting buyers
                field.notEmptyCondition.signalAll();
            }
        } finally {
            lock.unlock();
        }
    }

    public void buyAnimal(int buyerId) {
        lock.lock();
        try {
            String[] animalTypes = fields.keySet().toArray(new String[0]);
            if (animalTypes.length == 0) {
                return;
            }
            
            // Choose a random field
            String chosenField = animalTypes[random.nextInt(animalTypes.length)];
            Field field = fields.get(chosenField);
            
            System.out.printf("%d buyer=%d Attempting to buy an animal from Field (%s)\n", 
                tick, buyerId, chosenField.toLowerCase());
            
            // Track wait time for buyers
            int waitStart = tick;
            
            // Wait if field is empty
            while (field.animals.isEmpty()) {
                System.out.printf("%d buyer=%d Field (%s) is empty. Waiting...\n", 
                    tick, buyerId, chosenField.toLowerCase());
                field.notEmptyCondition.await();
            }
            
            // Calculate how long the buyer waited
            int waitedTicks = tick - waitStart;
            
            // Purchase the animal (takes 1 tick)
            sleep(1);
            String animal = field.animals.poll();
            
            System.out.printf("%d buyer=%d collected_from_field=%s waited_ticks=%d\n", 
                tick, buyerId, chosenField.toLowerCase(), waitedTicks);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock();
        }
    }

    public void farmerBreak(int farmerId) {
        int breakTicks = random.nextInt(maxBreakTicks - minBreakTicks + 1) + minBreakTicks;
        System.out.printf("%d farmer=%d Taking a break for %d ticks\n", tick, farmerId, breakTicks);
        sleep(breakDuration);
    }

    public void sleep(int duration) {
        try {
            Thread.sleep(duration);
            tick += duration / TICK_DURATION;
        } catch (InterruptedException ignored) {
        }
    }
}

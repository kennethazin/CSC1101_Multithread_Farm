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
    private static final int BASE_MOVE_TIME = 10; // 10 ticks to move between locations

    public Farm(int numFarmers, int numBuyers, double deliveryProbability, int minBreakTicks, int maxBreakTicks, int breakDuration) {
        this.numFarmers = numFarmers;
        this.numBuyers = numBuyers;
        this.deliveryProbability = deliveryProbability;
        this.minBreakTicks = minBreakTicks;
        this.maxBreakTicks = maxBreakTicks;
        this.breakDuration = breakDuration;
        
        // Initialize the animal types in enclosure
        for (String animalType : new String[] {"COW", "PIG", "SHEEP", "LLAMA", "CHICKEN"}) {
            enclosure.put(animalType, new LinkedList<>());
        }
        
        // Initialize fields with animals
        for (String animalType : new String[] {"COW", "PIG", "SHEEP", "LLAMA", "CHICKEN"}) {
            Queue<String> queue = new LinkedList<>();
            Condition cond = lock.newCondition();
            fields.put(animalType, new Field(queue, cond, animalType));
            
            // Start with 5 animals in each field
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
        new Thread(this::deliverAnimals).start(); //start animal delivery thread, without creating a new class/invoking it
    }

    private void deliverAnimals() {
        while (true) { // infinite loop
            sleep(100);
            lock.lock(); // acquire lock
            try {
                if (random.nextDouble() < deliveryProbability) { // configurable probability
                    // Create a random distribution of 10 animals
                    Map<String, Integer> animalCounts = generateRandomAnimalDistribution(10);
                    
                    StringBuilder deliveryReport = new StringBuilder();
                    deliveryReport.append(String.format("[TICK %d] Deposit_of_animals : ", tick));
                    
                    // Add animals to enclosure based on generated distribution
                    for (Map.Entry<String, Integer> entry : animalCounts.entrySet()) {
                        String animalType = entry.getKey();
                        int count = entry.getValue();
                        
                        if (count > 0) {
                            Queue<String> animalQueue = enclosure.get(animalType);
                            for (int i = 0; i < count; i++) {
                                animalQueue.add(animalType);
                            }
                            deliveryReport.append(animalType.toLowerCase()).append("=").append(count).append(" ");
                        }
                    }
                    
                    System.out.println(deliveryReport.toString().trim());
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
        
        // Initialize all animal types with 0
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

    // Take animals from enclosure - now returns a map of animal types and counts
    public Map<String, Integer> takeFromEnclosure(int farmerId) {
        Map<String, Integer> takenAnimals = new HashMap<>();
        lock.lock();
        try {
            // Wait until there's at least one animal in the enclosure
            boolean hasAnimals = false;
            while (!hasAnimals) {
                for (Queue<String> queue : enclosure.values()) {
                    if (!queue.isEmpty()) {
                        hasAnimals = true;
                        break;
                    }
                }
                
                if (!hasAnimals) {
                    System.out.printf("[TICK %d] [FARMER-%d] Waiting for animals in enclosure...%n", tick, farmerId);
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
            
            // Take up to 10 animals from the enclosure
            int totalTaken = 0;
            StringBuilder takenReport = new StringBuilder();
            takenReport.append(String.format("[TICK %d] [FARMER-%d] collected_animals : ", tick, farmerId));
            
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
                    takenReport.append(animalType.toLowerCase()).append("=").append(count).append(" ");
                }
                
                if (totalTaken >= 10) {
                    break;
                }
            }
            
            if (totalTaken > 0) {
                System.out.println(takenReport.toString().trim());
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
        
        // First move: simulate movement from enclosure to first field
        int currentLocation = 0; // 0 = enclosure, 1,2,3,4,5 = fields
        int remainingAnimals = totalAnimals;
        
        for (Map.Entry<String, Integer> entry : animals.entrySet()) {
            String animalType = entry.getKey();
            int count = entry.getValue();
            
            if (count <= 0) continue;
            
            // Calculate move time from current location to this field
            int moveTime;
            if (currentLocation == 0) {
                // Moving from enclosure to field
                moveTime = BASE_MOVE_TIME + remainingAnimals;
            } else {
                // Moving from one field to another
                moveTime = BASE_MOVE_TIME + remainingAnimals;
            }
            
            // Update current location
            currentLocation = getFieldIndex(animalType) + 1;
            
            // Simulate movement
            System.out.printf("[TICK %d] [FARMER-%d] Moving to field %s with %d animals (%d ticks)...%n",
                    tick, farmerId, animalType, count, moveTime);
            sleep(moveTime);
            
            // Now attempt to stock the field
            stockField(farmerId, animalType, count);
            
            // Update remaining animals
            remainingAnimals -= count;
        }
        
        // Return to enclosure
        if (currentLocation != 0) {
            int returnTime = BASE_MOVE_TIME;
            System.out.printf("[TICK %d] [FARMER-%d] Returning to enclosure (%d ticks)...%n",
                    tick, farmerId, returnTime);
            sleep(returnTime);
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
                System.out.printf("[TICK %d] [FARMER-%d] began_stocking_field : %s=%d%n", 
                    tick, farmerId, animalType.toLowerCase(), count);
                
                // Stock the animals, respecting field capacity
                int stocked = 0;
                for (int i = 0; i < count; i++) {
                    if (field.animals.size() < FIELD_CAPACITY) {
                        sleep(1); // Takes 1 tick to stock each animal
                        field.animals.add(animalType);
                        stocked++;
                    } else {
                        break;
                    }
                }
                
                System.out.printf("[TICK %d] [FARMER-%d] finished_stocking_field : %s=%d%n", 
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
            
            System.out.printf("[TICK %d] [BUYER-%d] Attempting to buy an animal from Field (%s)...%n", 
                tick, buyerId, chosenField);
            
            // Track wait time for buyers
            int waitStart = tick;
            
            // Wait if field is empty
            while (field.animals.isEmpty()) {
                System.out.printf("[TICK %d] [BUYER-%d] Field (%s) is empty. Waiting...%n", 
                    tick, buyerId, chosenField);
                field.notEmptyCondition.await();
            }
            
            // Calculate how long the buyer waited
            int waitedTicks = tick - waitStart;
            
            // Purchase the animal (takes 1 tick)
            sleep(1);
            String animal = field.animals.poll();
            
            System.out.printf("[TICK %d] [BUYER-%d] collected_from_field=%s waited_ticks=%d%n", 
                tick, buyerId, chosenField.toLowerCase(), waitedTicks);
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock();
        }
    }

    public void farmerBreak(int farmerId) {
        int breakTicks = random.nextInt(maxBreakTicks - minBreakTicks + 1) + minBreakTicks;
        System.out.printf("[TICK %d] [FARMER-%d] Taking a break for %d ticks.%n", tick, farmerId, breakTicks);
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

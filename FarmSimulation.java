import java.util.*;
import java.util.concurrent.locks.*;

public class FarmSimulation {
    public static void main(String[] args) {
        Farm farm = new Farm(3, 3, 0.01, 200, 300, 150);
        farm.runSimulation();
    }
}
// Farmer class
class Farm {
    private static final int TICK_DURATION = 10; // ms per tick
    private static final int ENCLOSURE_CAPACITY = 50;
    private static final int FIELD_CAPACITY = 20;

    private final int numFarmers;
    private final int numBuyers;
    private final double deliveryProbability;
    public final int minBreakTicks;
    private final int maxBreakTicks;
    private final int breakDuration;

    private final Queue<String> enclosure = new LinkedList<>();
    private final Map<String, FieldData> fields = new HashMap<>();
    private final Lock lock = new ReentrantLock(true);
    private final Condition notEmpty = lock.newCondition();
    private final Random random = new Random();
    private int tick = 0;

    public Farm(int numFarmers, int numBuyers, double deliveryProbability, int minBreakTicks, int maxBreakTicks, int breakDuration) {
        this.numFarmers = numFarmers;
        this.numBuyers = numBuyers;
        this.deliveryProbability = deliveryProbability;
        this.minBreakTicks = minBreakTicks;
        this.maxBreakTicks = maxBreakTicks;
        this.breakDuration = breakDuration;
        
        // Initialise fields with animals
        for (String animalType : new String[] {"COW", "PIG", "SHEEP", "LLAMA", "CHICKEN"}) {
            Queue<String> queue = new LinkedList<>();
            Condition cond = lock.newCondition();
            fields.put(animalType, new FieldData(queue, cond));
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
                    for (int i = 0; i < 10; i++) {
                        if (enclosure.size() < ENCLOSURE_CAPACITY) {
                            String animal = getRandomAnimal();
                            enclosure.add(animal);
                            System.out.printf("[TICK %d] Delivery added %s to enclosure.%n", tick, animal);
                        } else {
                            break; // stop adding if enclosure is full
                        }
                    }
                    notEmpty.signalAll(); // signal the waiting threads
                } 
            } finally {
                lock.unlock(); // release lock  
            }
        }
    }

    public String takeFromEnclosure(int farmerId) {
        lock.lock();
        try {
            while (enclosure.isEmpty()) {
                notEmpty.await(); 
            }
            String animal = enclosure.poll(); // farmer take animal from front of queue using poll()
            System.out.printf("[TICK %d] [FARMER-%d] Taking %s from enclosure...%n", tick, farmerId, animal);
            return animal;
        } catch (InterruptedException e) {
            return null;
        } finally {
            lock.unlock();
        }
    }

    public void stockAnimal(int farmerId, String animal) {
        lock.lock();
        try {
            FieldData fieldData = fields.get(animal);
            if (fieldData != null && fieldData.animals.size() < FIELD_CAPACITY) {
                System.out.printf("[TICK %d] [FARMER-%d] Moving %s to field...%n", tick, farmerId, animal);
                sleep(10);
                fieldData.animals.add(animal);
                System.out.printf("[TICK %d] [FARMER-%d] Stocked 1 %s in Field (%s).%n", tick, farmerId, animal, animal);
                fieldData.notEmptyCondition.signalAll(); // signal the waiting buyers
            }
        } finally {
            lock.unlock();
        }
    }

    public void buyAnimal(int buyerId) {
        lock.lock();
        try {
            while (true) {
                String[] animalTypes = fields.keySet().toArray(new String[0]); // retrieve keys (animal types) from fields map and convert to array
                if (animalTypes.length == 0) {
                    return;
                }
                String chosenField = animalTypes[random.nextInt(animalTypes.length)]; // choose a random field
                FieldData fieldData = fields.get(chosenField);
                
                System.out.printf("[TICK %d] [BUYER-%d] Attempting to buy an animal from Field (%s)...%n", tick, buyerId, chosenField);
                
                //if empty, wait specifically on this field's condition
                while (fieldData.animals.isEmpty()) {
                    System.out.printf("[TICK %d] [BUYER-%d] Field (%s) is empty. Waiting...%n", tick, buyerId, chosenField);
                    fieldData.notEmptyCondition.await();
            }
            String animal = fieldData.animals.poll();
            System.out.printf("[TICK %d] [BUYER-%d] Purchased 1 %s.%n", tick, buyerId, animal);
            break;
            }
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

    private String getRandomAnimal() {
        String[] animals = {"COW", "PIG", "SHEEP", "LLAMA", "CHICKEN"};
        return animals[random.nextInt(animals.length)];
    }

    public void sleep(int duration) {
        try {
            Thread.sleep(duration);
            tick += duration / TICK_DURATION;
        } catch (InterruptedException ignored) {
        }
    }
}

// Farmer class
class Farmer implements Runnable {
    private final int id;
    private final Farm farm;
    private int workTicks = 0;

    public Farmer(int id, Farm farm) {
        this.id = id;
        this.farm = farm;
    }

    @Override
    public void run() {
        while (true) {
            String animal = farm.takeFromEnclosure(id);
            if (animal != null) {
                farm.stockAnimal(id, animal);
                workTicks += 10;
                if (workTicks >= farm.minBreakTicks) {
                    farm.farmerBreak(id);
                    workTicks = 0;
                }
            }
        }
    }
}

// Buyer class
class Buyer implements Runnable {
    private final int id;
    private final Farm farm;

    public Buyer(int id, Farm farm) {
        this.id = id;
        this.farm = farm;
    }

    @Override
    public void run() {
        while (true) {
            farm.buyAnimal(id);
            farm.sleep(100);
        }
    }
}

// Field Class
class FieldData {
    Queue<String> animals;
    Condition notEmptyCondition;

    FieldData(Queue<String> animals, Condition notEmptyCondition) {
        this.animals = animals;
        this.notEmptyCondition = notEmptyCondition;
    }
}


import java.util.*;
import java.util.concurrent.locks.*;

class Farm {
    private static final int TICK_DURATION = 10; // ms per tick
    private static final int ENCLOSURE_CAPACITY = 50;
    private static final int FIELD_CAPACITY = 20;
    private static final int NUM_FARMERS = 3;
    private static final int NUM_BUYERS = 3;

    private final Queue<String> enclosure = new LinkedList<>();
    private final Map<String, Queue<String>> fields = new HashMap<>();
    private final Lock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();
    private final Random random = new Random();
    private int tick = 0;

    public Farm() {
        // Initialise fields with animals
        fields.put("COW", new LinkedList<>());
        fields.put("PIG", new LinkedList<>());
        fields.put("SHEEP", new LinkedList<>());
        fields.put("LLAMA", new LinkedList<>());
        fields.put("CHICKEN", new LinkedList<>());
    }

    // starts farm simulation and create/starts threads for farmers, buyers and delivery
    public void runSimulation() {
        for (int i = 1; i <= NUM_FARMERS; i++) { // start farm threads
            new Thread(new Farmer(i, this)).start();
        }
        for (int i = 1; i <= NUM_BUYERS; i++) { // start buyer threads
            new Thread(new Buyer(i, this)).start();
        }
        new Thread(this::deliverAnimals).start(); //start animal delivery thread, without creating a new class/invoking it
    }

    private void deliverAnimals() {
        while (true) { // infinite loop
            sleep(100);
            lock.lock(); // acquire lock
            try {
                if (enclosure.size() < ENCLOSURE_CAPACITY) {
                    String animal = getRandomAnimal();
                    enclosure.add(animal);
                    System.out.printf("[TICK %d] Delivery added %s to enclosure.%n", tick, animal);
                    notEmpty.signalAll(); // signal the waiting threads
                }
            } finally {
                lock.unlock(); // release lock even 
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
            Queue<String> field = fields.get(animal); // get the field for the animal
            if (field != null && field.size() < FIELD_CAPACITY) {
                System.out.printf("[TICK %d] [FARMER-%d] Moving %s to field...%n", tick, farmerId, animal);
                sleep(10);
                field.add(animal);
                System.out.printf("[TICK %d] [FARMER-%d] Stocked 1 %s in Field (%s).%n", tick, farmerId, animal, animal);
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
            String chosenField = animalTypes[random.nextInt(animalTypes.length)];
            Queue<String> field = fields.get(chosenField);
            
            System.out.printf("[TICK %d] [BUYER-%d] Attempting to buy an animal from Field (%s)...%n", tick, buyerId, chosenField);
            if (field != null && !field.isEmpty()) {
                String animal = field.poll();
                System.out.printf("[TICK %d] [BUYER-%d] Purchased 1 %s.%n", tick, buyerId, animal);
            } else {
                System.out.printf("[TICK %d] [BUYER-%d] Purchase failed, Field (%s) is empty.%n", tick, buyerId, chosenField);
            }
        } finally {
            lock.unlock();
        }
    }

    private String getRandomAnimal() {
        String[] animals = {"COW", "PIG", "SHEEP", "LLAMA", "CHICKEN"};
        return animals[random.nextInt(animals.length)];
    }

    private void sleep(int duration) {
        try {
            Thread.sleep(duration);
            tick += duration / TICK_DURATION;
        } catch (InterruptedException ignored) {
        }
    }
}

public class FarmSimulation {
    public static void main(String[] args) {
        Farm farm = new Farm();
        farm.runSimulation();
    }
}

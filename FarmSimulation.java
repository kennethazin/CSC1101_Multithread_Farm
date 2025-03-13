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
    private static final int TICK_DURATION = 50; // ms per tick
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

    public Farm(int numFarmers, int numBuyers, double deliveryProbability, int minBreakTicks, int maxBreakTicks,
            int breakDuration) {
        this.numFarmers = numFarmers;
        this.numBuyers = numBuyers;
        this.deliveryProbability = deliveryProbability;
        this.minBreakTicks = minBreakTicks;
        this.maxBreakTicks = maxBreakTicks;
        this.breakDuration = breakDuration;

        for (String animalType : new String[] { "COW", "PIG", "SHEEP", "LLAMA", "CHICKEN" }) {
            enclosure.put(animalType, new LinkedList<>());
        }

        for (String animalType : new String[] { "COW", "PIG", "SHEEP", "LLAMA", "CHICKEN" }) {
            Queue<String> queue = new LinkedList<>();
            Condition cond = lock.newCondition();
            fields.put(animalType, new Field(queue, cond, animalType));

            for (int i = 0; i < 5; i++) {
                queue.add(animalType);
            }
        }
    }

    // starts farm simulation and create/starts threads for farmers, buyers and
    // delivery
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
        String[] animalTypes = { "COW", "PIG", "SHEEP", "LLAMA", "CHICKEN" };
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
                }
            }

            int waitedTicks = tick - waitStart;
            int totalTaken = 0;
            for (String animalType : new String[] { "COW", "PIG", "SHEEP", "LLAMA", "CHICKEN" }) {
                Queue<String> queue = enclosure.get(animalType);
                int count = 0;

                while (!queue.isEmpty() && totalTaken < 10) {
                    queue.poll();
                    count++;
                    totalTaken++;
                }

                if (count > 0) {
                    takenAnimals.put(animalType, count);
                }
            }

            if (totalTaken > 0) {
                System.out.printf("%d farmer=%d collected_animals waited_ticks=%d: %s\n",
                        tick, farmerId, waitedTicks, takenAnimals);
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
        if (animals.isEmpty())
            return;

        // Prioritize which fields to stock first
        Map<String, Integer> prioritizedFields = prioritizeFieldsForStocking(animals);

        int totalAnimals = animals.values().stream().mapToInt(Integer::intValue).sum();
        int remainingAnimals = totalAnimals;
        String previousField = null;

        for (Map.Entry<String, Integer> entry : prioritizedFields.entrySet()) {
            String animalType = entry.getKey();
            int count = entry.getValue();

            if (count <= 0)
                continue;

            // move from enclosure or between fields
            if (previousField == null) {
                int moveTime = 10 + count; // 10 + 1 per animal
                sleep(moveTime);
                tick += moveTime;
            } else {
                int moveTime = 10 + remainingAnimals; // moving between fields: 10 + 1 per animal
                sleep(moveTime);
                tick += moveTime;
            }

            System.out.printf("%d farmer=%d moved_to_field=%s : %s=%d\n",
                    tick, farmerId, animalType.toLowerCase(), animalType.toLowerCase(), count);

            // Now attempt to stock the field
            stockField(farmerId, animalType, count);
            remainingAnimals -= count;
            previousField = animalType; // track the last field the farmer stocked
        }

        // After stocking all animals, return to enclosure
        int returnTime = 10;
        sleep(returnTime);
        tick += returnTime;
        System.out.printf("%d farmer=%d returned_to_enclosure\n", tick, farmerId);
    }

    public Map<String, Integer> prioritizeFieldsForStocking(Map<String, Integer> animals) {
        lock.lock();
        try {
            // Create a priority map for fields
            Map<String, Integer> fieldPriorities = new HashMap<>();

            for (Map.Entry<String, Field> entry : fields.entrySet()) {
                String animalType = entry.getKey();
                Field field = entry.getValue();

                // Only consider fields for which we have animals to stock
                if (!animals.containsKey(animalType) || animals.get(animalType) <= 0) {
                    continue;
                }

                // Calculate priority
                int priority = 0;

                // 1️⃣ Highest priority if the field is empty
                if (field.animals.isEmpty()) {
                    priority += 100;
                }

                // 2️⃣ More priority if buyers are waiting for this field
                priority += field.getWaitingBuyers() * 50; // Each waiting buyer adds priority

                // 3️⃣ Fields with more capacity left get higher priority
                priority += (FIELD_CAPACITY - field.animals.size()) * 10;

                // Store the priority score
                fieldPriorities.put(animalType, priority);
            }

            // Sort by priority in descending order
            return fieldPriorities.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .collect(LinkedHashMap::new, (m, e) -> m.put(e.getKey(), animals.get(e.getKey())), Map::putAll);

        } finally {
            lock.unlock();
        }
    }

    private int getFieldIndex(String animalType) {
        String[] types = { "COW", "PIG", "SHEEP", "LLAMA", "CHICKEN" };
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

                sleep(count);
                tick += count;

                // Stock the animals, respecting field capacity
                int stocked = 0; // Calculate total animals we can stock (limited by field capacity)
                int toStock = Math.min(count, FIELD_CAPACITY - field.animals.size());

                if (toStock > 0) {
                    for (int i = 0; i < toStock; i++) {
                        field.animals.add(animalType);
                        stocked++;
                    }
                }

                System.out.printf("%d farmer=%d finished_stocking_field : %s=%d\n",
                        tick, farmerId, animalType.toLowerCase(), stocked);

                // ensure the farmer returns to the enclosure after stocking
                int returnTime = 10;
                sleep(returnTime);
                tick += returnTime;

                System.out.printf("%d farmer=%d returned_to_enclosure\n", tick, farmerId);

                // notify waiting buyers
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

import java.util.Map;

// Farmer class
public class Farmer implements Runnable {
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
            Map<String, Integer> animals = farm.takeFromEnclosure(id);
            if (!animals.isEmpty()) {
                farm.stockAnimal(id, animals);
                
                // Increase work ticks based on movement and stocking
                int totalAnimals = 0;
                for (int count : animals.values()) {
                    totalAnimals += count;
                }
                
                // Each movement operation counts as work
                workTicks += 10 + totalAnimals;
                
                // Check if farmer needs a break
                if (workTicks >= farm.minBreakTicks) {
                    farm.farmerBreak(id);
                    workTicks = 0;
                }
            }
        }
    }
}
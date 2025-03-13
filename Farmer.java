import java.util.Map;

// Farmer class
public class Farmer implements Runnable {
    private final int id;
    private final Farm farm;
    private int workTicks = 10;

    public Farmer(int id, Farm farm) {
        this.id = id;
        this.farm = farm;
    }

    @Override
    public void run() {
        while (true) {
            Map<String, Integer> animals = farm.takeFromEnclosure(id);
            if (!animals.isEmpty()) {
                int totalAnimals = animals.values().stream().mapToInt(Integer::intValue).sum();

                // ✅ Move time: 10 ticks + 1 per animal
                int moveTime = 10 + totalAnimals;
                workTicks += moveTime;

                farm.stockAnimal(id, animals);

                // ✅ Stocking time: 1 tick per animal
                workTicks += totalAnimals;

                // ✅ Return time (if animals are left): 10 ticks + 1 per remaining animal
                int returnTime = 10 + totalAnimals;
                workTicks += returnTime;

                // ✅ Check if farmer needs a break
                if (workTicks >= farm.minBreakTicks) {
                    farm.farmerBreak(id);
                    workTicks = 0;
                }
            }
        }
    }
}
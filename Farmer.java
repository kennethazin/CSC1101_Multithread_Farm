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
import java.util.Random;

public class FarmSimulation {

    private static final int NUMBER_OF_FARMERS = 5;
    private static final int TICK_DURATION = 10;
    private static final int DELIVERY_INTERVAL = 100;
    private static final int BUY_INTERVAL = 10;

    public static void main(String[] args) {
        Enclosure enclosure = new Enclosure();
        Field[] fields = {
            new Field(Animal.AnimalType.PIG),
            new Field(Animal.AnimalType.COW),
            new Field(Animal.AnimalType.SHEEP),
            new Field(Animal.AnimalType.LLAMA),
            new Field(Animal.AnimalType.CHICKEN)
        };

        // start tick manager
        TickManager.startTicking(TICK_DURATION);

        // start farmers
        for (int i = 0; i < NUMBER_OF_FARMERS; i++) {
            new Farmer(enclosure, fields[i % fields.length]).start();
        }

        // Start buyers
        for (int i = 0; i < 3; i++) {
            new Buyer(fields, BUY_INTERVAL).start();
        }

        // animal deliveries every DELIVERY_INTERVAL ticks
        new Thread(() -> {
            Random random = new Random();
            while (true) {
                try {
                    Thread.sleep(DELIVERY_INTERVAL * TICK_DURATION);
                    for (int i = 0; i < 10; i++) {
                        Animal animal = new Animal(Animal.AnimalType.values()[random.nextInt(5)]);
                        enclosure.addAnimal(animal);
                        Logger.log("Delivery added " + animal);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }).start();
    }
}

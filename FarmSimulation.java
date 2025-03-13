import java.util.*;

/**
 * Main class for the farm simulation.
 * Creates and coordinates all simulation components.
 */
public class FarmSimulation {

    // config constants
    public static final int TICKS_PER_DAY = 1000;
    public static final int DEFAULT_TICK_TIME_MS = 100;
    public static final int INITIAL_ANIMALS_PER_FIELD = 0;
    public static final int DEFAULT_FIELD_CAPACITY = 100;
    public static final int NUM_FARMERS = 5;

    /**
     * Entry point for the simulation.
     * Creates and starts all simulation components.
     * 
     * @param args Command line arguments: [tickTimeMs] [numFarmers] [fieldCapacity]
     */
    public static void main(String[] args) {
        int tickTimeMs = DEFAULT_TICK_TIME_MS;
        int numFarmers = NUM_FARMERS;
        int fieldCapacity = DEFAULT_FIELD_CAPACITY;

        // Parse command line arguments if provided
        if (args.length > 0) {
            try {
                tickTimeMs = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid tick time provided. Using default: " + DEFAULT_TICK_TIME_MS + "ms");
            }

            if (args.length > 1) {
                try {
                    numFarmers = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    System.err.println("Invalid number of farmers provided. Using default: 1");
                }
            }

            if (args.length > 2) {
                try {
                    fieldCapacity = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    System.err.println("Invalid field capacity provided. Using unlimited capacity.");
                }
            }
        }

        // Log simulation settings
        System.out.println("Farm Simulation Started with:");
        System.out.println("- Tick time (ms): " + tickTimeMs);
        System.out.println("- Number of farmers: " + numFarmers);
        System.out.println("- Field capacity: " + fieldCapacity);

        // These objects are shared across multiple threads
        Farm farm = new Farm(fieldCapacity);
        TimeManager timeManager = new TimeManager(tickTimeMs);

        // Start time manager thread
        Thread timeThread = new Thread(timeManager, "TimeManager");
        timeThread.start();

        // Start animal delivery thread
        AnimalDelivery delivery = new AnimalDelivery(farm, timeManager);
        Thread deliveryThread = new Thread(delivery, "AnimalDelivery");
        deliveryThread.start();

        // Start farmer threads
        List<Thread> farmerThreads = new ArrayList<>();
        for (int i = 0; i < numFarmers; i++) {
            Farmer farmer = new Farmer(i + 1, farm, timeManager);
            // Naming threads helps with debugging
            Thread farmerThread = new Thread(farmer, "Farmer-" + (i + 1));
            farmerThreads.add(farmerThread);
            farmerThread.start();
        }

        // Start buyer threads (one per field type)
        List<Thread> buyerThreads = new ArrayList<>();
        for (AnimalType type : AnimalType.values()) {
            Buyer buyer = new Buyer(farm, timeManager, type);
            Thread buyerThread = new Thread(buyer, "Buyer-" + type);
            buyerThreads.add(buyerThread);
            buyerThread.start();
        }
    }
}
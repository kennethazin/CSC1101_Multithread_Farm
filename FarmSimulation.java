import java.util.*;

public class FarmSimulation {
    public static final int TICKS_PER_DAY = 1000;
    public static final int DEFAULT_TICK_TIME_MS = 100; // Can be adjusted
    public static final int INITIAL_ANIMALS_PER_FIELD = 5;
    public static final int DEFAULT_FIELD_CAPACITY = 100; // Added reasonable default
    
    public static void main(String[] args) {
        int tickTimeMs = DEFAULT_TICK_TIME_MS;
        int numFarmers = 3;  // For minimal version
        int fieldCapacity = DEFAULT_FIELD_CAPACITY; // Changed from Integer.MAX_VALUE
        
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
        
        // Create and start the farm simulation
        Farm farm = new Farm(fieldCapacity);
        TimeManager timeManager = new TimeManager(tickTimeMs);
        
        // Start time manager
        Thread timeThread = new Thread(timeManager);
        timeThread.start();
        
        // Start animal delivery service
        AnimalDelivery delivery = new AnimalDelivery(farm, timeManager);
        Thread deliveryThread = new Thread(delivery);
        deliveryThread.start();
        
        // Start farmers
        List<Thread> farmerThreads = new ArrayList<>();
        for (int i = 0; i < numFarmers; i++) {
            Farmer farmer = new Farmer(i + 1, farm, timeManager);
            Thread farmerThread = new Thread(farmer);
            farmerThreads.add(farmerThread);
            farmerThread.start();
        }
        
        // Start buyers (one per field type)
        List<Thread> buyerThreads = new ArrayList<>();
        for (AnimalType type : AnimalType.values()) {
            Buyer buyer = new Buyer(farm, timeManager, type);
            Thread buyerThread = new Thread(buyer);
            buyerThreads.add(buyerThread);
            buyerThread.start();
        }
        
        // Simulation will run indefinitely
        // In a real application, you might want a clean shutdown mechanism
    }
}

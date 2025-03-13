import java.util.Random;

public class Buyer implements Runnable {
    private static final int BUY_INTERVAL_TICKS_AVG = 10;
    private static final int COLLECTION_TIME = 1;
    
    private final Farm farm;
    private final TimeManager timeManager;
    private final AnimalType preferredType;
    private static int nextId = 1;
    private final int id;
    private final Random random = new Random();
    
    public Buyer(Farm farm, TimeManager timeManager, AnimalType preferredType) {
        this.farm = farm;
        this.timeManager = timeManager;
        this.preferredType = preferredType;
        this.id = nextId++;
    }
    
    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                // Random wait time with average of BUY_INTERVAL_TICKS_AVG
                long waitTime = Math.round(2.0 * random.nextDouble() * BUY_INTERVAL_TICKS_AVG);
                timeManager.waitTicks(waitTime);
                
                // Choose animal type to buy
                AnimalType typeToBuy = preferredType;
                Field field = farm.getField(typeToBuy);
                
                // Record start time for waiting
                long startWaitTick = timeManager.getCurrentTick();
                
                // Take animal from field (method already has built-in wait)
                Animal animal = field.takeAnimal();
                
                // Calculate wait time
                long waitedTicks = timeManager.getCurrentTick() - startWaitTick;
                
                // Wait for collection time
                timeManager.waitTicks(COLLECTION_TIME);
                
                // Log the collection
                Logger.logBuyerCollection(timeManager.getCurrentTick(), 
                                        Thread.currentThread().getId(), id, 
                                        typeToBuy.toString(), waitedTicks);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

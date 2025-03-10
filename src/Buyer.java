import java.util.Random;

public class Buyer extends Thread {
    private final Field[] fields;
    private final Random random = new Random();
    private final int buyInterval;

    public Buyer(Field[] fields, int buyInterval) {
        this.fields = fields;
        this.buyInterval = buyInterval;
    }

    @Override
    public void run() {
        try {
            while (true) {
                Field field = fields[random.nextInt(fields.length)];
                Animal animal = field.takeAnimal();
                Logger.log("Buyer bought " + animal);
                Thread.sleep(buyInterval * TickManager.getTick());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

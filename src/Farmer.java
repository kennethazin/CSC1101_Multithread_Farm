public class Farmer extends Thread {
    private final Enclosure enclosure;
    private final Field field;

    public Farmer(Enclosure enclosure, Field field) {
        this.enclosure = enclosure;
        this.field = field;
    }

    @Override
    public void run() {
        try {
            while (true) {
                Animal animal = enclosure.takeAnimal();
                Logger.log("Farmer moving " + animal + " to " + field.type + " field");
                Thread.sleep(10);
                field.addAnimal(animal);
                Thread.sleep(1);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

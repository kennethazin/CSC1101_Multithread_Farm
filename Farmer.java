class Farmer implements Runnable {
    private final int id;
    private final Farm farm;

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
            }
        }
    }
}
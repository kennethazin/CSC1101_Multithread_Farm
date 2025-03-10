class Buyer implements Runnable {
    private final int id;
    private final Farm farm;

    public Buyer(int id, Farm farm) {
        this.id = id;
        this.farm = farm;
    }

    @Override
    public void run() {
        while (true) {
            farm.buyAnimal(id);
            try {
                Thread.sleep(100);
            } catch (InterruptedException ignored) {
            }
        }
    }
}
public class Animal {
    public enum AnimalType { PIG, COW, SHEEP, LLAMA, CHICKEN }

    private final AnimalType type;

    public Animal(AnimalType type) {
        this.type = type;
    }

    public AnimalType getType() {
        return type;
    }

    @Override
    public String toString() {
        return type.name();
    }
}

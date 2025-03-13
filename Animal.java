public class Animal {
    private final AnimalType type;
    
    public Animal(AnimalType type) {
        this.type = type;
    }
    
    public AnimalType getType() {
        return type;
    }
    
    @Override
    public String toString() {
        return type.toString();
    }
}

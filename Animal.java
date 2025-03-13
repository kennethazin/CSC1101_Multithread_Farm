/**
 * Immutable animal entity.
 * Immutability provides thread safety without explicit synchronization.
 */
public class Animal {
    private final AnimalType type; // Immutability ensures thread safety
    
    /**
     * Creates an animal of the specified type.
     * 
     * @param type The type of animal
     */
    public Animal(AnimalType type) {
        this.type = type;
    }
    
    /**
     * Gets the animal type.
     * Thread-safe because field is immutable.
     * 
     * @return The animal type
     */
    public AnimalType getType() {
        return type;
    }
    
    @Override
    public String toString() {
        return type.toString();
    }
}

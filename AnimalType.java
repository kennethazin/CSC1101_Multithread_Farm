public enum AnimalType {
    PIG,
    COW,
    SHEEP,
    LLAMA,
    CHICKEN;
    
    @Override
    public String toString() {
        return name().toLowerCase() + "s"; // Returns "pigs", "cows", etc.
    }
}

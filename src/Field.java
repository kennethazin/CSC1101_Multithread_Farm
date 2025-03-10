import java.util.*;
import java.util.concurrent.locks.*;


public class Field {
    final Animal.AnimalType type;
    private final List<Animal> animals = new ArrayList<>();
    private final Lock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();

    public Field(Animal.AnimalType type) {
        this.type = type;
    }

    public void addAnimal(Animal animal) {
        lock.lock();
        try {
            animals.add(animal);
            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    public Animal takeAnimal() throws InterruptedException {
        lock.lock();
        try {
            while (animals.isEmpty()) {
                notEmpty.await();
            }
            return animals.remove(0);
        } finally {
            lock.unlock();
        }
    }

    public boolean isEmpty() {
        lock.lock();
        try {
            return animals.isEmpty();
        } finally {
            lock.unlock();
        }
    }
}

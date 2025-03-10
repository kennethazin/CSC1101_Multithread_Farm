import java.util.*;
import java.util.concurrent.locks.*;

public class Enclosure {
    private final Queue<Animal> animals = new LinkedList<>();
    private final Lock lock = new ReentrantLock();
    private final Condition notEmpty = lock.newCondition();

    public void addAnimal(Animal animal) {
        lock.lock();
        try {
            animals.offer(animal);
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
            return animals.poll();
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

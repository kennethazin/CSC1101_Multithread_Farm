import java.util.Queue;
import java.util.concurrent.locks.Condition;

public class Field {
    Queue<String> animals;
    Condition notEmptyCondition;
    String fieldName;
    private int waitingBuyers = 0; // Track waiting buyers

    public Field(Queue<String> animals, Condition notEmptyCondition, String fieldName) {
        this.animals = animals;
        this.notEmptyCondition = notEmptyCondition;
        this.fieldName = fieldName;
    }

    public synchronized void addWaitingBuyer() {
        waitingBuyers++;
    }

    public synchronized void removeWaitingBuyer() {
        if (waitingBuyers > 0) waitingBuyers--;
    }

    public synchronized int getWaitingBuyers() {
        return waitingBuyers;
    }
}

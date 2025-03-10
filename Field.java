import java.util.Queue;
import java.util.concurrent.locks.Condition;

// Field Class
public class Field {
    Queue<String> animals;
    Condition notEmptyCondition;

    public Field(Queue<String> animals, Condition notEmptyCondition) {
        this.animals = animals;
        this.notEmptyCondition = notEmptyCondition;
    }
}


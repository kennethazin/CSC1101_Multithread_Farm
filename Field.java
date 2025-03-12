import java.util.Queue;
import java.util.concurrent.locks.Condition;

// Field Class
public class Field {
    Queue<String> animals;
    Condition notEmptyCondition;
    String fieldName;

    public Field(Queue<String> animals, Condition notEmptyCondition, String fieldName) {
        this.animals = animals;
        this.notEmptyCondition = notEmptyCondition;
        this.fieldName = fieldName;
    }
}


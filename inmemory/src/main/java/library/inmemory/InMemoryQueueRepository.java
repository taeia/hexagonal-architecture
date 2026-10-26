package library.inmemory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import library.domain.Book;
import library.domain.Queue;
import library.ports.driven.QueueRepository;

/**
 * One queue per book, kept whole. A book without a stored queue gets a new, empty one
 * from {@code of}; it is kept only once somebody saves it.
 */
public class InMemoryQueueRepository implements QueueRepository {

    private final Map<Book, Queue> queues = new LinkedHashMap<>();

    @Override
    public Queue of(Book book) {
        Queue queue = queues.get(book);
        return queue != null ? queue : new Queue(book);
    }

    @Override
    public List<Queue> all() {
        return new ArrayList<>(queues.values());
    }

    @Override
    public void save(Queue queue) {
        queues.put(queue.book(), queue);
    }

    public void clear() {
        queues.clear();
    }
}

package library.domain;

import java.util.ArrayList;
import java.util.List;

import library.ports.driven.QueueRepository;

class FakeQueueRepository implements QueueRepository {

    private final List<Queue> queues = new ArrayList<>();
    final List<Queue> saved = new ArrayList<>();

    FakeQueueRepository(Queue... queues) {
        this.queues.addAll(List.of(queues));
    }

    @Override
    public Queue of(Book book) {
        for (Queue queue : queues) {
            if (queue.book() == book) {
                return queue;
            }
        }
        return new Queue(book);
    }

    @Override
    public List<Queue> all() {
        return List.copyOf(queues);
    }

    @Override
    public void save(Queue queue) {
        saved.add(queue);
    }
}

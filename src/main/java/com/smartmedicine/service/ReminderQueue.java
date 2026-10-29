package com.smartmedicine.service;

import com.smartmedicine.model.DoseRecord;
import com.smartmedicine.model.DoseStatus;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.PriorityQueue;
import java.util.Set;

/**
 * Scheduling algorithm: a min-heap (PriorityQueue) ordered by scheduled date-time. Polling removes every dose whose
 * time has arrived (O(log n) each). A HashSet of already-fired dose ids guarantees one alarm per dose, and doses that
 * are already older than the grace period are skipped silently (no late alarm storm after the app starts).
 */
public final class ReminderQueue {
    private final PriorityQueue<DoseRecord> heap = new PriorityQueue<>(
            Comparator.comparing(DoseRecord::scheduledDateTime).thenComparingLong(DoseRecord::id));
    private final Set<Long> fired = new HashSet<>();

    /** Replaces the queue contents. Taken/missed doses and doses that already fired are not queued. */
    public synchronized void rebuild(Collection<DoseRecord> doses) {
        heap.clear();
        for (DoseRecord d : doses) {
            if (d.status() == DoseStatus.PENDING && !fired.contains(d.id())) heap.add(d);
        }
    }

    /** Returns doses that must ring now (each dose at most once, in time order). */
    public synchronized List<DoseRecord> pollDue(LocalDateTime now, Duration grace) {
        List<DoseRecord> due = new ArrayList<>();
        while (!heap.isEmpty() && !heap.peek().scheduledDateTime().isAfter(now)) {
            DoseRecord d = heap.poll();
            boolean firstTime = fired.add(d.id());
            boolean withinGrace = !d.scheduledDateTime().plus(grace).isBefore(now);
            if (firstTime && withinGrace) due.add(d);
        }
        return due;
    }

    public synchronized Optional<DoseRecord> peekNext() { return Optional.ofNullable(heap.peek()); }

    public synchronized List<DoseRecord> snapshot() {
        List<DoseRecord> list = new ArrayList<>(heap);
        list.sort(heap.comparator());
        return list;
    }

    public synchronized void clearFired() { fired.clear(); }
    public synchronized void clear() { heap.clear(); }
    public synchronized int size() { return heap.size(); }
}

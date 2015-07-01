package mpicbg.stitching.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * A queue that has a fixed size.
 * 
 * @author gabriel
 *
 * @param <T>
 */
public class FixedSizePriorityQueue<T extends Comparable<T>> {

    private final TreeSet<T> treeSet;
    private final int capacity;

    public FixedSizePriorityQueue(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("Capacity must be at least 1");
        } else {
            this.capacity = capacity;
            treeSet = new TreeSet<T>();
        }
    }

    /**
     * 
     * @return the Content of the Queue as an List.
     */
    public List<T> asList() {
        return new ArrayList<T>(treeSet);
    }

    /**
     * Tries to insert an element into the queue, if the queue is at maximum
     * capacity the smallest element in the queue will be removed to make room
     * for the new element. Provided the new Element is larger than it.
     * 
     * @param e
     *            the element to insert.
     * @return if the insertion was successful.
     */
    public boolean add(T e) {
        // there is space left
        if (treeSet.size() < capacity) {
            return treeSet.add(e);
        }

        // no space left
        if (e.compareTo(treeSet.first()) < 0) {
            // element is too low
            return false;
        } else {
            // make room for new element
            treeSet.pollFirst();
            return treeSet.add(e);
        }
    }

    /**
     * Removes the specified element from this queue if it is present.
     *
     * @param e
     *            element to be removed from this set, if present
     * @return {@code true} if this set contained the specified element
     */
    public boolean remove(T e) {
        return treeSet.remove(e);
    }
}

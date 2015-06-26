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
        this.capacity = capacity;
        treeSet = new TreeSet<>();
    }

    public List<T> getAllElements() {
        return new ArrayList<>(treeSet);
    }

    /**
     * Tries to insert an element into the queue, if the queue is at maximum
     * capacity the smallest element in the queue will be removed to make room
     * for the new element. Provided the new Element is larger than it.
     * 
     * @param element
     *            the element to insert.
     * @return if the insertion was successful.
     */
    public boolean add(T element) {
        // there is space left
        if (treeSet.size() < capacity) {
            return treeSet.add(element);
        }

        // no space left
        if (element.compareTo(treeSet.first()) < 0) {
            // element is too low
            return false;
        } else {
            // make room for new element
            treeSet.pollFirst();
            return treeSet.add(element);
        }
    }
}

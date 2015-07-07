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

    private final TreeSet<T> m_treeSet;
    private final int m_capacity;

    public FixedSizePriorityQueue(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("Capacity must be at least 1");
        } else {
            this.m_capacity = capacity;
            m_treeSet = new TreeSet<T>();
        }
    }

    /**
     * 
     * @return the Content of the Queue as an List.
     */
    public List<T> asList() {
        return new ArrayList<T>(m_treeSet);
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
        if (m_treeSet.size() < m_capacity) {
            return m_treeSet.add(e);
        }

        // no space left
        if (e.compareTo(m_treeSet.first()) <= 0) {
            // element is too low
            return false;
        } else {
            boolean result = m_treeSet.add(e);
            if (result) {
                // remove lowest element
                m_treeSet.pollFirst();
            }
            return result;
        }
    }

    /**
     * @return the size of the queue.
     */
    public int size() {
        return m_treeSet.size();
    }
}

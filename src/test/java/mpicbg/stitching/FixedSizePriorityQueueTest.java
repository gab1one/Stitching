package mpicbg.stitching;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import mpicbg.stitching.utils.FixedSizePriorityQueue;

public class FixedSizePriorityQueueTest {

    private FixedSizePriorityQueue<Integer> m_queue;

    @Before
    public void setup() {
        m_queue = new FixedSizePriorityQueue<Integer>(5);
    }

    @Test
    public void maxSizeTest() {
        for (int i = 0; i < 5; i++) {
            assertTrue("add failed!", m_queue.add(i));
        }

        assertFalse("add did not fail!", m_queue.add(0));
        assertEquals("wrong size !", m_queue.size(), 5);
        assertTrue("add failed", m_queue.add(5));
        assertEquals("wrong size after adding.", m_queue.size(), 5);
        assertEquals(m_queue.asList().toArray(new Integer[5]),
                new Integer[] { 1, 2, 3, 4, 5 });

        assertFalse("add did not fail on duplicate", m_queue.add(5));
        assertEquals("wrong size!", m_queue.size(), 5);
        assertEquals(m_queue.asList().toArray(new Integer[5]),
                new Integer[] { 1, 2, 3, 4, 5 });

    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorZeroTest() {
        m_queue = new FixedSizePriorityQueue<Integer>(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructorNegativeTest() {
        m_queue = new FixedSizePriorityQueue<Integer>(-1);
    }

}

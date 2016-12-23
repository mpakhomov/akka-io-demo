package com.mpakhomov.collection;

/**
 * Based on CircularFifoQueue<E> from org.apache.commons.collections4
 *
 * Created with IntelliJ IDEA.
 * User: mpakhomov
 * Date: 8/13/13
 * Time: 11:57 AM
 * To change this template use File | Settings | File Templates.
 */
import java.util.*;

/**
 * CircularBuffer is a first-in first-out queue with a fixed size that
 * replaces its oldest element if full. It also provides random access
 * to its elements in constant O(1) time.
 */
public class CircularBuffer<E> extends AbstractCollection<E>
        implements Queue<E> {

    /** Underlying storage array. */
    private E[] elements;

    /** Array index of first (oldest) queue element. */
    private int start = 0;

    /**
     * Index mod maxElements of the array position following the last queue
     * element.  Queue elements start at elements[start] and "wrap around"
     * elements[maxElements-1], ending at elements[decrement(end)].
     * For example, elements = {c,a,b}, start=1, end=1 corresponds to
     * the queue [a,b,c].
     */
    private int end = 0;

    /** Capacity of the queue. */
    private final int maxElements;

    /** Flag to indicate if the queue is currently full. */
    private transient boolean full = false;

    /**
     * Increments the internal index.
     *
     * @param index  the index to increment
     * @return the updated index
     */
    private int increment(int index) {
        index++;
        if (index >= maxElements) {
            index = 0;
        }
        return index;
    }

    /**
     * Decrements the internal index.
     *
     * @param index  the index to decrement
     * @return the updated index
     */
    private int decrement(int index) {
        index--;
        if (index < 0) {
            index = maxElements - 1;
        }
        return index;
    }


    private class CircularBufferIterator implements Iterator<E> {

        private int index = start;
        private int lastReturnedIndex = -1;
        private boolean isFirst = full;

        @Override
        public boolean hasNext() {
            return isFirst || index != end;
        }

        @Override
        public E next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            isFirst = false;
            lastReturnedIndex = index;
            index = increment(index);
            return elements[lastReturnedIndex];
        }

        @Override
        public void remove() {
            if (lastReturnedIndex == -1) {
                throw new IllegalStateException();
            }

            // First element can be removed quickly
            if (lastReturnedIndex == start) {
                CircularBuffer.this.remove();
                lastReturnedIndex = -1;
                return;
            }

            int pos = lastReturnedIndex + 1;
            if (start < lastReturnedIndex && pos < end) {
                // shift in one part
                System.arraycopy(elements, pos, elements, lastReturnedIndex, end - pos);
            } else {
                // Other elements require us to shift the subsequent elements
                while (pos != end) {
                    if (pos >= maxElements) {
                        elements[pos - 1] = elements[0];
                        pos = 0;
                    } else {
                        elements[decrement(pos)] = elements[pos];
                        pos = increment(pos);
                    }
                }
            }

            lastReturnedIndex = -1;
            end = decrement(end);
            elements[end] = null;
            full = false;
            index = decrement(index);
        }
    }

    /**
     * Constructor that creates a queue with the default size of 32.
     */
    public CircularBuffer() {
        this(32);
    }

    /**
     * Constructor that creates a queue with the specified size.
     *
     * @param size  the size of the buffer (cannot be changed)
     * @throws IllegalArgumentException  if the size is &lt; 1
     */
    @SuppressWarnings("unchecked")
    public CircularBuffer(final int size) {
        if (size <= 1) {
            throw new IllegalArgumentException("The size must be greater than 1");
        }
        elements = (E[]) new Object[size];
        maxElements = elements.length;
    }


    @Override
    public Iterator<E> iterator() {
        return new CircularBufferIterator();
    }

    /**
     * Returns the number of elements stored in the queue.
     *
     * @return this queue's size
     */
    @Override
    public int size() {
        int size = 0;

        if (end < start) {
            size = maxElements - start + end;
        } else if (end == start) {
            size = full ? maxElements : 0;
        } else {
            size = end - start;
        }

        return size;
    }

    /**
     * Adds the given element to this queue. If the queue is full, the least recently added
     * element is discarded so that a new element can be inserted.
     *
     * @param element  the element to add
     * @return true, always
     * @throws NullPointerException  if the given element is null
     */
    @Override
    public boolean offer(E element) {
        if (null == element) {
            throw new NullPointerException("Attempted to add null object to queue");
        }

        if (isAtFullCapacity()) {
            remove();
        }

        elements[end++] = element;

        if (end >= maxElements) {
            end = 0;
        }

        if (end == start) {
            full = true;
        }

        return true;
    }

    @Override
    public E remove() {
        if (isEmpty()) {
            throw new NoSuchElementException("queue is empty");
        }

        final E element = elements[start];
        if (element != null) {
            elements[start++] = null;

            if (start >= maxElements) {
                start = 0;
            }
            full = false;
        }
        return element;
    }

    @Override
    public E poll() {
        if (isEmpty()) {
            return null;
        } else {
            return remove();
        }
    }

    @Override
    public E element() {
        if (isEmpty()) {
            throw new NoSuchElementException("queue is empty");
        } else {
            return peek();
        }
    }

    @Override
    public E peek() {
        if (isEmpty()) {
            return null;
        }
        return elements[start];
    }

    /**
     * Returns the element at the specified position in this queue.
     *
     * @param index the position of the element in the queue
     * @return the element at position {@code index}
     * @throws NoSuchElementException if the requested position is outside the range [0, size)
     */
    public E get(final int index) {
        final int sz = size();
        if (index < 0 || index >= sz) {
            throw new NoSuchElementException(
                    String.format("The specified index (%1$d) is outside the available range [0, %2$d)",
                            Integer.valueOf(index), Integer.valueOf(sz)));
        }

        final int idx = (start + index) % maxElements;
        return elements[idx];
    }

    private boolean isAtFullCapacity() {
        return size() == maxElements;
    }

    public static void main(String[] args) {
        CircularBuffer<Integer> buf = new CircularBuffer<Integer>(5);
        buf.offer(10);
        buf.offer(7);
        buf.offer(3);
        buf.offer(5);
        buf.offer(4);
        for (int i = 0; i < buf.size(); i++) {
            System.out.printf("buff[%d] = %d\n", i, buf.get(i));
        }
        buf.offer(9);
        for (int i = 0; i < buf.size(); i++) {
            System.out.printf("buff[%d] = %d\n", i, buf.get(i));
        }
        Integer element = null;
        while ( (element = buf.poll()) != null) {
            System.out.println(element);
        }
    }
}

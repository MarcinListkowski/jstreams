package com.amoerie.jstreams;

import com.amoerie.jstreams.functions.Filter;
import com.amoerie.jstreams.functions.Mapper;
import com.amoerie.jstreams.functions.Reducer;

import java.util.*;

/**
 * Represents a collection of elements that are not known at construction time
 * This is a wrapper around the Iterator class, providing more functional methods than is standard provided by Java,
 * by combining some classic functional paradigms such as map and flatMap.
 *
 * @param <E> the type of each element in the stream
 */
public abstract class Stream<E> implements Iterable<E> {

    protected Stream() {
    }

    /**
     * Creates a new empty stream, containing no elements
     *
     * @param <E> the type of the elements of this stream
     * @return a new empty stream containing no elements
     */
    public static <E> Stream<E> empty() {
        return new EmptyStream<E>();
    }

    /**
     * Creates a new singleton stream, containing exactly one element
     *
     * @param element the single element
     * @param <E>     the type of the single element
     * @return a new stream containing exactly one element
     */
    public static <E> Stream<E> singleton(final E element) {
        return new SingletonStream<E>(element);
    }

    /**
     * Creates a new stream from the provided array of elements
     *
     * @param array the array of elements
     * @param <E>   the type of the elements
     * @return a new stream containing the elements of the array
     */
    public static <E> Stream<E> create(final E[] array) {
        if (array == null)
            throw new IllegalArgumentException("Unable to create a stream from this array because it is null!");
        return create(Arrays.asList(array));
    }

    /**
     * Creates a new stream from the provided iterable
     * This is a lazy operation, it does not consume the iterable until a consuming operation is called, such as toList()
     *
     * @param iterable an iterable containing elements
     * @param <E>      the type of an element
     * @return a new stream containing the elements of the iterable
     */
    public static <E> Stream<E> create(final Iterable<E> iterable) {
        if(iterable == null)
            throw new IllegalArgumentException("Unable to create a stream from this iterable because it is null!");
        return new IterableStream<E>(iterable);
    }

    /**
     * Casts every element of this stream to another class
     *
     * @param clazz the class to cast to
     * @param <C>   the type of the class to cast to
     * @return a new stream containing every element casted to another class
     */
    public <C> Stream<C> cast(final Class<C> clazz) {
        if (clazz == null)
            throw new IllegalArgumentException("Unable to cast this stream because the class to cast to is null!");
        return new CastStream<E, C>(this, clazz);
    }

    /**
     * Concatenates this stream with another stream
     *
     * @param other the other stream to concatenate with
     * @return a new stream containing all the elements of this stream and the other stream
     */
    public Stream<E> concat(final Stream<E> other) {
        return new FlatStream<E>(create(Arrays.asList(this, other)));
    }

    /**
     * Filters the elements of this stream with the given filter.
     *
     * @param filter the predicate that returns true or false for a given element
     * @return a new stream containing only the elements that satisfied the filter
     */
    public Stream<E> filter(final Filter<E> filter) {
        if (filter == null)
            throw new IllegalArgumentException("Unable to filter this stream because the filter is null!");
        return new FilteredStream<E>(this, filter);
    }

    /**
     * Gets the first element of this stream
     *
     * @return the first element of this stream or null if the stream is empty
     */
    public E first() {
        Iterator<E> iterator = iterator();
        return iterator.hasNext() ? iterator.next() : null;
    }

    /**
     * Maps each element of this stream to a separate stream, and then flattens the result to one single stream
     *
     * @param mapper the function that turns one element into a stream of values
     * @param <R>    the type of one mapped element
     * @return a new stream containing all elements of all the jstreams the mapper function created
     */
    public <R> Stream<R> flatMap(final Mapper<E, Stream<R>> mapper) {
        if (mapper == null)
            throw new IllegalArgumentException("Unable to flatMap this stream because the mapper is null!");
        return new FlatStream<R>(new MappedStream<E, Stream<R>>(this, mapper));
    }

    /**
     * Gets the last element of this stream
     *
     * @return the last element of this stream or null if the stream is empty
     */
    public E last() {
        E last = null;
        for (E e : this)
            last = e;
        return last;
    }

    /**
     * Calculates the amount of elements in this stream
     *
     * @return the length of this stream
     */
    public int length() {
        return reduce(new Reducer<E, Integer>() {
            @Override
            public Integer reduce(Integer length, E element) {
                return length + 1;
            }
        }, 0);
    }

    /**
     * Filters out the elements that are of a certain class
     *
     * @param clazz the class that should be filtered out
     * @param <C>   the type of the class
     * @return a new stream containing only the elements of the provided class
     */
    public <C> Stream<C> ofClass(final Class<C> clazz) {
        return this.filter(new Filter<E>() {
            @Override
            public boolean apply(E e) {
                return clazz.isInstance(e);
            }
        }).cast(clazz);
    }

    /**
     * Maps each element of this stream to another value
     *
     * @param mapper the function that takes an element as its input and returns any other value
     * @param <R>    the type of the element after it has been mapped
     * @return a new stream containing the mapped elements
     */
    public <R> Stream<R> map(final Mapper<E, R> mapper) {
        if (mapper == null)
            throw new IllegalArgumentException("Unable to map this stream because the mapper is null!");
        return new MappedStream<E, R>(this, mapper);
    }

    /**
     * Reduces this stream to a single value by repeatedly applying the same reduction operator to the
     * current value and the next element.
     * For example, to reduce a stream of integers to a sum:
     * numbers.reduce((sum, number) => sum + number, 0)
     *
     * @param reducer the reduction function that turns the current value and the next element into the next value
     * @param <R>     the type of the result of the reduced stream
     * @return the final value after reducing every element
     */
    public <R> R reduce(final Reducer<E, R> reducer, final R initialValue) {
        if (reducer == null)
            throw new IllegalArgumentException("Unable to reduce this stream because the reducer is null!");
        R accumulator = initialValue;
        for (E e : this) {
            accumulator = reducer.reduce(accumulator, e);
        }
        return accumulator;
    }

    /**
     * Skips a certain number of elements of this stream
     *
     * @param number the number of items to skip
     * @return a new stream containing the remaining elements of this stream after skipping a certain number of elements
     */
    public Stream<E> skip(final int number) {
        if (number < 0)
            throw new IllegalArgumentException("Unable to skip a number of elements of this stream because the number is negative!");
        return new SkipStream<E>(this, number);
    }

    /**
     * Determines whether any of the elements in this stream satisfy the given predicate
     * @param filter the filter that returns true or false for any given element
     * @return true if one of the elements satisfied the predicate or false otherwise
     */
    public boolean some(final Filter<E> filter) {
        if(filter == null)
            throw new IllegalArgumentException("Unable to determine if some element satisfies this filter because the filter is null!");
        return this.filter(filter).first() != null;
    }

    /**
     * Sorts this stream using the provided comparator. This operator is lazy but greedy, meaning that it will wait as long as possible to actually materialize your stream
     * to sort it. Once you start iterating over the elements, it will sort just in time.
     * Note that multiple iterations will also a separate sort every time.
     *
     * @param comparator the comparator to use as the basis for the sorting
     * @return a new stream containing all elements of this stream in the order as specified by the comparator
     */
    public Stream<E> sort(final Comparator<E> comparator) {
        if (comparator == null) throw new IllegalArgumentException("Unable to sort stream, comparator cannot be null!");
        return new SortedStream<E>(this, comparator);
    }

    /**
     * Sorts this stream based on a property of each element, provided that that property implements Comparable.
     *
     * @param mapper the function that extracts a value from an element so it can be used as the basis for the comparison
     * @param <T>              the type of the property that is the basis for the comparison
     * @return a new stream containing all elements of this stream sorted by the given property
     */
    public <T extends Comparable<T>> Stream<E> sortBy(final Mapper<E, T> mapper) {
        if (mapper == null)
            throw new IllegalArgumentException("Unable to sort stream because the mapper is null!");
        return sort(new Comparator<E>() {
            @Override
            public int compare(E left, E right) {
                return mapper.map(left).compareTo(mapper.map(right));
            }
        });
    }

    /**
     * Sorts this stream descendingly based on a mapped value of each element, provided that that value implements Comparable.
     * @param mapper the function that extracts a value from an element so it can be used as the basis for the comparison
     * @param <T>              the type of the property that is the basis for the comparison
     * @return a new stream containing all elements of this stream sorted by the given property
     */
    public <T extends Comparable<T>> Stream<E> sortByDescending(final Mapper<E, T> mapper) {
        if (mapper == null)
            throw new IllegalArgumentException("Unable to sort stream because the mapper is null!");
        return sort(new Comparator<E>() {
            @Override
            public int compare(E left, E right) {
                return mapper.map(right).compareTo(mapper.map(left));
            }
        });
    }

    /**
     * Takes a certain number of elements from this stream and drops the remaining elements
     *
     * @param number the number of items to take
     * @return a new stream containing only the first n elements of this stream
     */
    public Stream<E> take(final int number) {
        if (number < 0)
            throw new IllegalArgumentException("Unable to take a number of elements of this stream because the number is negative!");
        return new TakeStream<E>(this, number);
    }

    /**
     * Groups this stream into chunks based on the key per element that is retrieved via the keySelector
     *
     * @param keyMapper a function that returns the grouping key for a given element
     * @param <K>         the type of the key
     * @return a stream containing groups as its elements
     */
    public <K> Stream<Group<K, E>> groupBy(final Mapper<E, K> keyMapper) {
        if (keyMapper == null)
            throw new IllegalArgumentException("Unable to group this stream because the keyMapper is null!");
        return new GroupedStream<K, E>(this, keyMapper);
    }

    /**
     * Turns this stream into a list
     *
     * @return a new list containing all the elements of this stream
     */
    public List<E> toList() {
        return reduce(new Reducer<E, List<E>>() {
            @Override
            public List<E> reduce(List<E> list, E element) {
                list.add(element);
                return list;
            }
        }, new ArrayList<E>());
    }

    /**
     * Turns this stream into a set
     *
     * @return a new set containing the elements of this stream
     */
    public Set<E> toSet() {
        return reduce(new Reducer<E, Set<E>>() {
            @Override
            public Set<E> reduce(Set<E> set, E element) {
                set.add(element);
                return set;
            }
        }, new HashSet<E>());
    }
}

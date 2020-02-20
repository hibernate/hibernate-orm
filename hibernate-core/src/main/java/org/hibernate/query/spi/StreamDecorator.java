/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.spi;

import java.lang.reflect.InvocationTargetException;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Optional;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Collector;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import org.hibernate.HibernateException;
import org.hibernate.Incubating;
import org.hibernate.internal.util.ReflectHelper;

/**
 * The {@link StreamDecorator} wraps a Java {@link Stream} and registers a {@code closeHandler}
 * which is passed further to any resulting {@link Stream}.
 *
 * The goal of the {@link StreamDecorator} is to close the underlying {@link Stream} upon
 * calling a terminal operation.
 *
 * @author Vlad Mihalcea
 * @since 5.4
 */
@Incubating
public class StreamDecorator<R> implements Stream<R> {

	private final Stream<R> delegate;

	private Runnable closeHandler;

	public StreamDecorator(
			Stream<R> delegate,
			Runnable closeHandler) {
		this.delegate = delegate;
		this.closeHandler = closeHandler;
		this.delegate.onClose( closeHandler );
	}

	@Override
	public Stream<R> filter(Predicate<? super R> predicate) {
		return new StreamDecorator<R>( delegate.filter( predicate ), closeHandler );
	}

	@Override
	public <R1> Stream<R1> map(Function<? super R, ? extends R1> mapper) {
		return new StreamDecorator<>( delegate.map( mapper ), closeHandler );
	}

	@Override
	public IntStream mapToInt(ToIntFunction<? super R> mapper) {
		return new IntStreamDecorator(
				delegate.mapToInt( mapper ),
				closeHandler
		);
	}

	@Override
	public LongStream mapToLong(ToLongFunction<? super R> mapper) {
		return new LongStreamDecorator(
				delegate.mapToLong( mapper ),
				closeHandler
		);
	}

	@Override
	public DoubleStream mapToDouble(ToDoubleFunction<? super R> mapper) {
		return new DoubleStreamDecorator(
				delegate.mapToDouble( mapper ),
				closeHandler
		);
	}

	@Override
	public <R1> Stream<R1> flatMap(Function<? super R, ? extends Stream<? extends R1>> mapper) {
		return new StreamDecorator<>( delegate.flatMap( mapper ), closeHandler );
	}

	@Override
	public IntStream flatMapToInt(Function<? super R, ? extends IntStream> mapper) {
		return new IntStreamDecorator(
				delegate.flatMapToInt( mapper ),
				closeHandler
		);
	}

	@Override
	public LongStream flatMapToLong(Function<? super R, ? extends LongStream> mapper) {
		return new LongStreamDecorator(
				delegate.flatMapToLong( mapper ),
				closeHandler
		);
	}

	@Override
	public DoubleStream flatMapToDouble(Function<? super R, ? extends DoubleStream> mapper) {
		return new DoubleStreamDecorator(
				delegate.flatMapToDouble( mapper ),
				closeHandler
		);
	}

	@Override
	public Stream<R> distinct() {
		return new StreamDecorator<>( delegate.distinct(), closeHandler );
	}

	@Override
	public Stream<R> sorted() {
		return new StreamDecorator<>( delegate.sorted(), closeHandler );
	}

	@Override
	public Stream<R> sorted(Comparator<? super R> comparator) {
		return new StreamDecorator<>( delegate.sorted( comparator ), closeHandler );
	}

	@Override
	public Stream<R> peek(Consumer<? super R> action) {
		return new StreamDecorator<>( delegate.peek( action ), closeHandler );
	}

	@Override
	public Stream<R> limit(long maxSize) {
		return new StreamDecorator<>( delegate.limit( maxSize ), closeHandler );
	}

	@Override
	public Stream<R> skip(long n) {
		return new StreamDecorator<>( delegate.skip( n ), closeHandler );
	}

	@Override
	public void forEach(Consumer<? super R> action) {
		delegate.forEach( action );
		close();
	}

	@Override
	public void forEachOrdered(Consumer<? super R> action) {
		delegate.forEachOrdered( action );
		close();
	}

	@Override
	public Object[] toArray() {
		Object[] result = delegate.toArray();
		close();
		return result;
	}

	@Override
	public <A> A[] toArray(IntFunction<A[]> generator) {
		A[] result = delegate.toArray( generator );
		close();
		return result;
	}

	@Override
	public R reduce(R identity, BinaryOperator<R> accumulator) {
		R result = delegate.reduce( identity, accumulator );
		close();
		return result;
	}

	@Override
	public Optional<R> reduce(BinaryOperator<R> accumulator) {
		Optional<R> result = delegate.reduce( accumulator );
		close();
		return result;
	}

	@Override
	public <U> U reduce(
			U identity, BiFunction<U, ? super R, U> accumulator, BinaryOperator<U> combiner) {
		U result = delegate.reduce( identity, accumulator, combiner );
		close();
		return result;
	}

	@Override
	public <R1> R1 collect(
			Supplier<R1> supplier, BiConsumer<R1, ? super R> accumulator, BiConsumer<R1, R1> combiner) {
		R1 result = delegate.collect( supplier, accumulator, combiner );
		close();
		return result;
	}

	@Override
	public <R1, A> R1 collect(Collector<? super R, A, R1> collector) {
		R1 result = delegate.collect( collector );
		close();
		return result;
	}

	@Override
	public Optional<R> min(Comparator<? super R> comparator) {
		Optional<R> result = delegate.min( comparator );
		close();
		return result;
	}

	@Override
	public Optional<R> max(Comparator<? super R> comparator) {
		Optional<R> result = delegate.max( comparator );
		close();
		return result;
	}

	@Override
	public long count() {
		long result = delegate.count();
		close();
		return result;
	}

	@Override
	public boolean anyMatch(Predicate<? super R> predicate) {
		boolean result = delegate.anyMatch( predicate );
		close();
		return result;
	}

	@Override
	public boolean allMatch(Predicate<? super R> predicate) {
		boolean result = delegate.allMatch( predicate );
		close();
		return result;
	}

	@Override
	public boolean noneMatch(Predicate<? super R> predicate) {
		boolean result = delegate.noneMatch( predicate );
		close();
		return result;
	}

	@Override
	public Optional<R> findFirst() {
		Optional<R> result = delegate.findFirst();
		close();
		return result;
	}

	@Override
	public Optional<R> findAny() {
		Optional<R> result = delegate.findAny();
		close();
		return result;
	}

	@Override
	public Iterator<R> iterator() {
		return delegate.iterator();
	}

	@Override
	public Spliterator<R> spliterator() {
		return delegate.spliterator();
	}

	@Override
	public boolean isParallel() {
		return delegate.isParallel();
	}

	@Override
	public Stream<R> sequential() {
		return new StreamDecorator<>( delegate.sequential(), closeHandler );
	}

	@Override
	public Stream<R> parallel() {
		return new StreamDecorator<>( delegate.parallel(), closeHandler );
	}

	@Override
	public Stream<R> unordered() {
		return new StreamDecorator<>( delegate.unordered(), closeHandler );
	}

	@Override
	public Stream<R> onClose(Runnable closeHandler) {
		this.closeHandler = closeHandler;
		return this;
	}

	@Override
	public void close() {
		delegate.close();
	}

	//Methods added to JDK 9

	public Stream<R> takeWhile(Predicate<? super R> predicate) {
		try {
			@SuppressWarnings("unchecked")
			Stream<R> result = (Stream<R>)
					ReflectHelper.getMethod( Stream.class, "takeWhile", Predicate.class )
							.invoke( delegate, predicate );
			return new StreamDecorator<>( result, closeHandler );
		}
		catch (IllegalAccessException | InvocationTargetException e) {
			throw new HibernateException( e );
		}
	}

	public Stream<R> dropWhile(Predicate<? super R> predicate) {
		try {
			@SuppressWarnings("unchecked")
			Stream<R> result = (Stream<R>)
					ReflectHelper.getMethod( Stream.class, "dropWhile", Predicate.class )
							.invoke( delegate, predicate );
			return new StreamDecorator<>( result, closeHandler );
		}
		catch (IllegalAccessException | InvocationTargetException e) {
			throw new HibernateException( e );
		}
	}
}

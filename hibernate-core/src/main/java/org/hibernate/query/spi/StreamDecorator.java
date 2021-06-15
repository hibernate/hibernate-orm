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
 * The {@link StreamDecorator} wraps a Java {@link Stream} to close the underlying
 * {@link Stream} upon calling a terminal operation.
 *
 * @author Vlad Mihalcea
 * @since 5.4
 */
@Incubating
public class StreamDecorator<R> implements Stream<R> {

	private final Stream<R> delegate;

	public StreamDecorator(
			Stream<R> delegate) {
		this.delegate = delegate;
	}

	@SuppressWarnings("unchecked")
	private <T> Stream<T> newDecorator(Stream<T> stream) {
		return delegate == stream ? (Stream<T>) this : new StreamDecorator<>( stream );
	}

	@Override
	public Stream<R> filter(Predicate<? super R> predicate) {
		return newDecorator( delegate.filter( predicate ) );
	}

	@Override
	public <R1> Stream<R1> map(Function<? super R, ? extends R1> mapper) {
		return newDecorator( delegate.map( mapper ) );
	}

	@Override
	public IntStream mapToInt(ToIntFunction<? super R> mapper) {
		return new IntStreamDecorator( delegate.mapToInt( mapper ) );
	}

	@Override
	public LongStream mapToLong(ToLongFunction<? super R> mapper) {
		return new LongStreamDecorator( delegate.mapToLong( mapper ) );
	}

	@Override
	public DoubleStream mapToDouble(ToDoubleFunction<? super R> mapper) {
		return new DoubleStreamDecorator( delegate.mapToDouble( mapper ) );
	}

	@Override
	public <R1> Stream<R1> flatMap(Function<? super R, ? extends Stream<? extends R1>> mapper) {
		return newDecorator( delegate.flatMap( mapper ) );
	}

	@Override
	public IntStream flatMapToInt(Function<? super R, ? extends IntStream> mapper) {
		return new IntStreamDecorator( delegate.flatMapToInt( mapper ) );
	}

	@Override
	public LongStream flatMapToLong(Function<? super R, ? extends LongStream> mapper) {
		return new LongStreamDecorator( delegate.flatMapToLong( mapper ) );
	}

	@Override
	public DoubleStream flatMapToDouble(Function<? super R, ? extends DoubleStream> mapper) {
		return new DoubleStreamDecorator( delegate.flatMapToDouble( mapper ) );
	}

	@Override
	public Stream<R> distinct() {
		return newDecorator( delegate.distinct() );
	}

	@Override
	public Stream<R> sorted() {
		return newDecorator( delegate.sorted() );
	}

	@Override
	public Stream<R> sorted(Comparator<? super R> comparator) {
		return newDecorator( delegate.sorted( comparator ) );
	}

	@Override
	public Stream<R> peek(Consumer<? super R> action) {
		return newDecorator( delegate.peek( action ) );
	}

	@Override
	public Stream<R> limit(long maxSize) {
		return newDecorator( delegate.limit( maxSize ) );
	}

	@Override
	public Stream<R> skip(long n) {
		return newDecorator( delegate.skip( n ) );
	}

	@Override
	public void forEach(Consumer<? super R> action) {
		try {
			delegate.forEach(action);
		}
		finally {
			close();
		}
	}

	@Override
	public void forEachOrdered(Consumer<? super R> action) {
		try {
			delegate.forEachOrdered( action );
		}
		finally {
			close();
		}
	}

	@Override
	public Object[] toArray() {
		try {
			return delegate.toArray();
		}
		finally {
			close();
		}
	}

	@Override
	public <A> A[] toArray(IntFunction<A[]> generator) {
		try {
			return delegate.toArray( generator );
		}
		finally {
			close();
		}
	}

	@Override
	public R reduce(R identity, BinaryOperator<R> accumulator) {
		try {
			return delegate.reduce( identity, accumulator );
		}
		finally {
			close();
		}
	}

	@Override
	public Optional<R> reduce(BinaryOperator<R> accumulator) {
		try {
			return delegate.reduce( accumulator );
		}
		finally {
			close();
		}
	}

	@Override
	public <U> U reduce(
			U identity, BiFunction<U, ? super R, U> accumulator, BinaryOperator<U> combiner) {
		try {
			return delegate.reduce( identity, accumulator, combiner );
		}
		finally {
			close();
		}
	}

	@Override
	public <R1> R1 collect(
			Supplier<R1> supplier, BiConsumer<R1, ? super R> accumulator, BiConsumer<R1, R1> combiner) {
		try {
			return delegate.collect( supplier, accumulator, combiner );
		}
		finally {
			close();
		}
	}

	@Override
	public <R1, A> R1 collect(Collector<? super R, A, R1> collector) {
		try {
			return delegate.collect( collector );
		}
		finally {
			close();
		}
	}

	@Override
	public Optional<R> min(Comparator<? super R> comparator) {
		try {
			return delegate.min( comparator );
		}
		finally {
			close();
		}
	}

	@Override
	public Optional<R> max(Comparator<? super R> comparator) {
		try {
			return delegate.max( comparator );
		}
		finally {
			close();
		}
	}

	@Override
	public long count() {
		try {
			return delegate.count();
		}
		finally {
			close();
		}
	}

	@Override
	public boolean anyMatch(Predicate<? super R> predicate) {
		try {
			return delegate.anyMatch( predicate );
		}
		finally {
			close();
		}
	}

	@Override
	public boolean allMatch(Predicate<? super R> predicate) {
		try {
			return delegate.allMatch( predicate );
		}
		finally {
			close();
		}
	}

	@Override
	public boolean noneMatch(Predicate<? super R> predicate) {
		try {
			return delegate.noneMatch( predicate );
		}
		finally {
			close();
		}
	}

	@Override
	public Optional<R> findFirst() {
		try {
			return delegate.findFirst();
		}
		finally {
			close();
		}
	}

	@Override
	public Optional<R> findAny() {
		try {
			return delegate.findAny();
		}
		finally {
			close();
		}
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
		return newDecorator( delegate.sequential() );
	}

	@Override
	public Stream<R> parallel() {
		return newDecorator( delegate.parallel() );
	}

	@Override
	public Stream<R> unordered() {
		return newDecorator( delegate.unordered() );
	}

	@Override
	public Stream<R> onClose(Runnable closeHandler) {
		return newDecorator( delegate.onClose( closeHandler ) );
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
			return newDecorator( result );
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
			return newDecorator( result );
		}
		catch (IllegalAccessException | InvocationTargetException e) {
			throw new HibernateException( e );
		}
	}
}

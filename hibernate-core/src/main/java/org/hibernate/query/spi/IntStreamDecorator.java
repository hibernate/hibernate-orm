/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.spi;

import java.lang.reflect.InvocationTargetException;
import java.util.IntSummaryStatistics;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;
import java.util.function.IntPredicate;
import java.util.function.IntToDoubleFunction;
import java.util.function.IntToLongFunction;
import java.util.function.IntUnaryOperator;
import java.util.function.ObjIntConsumer;
import java.util.function.Supplier;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import org.hibernate.HibernateException;
import org.hibernate.Incubating;
import org.hibernate.internal.util.ReflectHelper;

/**
 * The {@link IntStreamDecorator} wraps a Java {@link IntStream} to close the underlying
 * {@link IntStream} upon calling a terminal operation.
 *
 * @author Vlad Mihalcea
 * @since 5.4
 */
@Incubating
public class IntStreamDecorator implements IntStream {

	private final IntStream delegate;

	public IntStreamDecorator(
			IntStream delegate) {
		this.delegate = delegate;
	}

	private IntStream newDecorator(IntStream stream) {
		return delegate == stream ? this : new IntStreamDecorator( stream );
	}

	@Override
	public IntStream filter(IntPredicate predicate) {
		return newDecorator( delegate.filter( predicate ) );
	}

	@Override
	public IntStream map(IntUnaryOperator mapper) {
		return newDecorator( delegate.map( mapper ) );
	}

	@Override
	public <U> Stream<U> mapToObj(IntFunction<? extends U> mapper) {
		return new StreamDecorator<>( delegate.mapToObj( mapper ) );
	}

	@Override
	public LongStream mapToLong(IntToLongFunction mapper) {
		return new LongStreamDecorator( delegate.mapToLong( mapper ) );
	}

	@Override
	public DoubleStream mapToDouble(IntToDoubleFunction mapper) {
		return new DoubleStreamDecorator( delegate.mapToDouble( mapper ) );
	}

	@Override
	public IntStream flatMap(IntFunction<? extends IntStream> mapper) {
		return newDecorator( delegate.flatMap( mapper ) );
	}

	@Override
	public IntStream distinct() {
		return newDecorator( delegate.distinct() );
	}

	@Override
	public IntStream sorted() {
		return newDecorator( delegate.sorted() );
	}

	@Override
	public IntStream peek(IntConsumer action) {
		return newDecorator( delegate.peek( action ) );
	}

	@Override
	public IntStream limit(long maxSize) {
		return newDecorator( delegate.limit( maxSize ) );
	}

	@Override
	public IntStream skip(long n) {
		return newDecorator( delegate.skip( n ) );
	}

	@Override
	public void forEach(IntConsumer action) {
		try {
			delegate.forEach( action );
		}
		finally {
			close();
		}
	}

	@Override
	public void forEachOrdered(IntConsumer action) {
		try {
			delegate.forEachOrdered( action );
		}
		finally {
			close();
		}
	}

	@Override
	public int[] toArray() {
		try {
			return delegate.toArray();
		}
		finally {
			close();
		}
	}

	@Override
	public int reduce(int identity, IntBinaryOperator op) {
		try {
			return delegate.reduce( identity, op );
		}
		finally {
			close();
		}
	}

	@Override
	public OptionalInt reduce(IntBinaryOperator op) {
		try {
			return delegate.reduce( op );
		}
		finally {
			close();
		}
	}

	@Override
	public <R> R collect(
			Supplier<R> supplier, ObjIntConsumer<R> accumulator, BiConsumer<R, R> combiner) {
		try {
			return delegate.collect( supplier, accumulator, combiner );
		}
		finally {
			close();
		}
	}

	@Override
	public int sum() {
		try {
			return delegate.sum();
		}
		finally {
			close();
		}
	}

	@Override
	public OptionalInt min() {
		try {
			return delegate.min();
		}
		finally {
			close();
		}
	}

	@Override
	public OptionalInt max() {
		try {
			return delegate.max();
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
	public OptionalDouble average() {
		try {
			return delegate.average();
		}
		finally {
			close();
		}
	}

	@Override
	public IntSummaryStatistics summaryStatistics() {
		try {
			return delegate.summaryStatistics();
		}
		finally {
			close();
		}
	}

	@Override
	public boolean anyMatch(IntPredicate predicate) {
		try {
			return delegate.anyMatch( predicate );
		}
		finally {
			close();
		}
	}

	@Override
	public boolean allMatch(IntPredicate predicate) {
		try {
			return delegate.allMatch( predicate );
		}
		finally {
			close();
		}
	}

	@Override
	public boolean noneMatch(IntPredicate predicate) {
		try {
			return delegate.noneMatch( predicate );
		}
		finally {
			close();
		}
	}

	@Override
	public OptionalInt findFirst() {
		try {
			return delegate.findFirst();
		}
		finally {
			close();
		}
	}

	@Override
	public OptionalInt findAny() {
		try {
			return delegate.findAny();
		}
		finally {
			close();
		}
	}

	@Override
	public LongStream asLongStream() {
		return new LongStreamDecorator( delegate.asLongStream() );
	}

	@Override
	public DoubleStream asDoubleStream() {
		return new DoubleStreamDecorator( delegate.asDoubleStream() );
	}

	@Override
	public Stream<Integer> boxed() {
		return new StreamDecorator<>( delegate.boxed() );
	}

	@Override
	public IntStream sequential() {
		return newDecorator( delegate.sequential() );
	}

	@Override
	public IntStream parallel() {
		return newDecorator( delegate.parallel() );
	}

	@Override
	public IntStream unordered() {
		return newDecorator( delegate.unordered() );
	}

	@Override
	public IntStream onClose(Runnable closeHandler) {
		return newDecorator( delegate.onClose( closeHandler ) );
	}

	@Override
	public void close() {
		delegate.close();
	}

	@Override
	public PrimitiveIterator.OfInt iterator() {
		return delegate.iterator();
	}

	@Override
	public Spliterator.OfInt spliterator() {
		return delegate.spliterator();
	}

	@Override
	public boolean isParallel() {
		return delegate.isParallel();
	}

	//Methods added to JDK 9

	public IntStream takeWhile(IntPredicate predicate) {
		try {
			IntStream result = (IntStream)
					ReflectHelper.getMethod( IntStream.class, "takeWhile", IntPredicate.class )
							.invoke( delegate, predicate );
			return newDecorator( result );
		}
		catch (IllegalAccessException | InvocationTargetException e) {
			throw new HibernateException( e );
		}
	}

	public IntStream dropWhile(IntPredicate predicate) {
		try {
			IntStream result = (IntStream)
					ReflectHelper.getMethod( Stream.class, "dropWhile", IntPredicate.class )
							.invoke( delegate, predicate );
			return newDecorator( result );
		}
		catch (IllegalAccessException | InvocationTargetException e) {
			throw new HibernateException( e );
		}
	}
}

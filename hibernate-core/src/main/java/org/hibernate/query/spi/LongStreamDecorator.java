/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.spi;

import java.lang.reflect.InvocationTargetException;
import java.util.LongSummaryStatistics;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.LongBinaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongFunction;
import java.util.function.LongPredicate;
import java.util.function.LongToDoubleFunction;
import java.util.function.LongToIntFunction;
import java.util.function.LongUnaryOperator;
import java.util.function.ObjLongConsumer;
import java.util.function.Supplier;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import org.hibernate.HibernateException;
import org.hibernate.Incubating;
import org.hibernate.internal.util.ReflectHelper;

/**
 * The {@link LongStreamDecorator} wraps a Java {@link LongStream} to close the underlying
 * {@link LongStream} upon calling a terminal operation.
 *
 * @author Vlad Mihalcea
 * @since 5.4
 */
@Incubating
public class LongStreamDecorator implements LongStream {

	private final LongStream delegate;

	public LongStreamDecorator(
			LongStream delegate) {
		this.delegate = delegate;
	}

	private LongStream newDecorator(LongStream stream) {
		return delegate == stream ? this : new LongStreamDecorator( stream );
	}

	@Override
	public LongStream filter(LongPredicate predicate) {
		return newDecorator( delegate.filter( predicate ) );
	}

	@Override
	public LongStream map(LongUnaryOperator mapper) {
		return newDecorator( delegate.map( mapper ) );
	}

	@Override
	public <U> Stream<U> mapToObj(LongFunction<? extends U> mapper) {
		return new StreamDecorator<>( delegate.mapToObj( mapper ) );
	}

	@Override
	public IntStream mapToInt(LongToIntFunction mapper) {
		return new IntStreamDecorator( delegate.mapToInt( mapper ) );
	}

	@Override
	public DoubleStream mapToDouble(LongToDoubleFunction mapper) {
		return new DoubleStreamDecorator( delegate.mapToDouble( mapper ) );
	}

	@Override
	public LongStream flatMap(LongFunction<? extends LongStream> mapper) {
		return newDecorator( delegate.flatMap( mapper ) );
	}

	@Override
	public LongStream distinct() {
		return newDecorator( delegate.distinct() );
	}

	@Override
	public LongStream sorted() {
		return newDecorator( delegate.sorted() );
	}

	@Override
	public LongStream peek(LongConsumer action) {
		return newDecorator( delegate.peek( action ) );
	}

	@Override
	public LongStream limit(long maxSize) {
		return newDecorator( delegate.limit( maxSize ) );
	}

	@Override
	public LongStream skip(long n) {
		return newDecorator( delegate.skip( n ) );
	}

	@Override
	public void forEach(LongConsumer action) {
		delegate.forEach( action );
		close();
	}

	@Override
	public void forEachOrdered(LongConsumer action) {
		delegate.forEachOrdered( action );
		close();
	}

	@Override
	public long[] toArray() {
		long[] result = delegate.toArray();
		close();
		return result;
	}

	@Override
	public long reduce(long identity, LongBinaryOperator op) {
		long result = delegate.reduce( identity, op );
		close();
		return result;
	}

	@Override
	public OptionalLong reduce(LongBinaryOperator op) {
		OptionalLong result = delegate.reduce( op );
		close();
		return result;
	}

	@Override
	public <R> R collect(
			Supplier<R> supplier, ObjLongConsumer<R> accumulator, BiConsumer<R, R> combiner) {
		R result = delegate.collect( supplier, accumulator, combiner );
		close();
		return result;
	}

	@Override
	public long sum() {
		long result = delegate.sum();
		close();
		return result;
	}

	@Override
	public OptionalLong min() {
		OptionalLong result = delegate.min();
		close();
		return result;
	}

	@Override
	public OptionalLong max() {
		OptionalLong result = delegate.max();
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
	public OptionalDouble average() {
		OptionalDouble result = delegate.average();
		close();
		return result;
	}

	@Override
	public LongSummaryStatistics summaryStatistics() {
		LongSummaryStatistics result = delegate.summaryStatistics();
		close();
		return result;
	}

	@Override
	public boolean anyMatch(LongPredicate predicate) {
		boolean result = delegate.anyMatch( predicate );
		close();
		return result;
	}

	@Override
	public boolean allMatch(LongPredicate predicate) {
		boolean result = delegate.allMatch( predicate );
		close();
		return result;
	}

	@Override
	public boolean noneMatch(LongPredicate predicate) {
		boolean result = delegate.noneMatch( predicate );
		close();
		return result;
	}

	@Override
	public OptionalLong findFirst() {
		OptionalLong result = delegate.findFirst();
		close();
		return result;
	}

	@Override
	public OptionalLong findAny() {
		OptionalLong result = delegate.findAny();
		close();
		return result;
	}

	@Override
	public DoubleStream asDoubleStream() {
		DoubleStream result = delegate.asDoubleStream();
		close();
		return result;
	}

	@Override
	public Stream<Long> boxed() {
		return new StreamDecorator<>( delegate.boxed() );
	}

	@Override
	public LongStream sequential() {
		return newDecorator( delegate.sequential() );
	}

	@Override
	public LongStream parallel() {
		return newDecorator( delegate.parallel() );
	}

	@Override
	public LongStream unordered() {
		return newDecorator( delegate.unordered() );
	}

	@Override
	public LongStream onClose(Runnable closeHandler) {
		return newDecorator( delegate.onClose( closeHandler ) );
	}

	@Override
	public void close() {
		delegate.close();
	}

	@Override
	public PrimitiveIterator.OfLong iterator() {
		return delegate.iterator();
	}

	@Override
	public Spliterator.OfLong spliterator() {
		return delegate.spliterator();
	}

	@Override
	public boolean isParallel() {
		return delegate.isParallel();
	}

	//Methods added to JDK 9

	public LongStream takeWhile(LongPredicate predicate) {
		try {
			LongStream result = (LongStream)
					ReflectHelper.getMethod( LongStream.class, "takeWhile", LongPredicate.class )
							.invoke( delegate, predicate );
			return newDecorator( result );
		}
		catch (IllegalAccessException | InvocationTargetException e) {
			throw new HibernateException( e );
		}
	}

	public LongStream dropWhile(LongPredicate predicate) {
		try {
			LongStream result = (LongStream)
					ReflectHelper.getMethod( Stream.class, "dropWhile", LongPredicate.class )
							.invoke( delegate, predicate );
			return newDecorator( result );
		}
		catch (IllegalAccessException | InvocationTargetException e) {
			throw new HibernateException( e );
		}
	}
}

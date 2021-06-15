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
		try {
			delegate.forEach( action );
		}
		finally {
			close();
		}
	}

	@Override
	public void forEachOrdered(LongConsumer action) {
		try {
			delegate.forEachOrdered( action );
		}
		finally {
			close();
		}
	}

	@Override
	public long[] toArray() {
		try {
			return delegate.toArray();
		}
		finally {
			close();
		}
	}

	@Override
	public long reduce(long identity, LongBinaryOperator op) {
		try {
			return delegate.reduce( identity, op );
		}
		finally {
			close();
		}
	}

	@Override
	public OptionalLong reduce(LongBinaryOperator op) {
		try {
			return delegate.reduce( op );
		}
		finally {
			close();
		}
	}

	@Override
	public <R> R collect(
			Supplier<R> supplier, ObjLongConsumer<R> accumulator, BiConsumer<R, R> combiner) {
		try {
			return delegate.collect( supplier, accumulator, combiner );
		}
		finally {
			close();
		}
	}

	@Override
	public long sum() {
		try {
			return delegate.sum();
		}
		finally {
			close();
		}
	}

	@Override
	public OptionalLong min() {
		try {
			return delegate.min();
		}
		finally {
			close();
		}
	}

	@Override
	public OptionalLong max() {
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
	public LongSummaryStatistics summaryStatistics() {
		try {
			return delegate.summaryStatistics();
		}
		finally {
			close();
		}
	}

	@Override
	public boolean anyMatch(LongPredicate predicate) {
		try {
			return delegate.anyMatch( predicate );
		}
		finally {
			close();
		}
	}

	@Override
	public boolean allMatch(LongPredicate predicate) {
		try {
			return delegate.allMatch( predicate );
		}
		finally {
			close();
		}
	}

	@Override
	public boolean noneMatch(LongPredicate predicate) {
		try {
			return delegate.noneMatch( predicate );
		}
		finally {
			close();
		}
	}

	@Override
	public OptionalLong findFirst() {
		try {
			return delegate.findFirst();
		}
		finally {
			close();
		}
	}

	@Override
	public OptionalLong findAny() {
		try {
			return delegate.findAny();
		}
		finally {
			close();
		}
	}

	@Override
	public DoubleStream asDoubleStream() {
		return new DoubleStreamDecorator( delegate.asDoubleStream() );
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

	//Methods added to JDK 16
	//TODO: Find a way to support mapMulti(LongMapMultiConsumer)
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.spi;

import java.lang.reflect.InvocationTargetException;
import java.util.DoubleSummaryStatistics;
import java.util.OptionalDouble;
import java.util.PrimitiveIterator;
import java.util.Spliterator;
import java.util.function.BiConsumer;
import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoublePredicate;
import java.util.function.DoubleToIntFunction;
import java.util.function.DoubleToLongFunction;
import java.util.function.DoubleUnaryOperator;
import java.util.function.ObjDoubleConsumer;
import java.util.function.Supplier;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import org.hibernate.HibernateException;
import org.hibernate.Incubating;
import org.hibernate.internal.util.ReflectHelper;

/**
 * The {@link DoubleStreamDecorator} wraps a Java {@link DoubleStream} to close the underlying
 * {@link DoubleStream} upon calling a terminal operation.
 *
 * @author Vlad Mihalcea
 * @since 5.4
 */
@Incubating
public class DoubleStreamDecorator implements DoubleStream {

	private final DoubleStream delegate;

	public DoubleStreamDecorator(
			DoubleStream delegate) {
		this.delegate = delegate;
	}

	private DoubleStream newDecorator(DoubleStream stream) {
		return delegate == stream ? this : new DoubleStreamDecorator( stream );
	}

	@Override
	public DoubleStream filter(DoublePredicate predicate) {
		return newDecorator( delegate.filter( predicate ) );
	}

	@Override
	public DoubleStream map(DoubleUnaryOperator mapper) {
		return newDecorator( delegate.map( mapper ) );
	}

	@Override
	public <U> Stream<U> mapToObj(DoubleFunction<? extends U> mapper) {
		return new StreamDecorator<>( delegate.mapToObj( mapper ) );
	}

	@Override
	public IntStream mapToInt(DoubleToIntFunction mapper) {
		return new IntStreamDecorator( delegate.mapToInt( mapper ) );
	}

	@Override
	public LongStream mapToLong(DoubleToLongFunction mapper) {
		return new LongStreamDecorator( delegate.mapToLong( mapper ) );
	}

	@Override
	public DoubleStream flatMap(DoubleFunction<? extends DoubleStream> mapper) {
		return newDecorator( delegate.flatMap( mapper ) );
	}

	@Override
	public DoubleStream distinct() {
		return newDecorator( delegate.distinct() );
	}

	@Override
	public DoubleStream sorted() {
		return newDecorator( delegate.sorted() );
	}

	@Override
	public DoubleStream peek(DoubleConsumer action) {
		return newDecorator( delegate.peek( action ) );
	}

	@Override
	public DoubleStream limit(long maxSize) {
		return newDecorator( delegate.limit( maxSize ) );
	}

	@Override
	public DoubleStream skip(long n) {
		return newDecorator( delegate.skip( n ) );
	}

	@Override
	public void forEach(DoubleConsumer action) {
		try {
			delegate.forEach( action );
		}
		finally {
			close();
		}
	}

	@Override
	public void forEachOrdered(DoubleConsumer action) {
		try {
			delegate.forEachOrdered( action );
		}
		finally {
			close();
		}
	}

	@Override
	public double[] toArray() {
		try {
			return delegate.toArray();
		}
		finally {
			close();
		}
	}

	@Override
	public double reduce(double identity, DoubleBinaryOperator op) {
		try {
			return delegate.reduce( identity, op );
		}
		finally {
			close();
		}
	}

	@Override
	public OptionalDouble reduce(DoubleBinaryOperator op) {
		try {
			return delegate.reduce( op );
		}
		finally {
			close();
		}
	}

	@Override
	public <R> R collect(
			Supplier<R> supplier, ObjDoubleConsumer<R> accumulator, BiConsumer<R, R> combiner) {
		try {
			return delegate.collect( supplier, accumulator, combiner );
		}
		finally {
			close();
		}
	}

	@Override
	public double sum() {
		try {
			return delegate.sum();
		}
		finally {
			close();
		}
	}

	@Override
	public OptionalDouble min() {
		try {
			return delegate.min();
		}
		finally {
			close();
		}
	}

	@Override
	public OptionalDouble max() {
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
	public DoubleSummaryStatistics summaryStatistics() {
		try {
			return delegate.summaryStatistics();
		}
		finally {
			close();
		}
	}

	@Override
	public boolean anyMatch(DoublePredicate predicate) {
		try {
			return delegate.anyMatch( predicate );
		}
		finally {
			close();
		}
	}

	@Override
	public boolean allMatch(DoublePredicate predicate) {
		try {
			return delegate.allMatch( predicate );
		}
		finally {
			close();
		}
	}

	@Override
	public boolean noneMatch(DoublePredicate predicate) {
		try {
			return delegate.noneMatch( predicate );
		}
		finally {
			close();
		}
	}

	@Override
	public OptionalDouble findFirst() {
		try {
			return delegate.findFirst();
		}
		finally {
			close();
		}
	}

	@Override
	public OptionalDouble findAny() {
		try {
			return delegate.findAny();
		}
		finally {
			close();
		}
	}

	@Override
	public Stream<Double> boxed() {
		return new StreamDecorator<>( delegate.boxed() );
	}

	@Override
	public DoubleStream sequential() {
		return newDecorator( delegate.sequential() );
	}

	@Override
	public DoubleStream parallel() {
		return newDecorator( delegate.parallel() );
	}

	@Override
	public DoubleStream unordered() {
		return newDecorator( delegate.unordered() );
	}

	@Override
	public DoubleStream onClose(Runnable closeHandler) {
		return newDecorator( delegate.onClose( closeHandler ) );
	}

	@Override
	public void close() {
		delegate.close();
	}

	@Override
	public PrimitiveIterator.OfDouble iterator() {
		return delegate.iterator();
	}

	@Override
	public Spliterator.OfDouble spliterator() {
		return delegate.spliterator();
	}

	@Override
	public boolean isParallel() {
		return delegate.isParallel();
	}

	//Methods added to JDK 9

	public DoubleStream takeWhile(DoublePredicate predicate) {
		try {
			DoubleStream result = (DoubleStream)
					ReflectHelper.getMethod( DoubleStream.class, "takeWhile", DoublePredicate.class )
							.invoke( delegate, predicate );
			return newDecorator( result );
		}
		catch (IllegalAccessException | InvocationTargetException e) {
			throw new HibernateException( e );
		}
	}

	public DoubleStream dropWhile(DoublePredicate predicate) {
		try {
			DoubleStream result = (DoubleStream)
					ReflectHelper.getMethod( Stream.class, "dropWhile", DoublePredicate.class )
							.invoke( delegate, predicate );
			return newDecorator( result );
		}
		catch (IllegalAccessException | InvocationTargetException e) {
			throw new HibernateException( e );
		}
	}
}

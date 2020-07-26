/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.spi;

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

import org.hibernate.Incubating;

/**
 * The {@link DoubleStreamDecorator} wraps a Java {@link DoubleStream} and registers a {@code closeHandler}
 * which is passed further to any resulting {@link Stream}.
 * <p>
 * The goal of the {@link DoubleStreamDecorator} is to close the underlying {@link DoubleStream} upon
 * calling a terminal operation.
 *
 * @author Vlad Mihalcea
 * @since 5.4
 */
@Incubating
public class DoubleStreamDecorator implements DoubleStream {

	private final DoubleStream delegate;

	private Runnable closeHandler;

	public DoubleStreamDecorator(
			DoubleStream delegate,
			Runnable closeHandler) {
		this.delegate = delegate;
		this.closeHandler = closeHandler;
		this.delegate.onClose( closeHandler );
	}

	@Override
	public DoubleStream filter(DoublePredicate predicate) {
		return new DoubleStreamDecorator(
				delegate.filter( predicate ),
				closeHandler
		);
	}

	@Override
	public DoubleStream map(DoubleUnaryOperator mapper) {
		return new DoubleStreamDecorator(
				delegate.map( mapper ),
				closeHandler
		);
	}

	@Override
	public <U> Stream<U> mapToObj(DoubleFunction<? extends U> mapper) {
		return new StreamDecorator<>(
				delegate.mapToObj( mapper ),
				closeHandler
		);
	}

	@Override
	public IntStream mapToInt(DoubleToIntFunction mapper) {
		return new IntStreamDecorator(
				delegate.mapToInt( mapper ),
				closeHandler
		);
	}

	@Override
	public LongStream mapToLong(DoubleToLongFunction mapper) {
		return new LongStreamDecorator(
				delegate.mapToLong( mapper ),
				closeHandler
		);
	}

	@Override
	public DoubleStream flatMap(DoubleFunction<? extends DoubleStream> mapper) {
		return new DoubleStreamDecorator(
				delegate.flatMap( mapper ),
				closeHandler
		);
	}

	@Override
	public DoubleStream distinct() {
		return new DoubleStreamDecorator(
				delegate.distinct(),
				closeHandler
		);
	}

	@Override
	public DoubleStream sorted() {
		return new DoubleStreamDecorator(
				delegate.sorted(),
				closeHandler
		);
	}

	@Override
	public DoubleStream peek(DoubleConsumer action) {
		return new DoubleStreamDecorator(
				delegate.peek( action ),
				closeHandler
		);
	}

	@Override
	public DoubleStream limit(long maxSize) {
		return new DoubleStreamDecorator(
				delegate.limit( maxSize ),
				closeHandler
		);
	}

	@Override
	public DoubleStream skip(long n) {
		return new DoubleStreamDecorator(
				delegate.skip( n ),
				closeHandler
		);
	}

	@Override
	public void forEach(DoubleConsumer action) {
		delegate.forEach( action );
		close();
	}

	@Override
	public void forEachOrdered(DoubleConsumer action) {
		delegate.forEachOrdered( action );
		close();
	}

	@Override
	public double[] toArray() {
		double[] result = delegate.toArray();
		close();
		return result;
	}

	@Override
	public double reduce(double identity, DoubleBinaryOperator op) {
		double result = delegate.reduce( identity, op );
		close();
		return result;
	}

	@Override
	public OptionalDouble reduce(DoubleBinaryOperator op) {
		OptionalDouble result = delegate.reduce( op );
		close();
		return result;
	}

	@Override
	public <R> R collect(
			Supplier<R> supplier, ObjDoubleConsumer<R> accumulator, BiConsumer<R, R> combiner) {
		R result = delegate.collect( supplier, accumulator, combiner );
		close();
		return result;
	}

	@Override
	public double sum() {
		double result = delegate.sum();
		close();
		return result;
	}

	@Override
	public OptionalDouble min() {
		OptionalDouble result = delegate.min();
		close();
		return result;
	}

	@Override
	public OptionalDouble max() {
		OptionalDouble result = delegate.max();
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
	public DoubleSummaryStatistics summaryStatistics() {
		DoubleSummaryStatistics result = delegate.summaryStatistics();
		close();
		return result;
	}

	@Override
	public boolean anyMatch(DoublePredicate predicate) {
		boolean result = delegate.anyMatch( predicate );
		close();
		return result;
	}

	@Override
	public boolean allMatch(DoublePredicate predicate) {
		boolean result = delegate.allMatch( predicate );
		close();
		return result;
	}

	@Override
	public boolean noneMatch(DoublePredicate predicate) {
		boolean result = delegate.noneMatch( predicate );
		close();
		return result;
	}

	@Override
	public OptionalDouble findFirst() {
		OptionalDouble result = delegate.findFirst();
		close();
		return result;
	}

	@Override
	public OptionalDouble findAny() {
		OptionalDouble result = delegate.findAny();
		close();
		return result;
	}

	@Override
	public Stream<Double> boxed() {
		return new StreamDecorator<>(
				delegate.boxed(),
				closeHandler
		);
	}

	@Override
	public DoubleStream sequential() {
		return new DoubleStreamDecorator(
				delegate.sequential(),
				closeHandler
		);
	}

	@Override
	public DoubleStream parallel() {
		return new DoubleStreamDecorator(
				delegate.parallel(),
				closeHandler
		);
	}

	@Override
	public DoubleStream unordered() {
		return new DoubleStreamDecorator(
				delegate.unordered(),
				closeHandler
		);
	}

	@Override
	public DoubleStream onClose(Runnable closeHandler) {
		this.closeHandler = closeHandler;
		return this;
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
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.spi;

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

import org.hibernate.Incubating;

/**
 * The {@link LongStreamDecorator} wraps a Java {@link LongStream} and registers a {@code closeHandler}
 * which is passed further to any resulting {@link Stream}.
 *
 * The goal of the {@link LongStreamDecorator} is to close the underlying {@link LongStream} upon
 * calling a terminal operation.
 *
 * @author Vlad Mihalcea
 * @since 5.4
 */
@Incubating
public class LongStreamDecorator implements LongStream {

	private final LongStream delegate;

	private Runnable closeHandler;

	public LongStreamDecorator(
			LongStream delegate,
			Runnable closeHandler) {
		this.delegate = delegate;
		this.closeHandler = closeHandler;
		this.delegate.onClose( closeHandler );
	}

	@Override
	public LongStream filter(LongPredicate predicate) {
		return new LongStreamDecorator(
				delegate.filter( predicate ),
				closeHandler
		);
	}

	@Override
	public LongStream map(LongUnaryOperator mapper) {
		return new LongStreamDecorator(
				delegate.map( mapper ),
				closeHandler
		);
	}

	@Override
	public <U> Stream<U> mapToObj(LongFunction<? extends U> mapper) {
		return new StreamDecorator<>(
				delegate.mapToObj( mapper ),
				closeHandler
		);
	}

	@Override
	public IntStream mapToInt(LongToIntFunction mapper) {
		return new IntStreamDecorator(
				delegate.mapToInt( mapper ),
				closeHandler
		);
	}

	@Override
	public DoubleStream mapToDouble(LongToDoubleFunction mapper) {
		return new DoubleStreamDecorator(
				delegate.mapToDouble( mapper ),
				closeHandler
		);
	}

	@Override
	public LongStream flatMap(LongFunction<? extends LongStream> mapper) {
		return new LongStreamDecorator(
				delegate.flatMap( mapper ),
				closeHandler
		);
	}

	@Override
	public LongStream distinct() {
		return new LongStreamDecorator(
				delegate.distinct(),
				closeHandler
		);
	}

	@Override
	public LongStream sorted() {
		return new LongStreamDecorator(
				delegate.sorted(),
				closeHandler
		);
	}

	@Override
	public LongStream peek(LongConsumer action) {
		return new LongStreamDecorator(
				delegate.peek( action ),
				closeHandler
		);
	}

	@Override
	public LongStream limit(long maxSize) {
		return new LongStreamDecorator(
				delegate.limit( maxSize ),
				closeHandler
		);
	}

	@Override
	public LongStream skip(long n) {
		return new LongStreamDecorator(
				delegate.skip( n ),
				closeHandler
		);
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
		boolean result = delegate.anyMatch(predicate);
		close();
		return result;
	}

	@Override
	public boolean allMatch(LongPredicate predicate) {
		boolean result = delegate.allMatch(predicate);
		close();
		return result;
	}

	@Override
	public boolean noneMatch(LongPredicate predicate) {
		boolean result = delegate.noneMatch(predicate);
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
		return new StreamDecorator<>(
				delegate.boxed(),
				closeHandler
		);
	}

	@Override
	public LongStream sequential() {
		return new LongStreamDecorator(
				delegate.sequential(),
				closeHandler
		);
	}

	@Override
	public LongStream parallel() {
		return new LongStreamDecorator(
				delegate.parallel(),
				closeHandler
		);
	}

	@Override
	public LongStream unordered() {
		return new LongStreamDecorator(
				delegate.unordered(),
				closeHandler
		);
	}

	@Override
	public LongStream onClose(Runnable closeHandler) {
		this.closeHandler = closeHandler;
		return this;
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
}

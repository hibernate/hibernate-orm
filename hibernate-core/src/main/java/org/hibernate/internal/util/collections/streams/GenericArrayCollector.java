/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal.util.collections.streams;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;

/**
 * @author Steve Ebersole
 */
public class GenericArrayCollector<T> implements Collector<T, List<T>, T[]> {
	public static <T> GenericArrayCollector<T> forType(Class<T> type) {
		return new GenericArrayCollector<T>( type );
	}

	private final Class<T> collectedType;

	public GenericArrayCollector(Class<T> collectedType) {
		this.collectedType = collectedType;
	}

	@Override
	public Supplier<List<T>> supplier() {
		return ArrayList::new;
	}

	@Override
	public BiConsumer<List<T>, T> accumulator() {
		return List::add;
	}

	@Override
	public BinaryOperator<List<T>> combiner() {
		return (ts, ts2) -> {
			ts.addAll( ts2 );
			return ts;
		};
	}

	@Override
	@SuppressWarnings("unchecked")
	public Function<List<T>, T[]> finisher() {
		return ts -> ts.toArray( (T[]) Array.newInstance( collectedType, ts.size() ) );
	}

	@Override
	public Set<Characteristics> characteristics() {
		return EnumSet.of( Characteristics.CONCURRENT );
	}
}

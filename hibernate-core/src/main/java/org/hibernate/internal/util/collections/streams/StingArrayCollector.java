/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.internal.util.collections.streams;

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
 * A Java 8 Stream Collector for collecting Strings into a String[].
 *
 * @author Steve Ebersole
 */
public class StingArrayCollector implements Collector<String, List<String>, String[]> {
	/**
	 * Singleton access
	 */
	public static final StingArrayCollector INSTANCE = new StingArrayCollector();

	@Override
	public Supplier<List<String>> supplier() {
		return ArrayList::new;
	}

	@Override
	public BiConsumer<List<String>, String> accumulator() {
		return List::add;
	}

	@Override
	public BinaryOperator<List<String>> combiner() {
		return (strings, strings2) -> {
			strings.addAll( strings2 );
			return strings;
		};
	}

	@Override
	public Function<List<String>, String[]> finisher() {
		return strings -> strings.toArray( new String[strings.size()] );
	}

	@Override
	public Set<Characteristics> characteristics() {
		return EnumSet.of( Characteristics.CONCURRENT );
	}
}

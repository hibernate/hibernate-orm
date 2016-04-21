/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.stream.Stream;

import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.StreamSupport.stream;

/**
 *
 * Utilities for obtaining {@link Stream} with an underlying {@link ScrollableResults}
 *
 * @author Christophe Taret
 */
public class ScrollableResultsStreamSupport {

	private ScrollableResultsStreamSupport(){
	}

	/**
	 * @see Spliterator for description
	 */
	private static final int DEFAULT_CHARACTERISTICS =
		Spliterator.NONNULL |
		Spliterator.IMMUTABLE;

	/**
	 *
	 * @param scrollableResults the {@link ScrollableResults}
	 * @param type the class of the {@link Stream}
	 * @param <T> the type of the {@link Stream}
	 * @return a {@link Stream} with values from the {@link ScrollableResults}
	 */
	public static <T> Stream<T> asStream(ScrollableResults scrollableResults, Class<T> type){
		return asStream( scrollableResults, type, DEFAULT_CHARACTERISTICS, false );
	}

	/**
	 *
	 * @param scrollableResults the {@link ScrollableResults}
	 * @param type the class of the {@link Stream}
	 * @param <T> the type of the {@link Stream}
	 * @return a parallel {@link Stream} with values from the {@link ScrollableResults}
	 */
	public static <T> Stream<T> asParallelStream(ScrollableResults scrollableResults, Class<T> type){
		return asStream( scrollableResults, type, DEFAULT_CHARACTERISTICS, true );
	}

	private static <T> Stream<T> asStream(
			ScrollableResults scrollableResults,
			Class<T> type,
			int characteristics,
			boolean parallel) {
		// build the stream whether parallel or not, with all required information
		return stream(
				spliteratorUnknownSize(
						new ScrollableResultIterator<>( scrollableResults, type ),
						characteristics ),
				parallel );
	}

	/**
	 * A {@link Iterator} implementation with an underlying {@link ScrollableResults}
	 * @param <T>
	 */
	private static class ScrollableResultIterator<T> implements Iterator<T> {
		private final ScrollableResults sr;
		private final Class<T> type;

		ScrollableResultIterator(ScrollableResults sr, Class<T> type) {
			this.sr = sr;
			this.type = type;
		}

		@Override
		public boolean hasNext() {
			return !sr.isLast();
		}

		@Override
		public T next() {
			if (sr.next()){
				return type.cast( sr.get()[0] );
			}
			return null;
		}
	}
}

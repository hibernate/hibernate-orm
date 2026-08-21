/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.generator;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.hibernate.Incubating;
import org.jetbrains.annotations.NotNull;

/**
 * An ordered, read-only collection of {@link GenerationRequest} elements
 * passed to {@link BeforeExecutionGenerator#generateBatch}.
 *
 * @since 8.0
 *
 * @see BeforeExecutionGenerator#generateBatch
 */
@Incubating
public interface GenerationRequests extends Iterable<GenerationRequest> {

	/**
	 * The number of requests in this collection.
	 */
	int size();

	/**
	 * Returns the request at the given index.
	 *
	 * @param index the zero-based index
	 * @return the request at that position
	 * @throws IndexOutOfBoundsException if the index is out of range
	 */
	GenerationRequest get(int index);

	@Override
	default @NotNull Iterator<GenerationRequest> iterator() {
		return new Iterator<>() {
			int index = 0;

			@Override
			public boolean hasNext() {
				return index < size();
			}

			@Override
			public GenerationRequest next() {
				if ( !hasNext() ) {
					throw new NoSuchElementException();
				}
				return get( index++ );
			}
		};
	}

	/**
	 * Creates a {@link GenerationRequests} backed by the given list.
	 */
	static GenerationRequests of(List<? extends GenerationRequest> list) {
		return new GenerationRequests() {
			@Override
			public int size() {
				return list.size();
			}

			@Override
			public GenerationRequest get(int index) {
				return list.get( index );
			}
		};
	}

	/**
	 * Creates a {@link GenerationRequests} of the given size where each
	 * request has {@code null} entity and {@code null} current value.
	 * <p>
	 * Useful for bulk operations where individual entity instances
	 * are not available (e.g., {@code INSERT ... SELECT}).
	 */
	static GenerationRequests of(int size) {
		return new GenerationRequests() {
			@Override
			public int size() {
				return size;
			}

			@Override
			public GenerationRequest get(int index) {
				return GenerationRequest.EMPTY;
			}
		};
	}
}

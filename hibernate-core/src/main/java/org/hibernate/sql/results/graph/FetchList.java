/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph;

import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.hibernate.Incubating;
import org.hibernate.engine.FetchTiming;
import org.hibernate.internal.util.IndexedConsumer;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.sql.results.graph.collection.internal.EagerCollectionFetch;
import org.hibernate.sql.results.graph.embeddable.EmbeddableResultGraphNode;
import org.hibernate.sql.results.graph.entity.internal.AbstractNonJoinedEntityFetch;

/**
 * This is essentially a List of Fetch(es), but exposing
 * an interface which is more suitable to our needs; in particular
 * it expresses the immutable nature of this structure, and allows
 * us to extend it with additional convenience methods such as
 * {@link #indexedForEach(IndexedConsumer)}.
 * And additional reason for the custom interface is to allow
 * custom implementations which can be highly optimised as
 * necessary for our specific needs; for example the
 * implementation {@link org.hibernate.sql.results.graph.internal.ImmutableFetchList}
 * is able to avoid caching problems related to JDK-8180450, which would
 * not have been possible with a standard generic container.
 * @since 6.2
 */
@Incubating
public interface FetchList extends Iterable<Fetch> {

	int size();

	boolean isEmpty();

	Fetch get(Fetchable fetchable);

	void forEach(Consumer<? super Fetch> consumer);

	void indexedForEach(IndexedConsumer<? super Fetch> consumer);

	default Stream<Fetch> stream() {
		return StreamSupport.stream( spliterator(), false );
	}

	default boolean hasJoinFetches() {
		for ( Fetch fetch : this ) {
			if ( fetch instanceof BasicFetch<?> || fetch instanceof AbstractNonJoinedEntityFetch || fetch.getTiming() == FetchTiming.DELAYED ) {
				// That's fine
			}
			else if ( fetch instanceof EmbeddableResultGraphNode embeddableResultGraphNode ) {
				// Check all these fetches as well
				if ( embeddableResultGraphNode.hasJoinFetches() ) {
					return true;
				}
			}
			else {
				return true;
			}
		}
		return false;
	}

	default boolean containsCollectionFetches() {
		for ( Fetch fetch : this ) {
			if ( fetch instanceof EagerCollectionFetch ) {
				return true;
			}
			else if ( fetch.asFetchParent() != null && fetch.asFetchParent().containsCollectionFetches() ) {
				return true;
			}
		}
		return false;
	}

	default int getCollectionFetchesCount() {
		int collectionFetchesCount = 0;
		for ( Fetch fetch : this ) {
			if ( fetch instanceof EagerCollectionFetch ) {
				collectionFetchesCount++;
			}
			else {
				final FetchParent fetchParent = fetch.asFetchParent();
				if ( fetchParent != null ) {
					collectionFetchesCount += fetchParent.getCollectionFetchesCount();
				}
			}
		}
		return collectionFetchesCount;
	}

}

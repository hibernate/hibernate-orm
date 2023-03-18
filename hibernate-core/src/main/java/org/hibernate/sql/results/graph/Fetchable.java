/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph;

import org.hibernate.Incubating;
import org.hibernate.engine.FetchTiming;
import org.hibernate.internal.util.IndexedConsumer;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.spi.NavigablePath;

/**
 * Parts of the domain model that can be fetched.  In other words,
 * a {@link ModelPart} which can produce {@link Fetch} references.
 *
 * @author Steve Ebersole
 */
@Incubating
public interface Fetchable extends ModelPart {
	/**
	 * The name of the fetchable.  This is the part's "local name".
	 *
	 * @see #getNavigableRole()
	 * @see NavigableRole#getLocalName()
	 */
	String getFetchableName();

	/**
	 * The key that identifies this {@linkplain Fetchable} within a {@link FetchableContainer}.
	 * If this {@linkplain Fetchable} is part of {@link FetchableContainer#visitFetchables(IndexedConsumer, EntityMappingType)},
	 * the values is guaranteed to be between 0 (inclusive) and {@link FetchableContainer#getNumberOfFetchableKeys()} (exclusive).
	 * Other {@linkplain Fetchable} objects may have a special negative value.
	 * <p>
	 * The main intent of this key is to index e.g. {@link Fetch} objects in an array.
	 */
	int getFetchableKey();

	/**
	 * The configured fetch timing and style
	 */
	FetchOptions getMappedFetchOptions();

	/**
	 * Check whether this Fetchable is considered a circular fetch.
	 *
	 * @param fetchablePath The overall path within the graph
	 *
	 * @return The Fetch representing the circularity; {@code null} indicates the fetch is not circular
	 */
	default Fetch resolveCircularFetch(
			NavigablePath fetchablePath,
			FetchParent fetchParent,
			FetchTiming fetchTiming,
			DomainResultCreationState creationState) {
		return null;
	}

	/**
	 * Generates a Fetch of this fetchable
	 *
	 * @param fetchParent The parent of the Fetch we are generating
	 * @param fetchablePath The overall path within the graph
	 * @param fetchTiming The requested fetch timing
	 */
	Fetch generateFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			FetchTiming fetchTiming,
			boolean selected,
			String resultVariable,
			DomainResultCreationState creationState);

	/**
	 * Should this Fetchable affect the fetch depth?  E.g., composites
	 * would generally not increment the fetch depth.
	 *
	 * @see org.hibernate.cfg.AvailableSettings#MAX_FETCH_DEPTH
	 */
	default boolean incrementFetchDepth(){
		return false;
	}

	default AttributeMapping asAttributeMapping() {
		return null;
	}

	default boolean isSelectable() {
		final AttributeMapping attributeMapping = asAttributeMapping();
		if ( attributeMapping != null ) {
			return attributeMapping.getAttributeMetadata().isSelectable();
		}
		else {
			return true;
		}
	}
}

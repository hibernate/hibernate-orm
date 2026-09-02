/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph;

import org.hibernate.Incubating;
import org.hibernate.engine.FetchTiming;
import org.hibernate.spi.NavigablePath;

/**
 * Contract for fetches including entity, collection and composite.  Acts as the
 * producer for the {@link DomainResultAssembler} for this result as well
 * as any {@link Initializer} instances needed
 *
 * @see Fetchable#generateFetch(FetchParent, NavigablePath, FetchTiming, boolean, String, DomainResultCreationState)
 * @see org.hibernate.query.results.spi.FetchBuilder#buildFetch(FetchParent, NavigablePath, org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata, DomainResultCreationState)
 *
 * @author Steve Ebersole
 */
@Incubating
@org.hibernate.SPI({ org.hibernate.SPI.Role.USE, org.hibernate.SPI.Role.IMPLEMENT, org.hibernate.SPI.Role.SUPPLY })
public interface Fetch extends DomainResultGraphNode {
	/**
	 * Get the property path to this fetch
	 *
	 * @return The property path
	 */
	NavigablePath getNavigablePath();

	/**
	 * Obtain the owner of this fetch.  Ultimately used to identify
	 * the thing that "owns" this fetched navigable for the purpose of:
	 * <p>
	 * * identifying the associated owner reference as we process the fetch
	 * * inject the fetched instance into the parent and potentially inject
	 * the parent reference into the fetched instance if it defines
	 * such injection (e.g. {@link org.hibernate.annotations.Parent})
	 */
	FetchParent getFetchParent();

	/**
	 * Utility method to avoid {@code instanceof} checks. Returns this if it's
	 * an instance of {@link FetchParent}, null otherwise.
	 */
	default FetchParent asFetchParent() {
		return null;
	}

	/**
	 * The value mapping being fetched
	 */
	Fetchable getFetchedMapping();

	/**
	 * immediate or delayed?
	 *
	 * todo (6.0) : should we also expose the fetch-style?  Perhaps the fetch-options?
	 */
	FetchTiming getTiming();

	/**
	 * Is the TableGroup associated with this Fetch defined?
	 */
	boolean hasTableGroup();

	/// Whether this fetch represents an eagerly fetched collection.
	///
	/// Provider implementations which model an eager collection fetch must
	/// override this method so fetch-list collection accounting remains correct.
	///
	/// @since 8.0
	default boolean isCollectionFetch() {
		return false;
	}

	@Override
	default boolean containsAnyNonScalarResults() {
		return true;
	}

	/**
	 * Create the assembler for this fetch
	 *
	 * @see DomainResultAssembler
	 */
	@org.hibernate.SPI(org.hibernate.SPI.Role.SUPPLY)
	DomainResultAssembler<?> createAssembler(
			InitializerParent<?> parent,
			AssemblerCreationState creationState);
}

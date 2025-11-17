/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph;

import java.util.BitSet;

import org.hibernate.spi.NavigablePath;

/**
 * @asciidoc
 *
 * Marker interface for Fetches that are actually references to
 * another fetch based on "normalized navigable path"
 *
 * The following query is used throughout the javadocs for these impls
 * to help describe what it going on and why certain methods do certain things.
 *
 *
 * ```
 * @Entity
 * class Person {
 *     ...
 *     @ManyToOne (mappedBy="owner")
 *     Address getAddress() {...}
 * }
 *
 * @Entity
 * class Address {
 *     ...
 *     @ManyToOne
 *     Person getOwner() {...}
 * }
 *
 * from Person p
 * 		join fetch p.address a
 * 		join fetch a.owner o
 * 		join fetch o.address oa
 * ```
 *
 *
 * Here we have one root result and 3 fetches.  2 of the fetches are bidirectional:
 *
 * 		`o`:: The paths `p` and `p.address.owner` (aliased as `o`) are the same table reference in SQL terms
 * 		`oa`:: The paths `p.address` and `p.address.owner.address` (aliased as `oa`) are again the same table reference
 *
 * @author Steve Ebersole
 */
public interface BiDirectionalFetch extends Fetch {
	/**
	 * The NavigablePath for the DomainResult or Fetch that this Fetch refers to.
	 *
	 * For `o`, the referenced path is `p`.  For `oa`, it's `p.address`
	 *
	 * Different from {@link #getNavigablePath()} which returns this fetch's path, i.e.
	 * `p.address.owner` and `p.address.owner.address` respectively
	 */
	NavigablePath getReferencedPath();

	@Override
	default void collectValueIndexesToCache(BitSet valueIndexes) {
		// No-op
	}
}

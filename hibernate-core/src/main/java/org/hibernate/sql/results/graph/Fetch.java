/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph;

import java.util.function.Consumer;

import org.hibernate.engine.FetchTiming;
import org.hibernate.query.NavigablePath;

/**
 * Contract for fetches including entity, collection and composite.  Acts as the
 * producer for the {@link DomainResultAssembler} for this result as well
 * as any {@link Initializer} instances needed
 *
 * @author Steve Ebersole
 */
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
	 *
	 * todo (6.0) : remove?
	 * 		- this is never used.  not sure its useful, and it creates a bi-directional link between
	 * 		Fetch#getParent and FetchParent#getFetches
	 */
	FetchParent getFetchParent();

	/**
	 * The value mapping being fetched
	 */
	Fetchable getFetchedMapping();

	/**
	 * immediate or delayed?
	 */
	FetchTiming getTiming();

	/**
	 * Is the TableGroup associated with this Fetch defined?
	 */
	boolean hasTableGroup();

	/**
	 * Create the assembler for this fetch
	 */
	DomainResultAssembler createAssembler(FetchParentAccess parentAccess, AssemblerCreationState creationState);
}

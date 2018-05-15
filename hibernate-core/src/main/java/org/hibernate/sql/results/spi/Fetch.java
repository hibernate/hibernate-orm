/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

import java.util.function.Consumer;

import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.query.NavigablePath;

/**
 * Contract for fetches including entity, collection and composite.  Acts as the
 * producer for the {@link DomainResultAssembler} for this result as well
 * as any {@link Initializer} instances needed
 *
 * todo (6.0) : we have fetch -> fetch-parent at the initializer level.  Do we also need fetch-parent -> fetch(es)?
 * 		- depends how the parent state gets resolved for injection into the parent instance
 *
 * @see EntityFetch
 * @see CollectionFetch
 * @see CompositeFetch
 *
 * @author Steve Ebersole
 */
public interface Fetch extends ResultSetMappingNode {
	/**
	 * Obtain the owner of this fetch.  Ultimately used to identify
	 * the thing that "owns" this fetched navigable for the purpose of:
	 *
	 * 		* identifying the associated owner reference as we process the fetch
	 * 		* inject the fetched instance into the parent and potentially inject
	 * 			the parent reference into the fetched instance if it defines
	 * 			such injection (e.g. {@link org.hibernate.annotations.Parent})
	 */
	FetchParent getFetchParent();

	/**
	 * The Navigable being fetched
	 */
	Navigable getFetchedNavigable();

	/**
	 * Get the navigable name of the fetched navigable
	 *
	 * @return The navigable name
	 */
	default String getFetchedNavigableName(){
		return getFetchedNavigable().getNavigableName();
	}

	/**
	 * Get the property path to this fetch
	 *
	 * @return The property path
	 */
	NavigablePath getNavigablePath();

	/**
	 * Is this fetch nullable?
	 *
	 * @return true, if this fetch is nullable; false, otherwise.
	 *
	 * todo (6.0) : isn't this more a function of the mapping (Navigable)?
	 * 		- maybe it is needed in the case of an inner-join versus outer-join fetch;
	 * 			but if so, how does this interact with mapped nullability (optional)
	 */
	boolean isNullable();

	DomainResultAssembler createAssembler(
			FetchParentAccess parentAccess,
			Consumer<Initializer> collector,
			AssemblerCreationContext creationContext,
			AssemblerCreationState creationState);
}

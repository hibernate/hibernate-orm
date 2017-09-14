/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

import org.hibernate.engine.FetchStrategy;
import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;

/**
 * Contract for fetches including entity, collection and composite.  Acts as the
 * producer for the {@link QueryResultAssembler} for this result as well
 * as any {@link Initializer} instances needed
 *
 * todo (6.0) : we have fetch -> fetch-parent at the initializer level.  Do we also need fetch-parent -> fetch(es)?
 * 		- depends how the parent state gets resolved for injection into the parent instance
 *
 * @see EntityFetch
 * @see PluralAttributeFetch
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

	ColumnReferenceQualifier getSqlExpressionQualifier();

	/**
	 * The Navigable being fetched
	 */
	Navigable getFetchedNavigable();

	/**
	 * Get the property path to this fetch
	 *
	 * @return The property path
	 */
	NavigablePath getNavigablePath();

	/**
	 * Gets the fetch strategy for this fetch.
	 *
	 * @return the fetch strategy for this fetch.
	 */
	FetchStrategy getFetchStrategy();

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

	void registerInitializers(FetchParentAccess parentAccess, InitializerCollector collector);
}

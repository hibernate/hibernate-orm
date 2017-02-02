/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.spi;

import org.hibernate.loader.PropertyPath;

/**
 * Contract for a FetchSource (aka, the thing that owns the fetched attribute).
 *
 *
 * @author Steve Ebersole
 */
public interface FetchSource {

	/**
	 * Get the property path to this fetch source
	 *
	 * @return The property path
	 */
	public PropertyPath getPropertyPath();

	/**
	 * Get the UID for this fetch source's query space.
	 *
	 * @return The query space UID.
	 */
	public String getQuerySpaceUid();

	/**
	 * Retrieve the fetches owned by this fetch source.
	 *
	 * @return The owned fetches.
	 */
	public Fetch[] getFetches();

	/**
	 * Retrieve the bidirectional entity references owned by this fetch source.
	 *
	 * @return The owned bidirectional entity references.
	 */
	public BidirectionalEntityReference[] getBidirectionalEntityReferences();

	/**
	 * Resolve the "current" {@link EntityReference}, or null if none.
	 *
	 * If this object is an {@link EntityReference}, then this object is returned;
	 * otherwise, if this object is a {@link Fetch}, then the nearest
	 * {@link EntityReference} will be resolved from its source, if possible.
	 *
	 * If no EntityReference can be resolved, null is return.
	 *
	 *  @return the "current" EntityReference or null if none.
	 * .
	 * @see org.hibernate.loader.plan.spi.Fetch#getSource().
	 */
	public EntityReference resolveEntityReference();
}

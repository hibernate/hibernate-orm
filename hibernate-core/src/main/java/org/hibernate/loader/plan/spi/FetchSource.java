/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2013, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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

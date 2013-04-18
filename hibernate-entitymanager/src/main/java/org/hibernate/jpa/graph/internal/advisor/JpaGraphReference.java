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
package org.hibernate.jpa.graph.internal.advisor;

import org.hibernate.loader.plan.spi.FetchOwner;

/**
 * Describes a reference to a JPA graph.  This encompasses both {@link javax.persistence.EntityGraph}
 * and {@link javax.persistence.Subgraph}.  Exposes the functionality needed for advising in a common way.
 *
 * @author Steve Ebersole
 */
interface JpaGraphReference {
	/**
	 * Callback to let the JPA graph reference node know that the particular attribute (by name) was processed, which
	 * means it already was accounted for in the LoadPlan graph.  For association attributes and composites, also
	 * returns a representation of the corresponding JPA graph node.
	 *
	 * @param attributeName The name of the attribute processed.
	 *
	 * @return The JPA graph reference corresponding to that attribute, if one.
	 */
	public JpaGraphReference attributeProcessed(String attributeName);

	/**
	 * For any attributes that are defined in the JPA graph, that were not processed (as would have been indicated
	 * by a previous call to {@link #attributeProcessed}), apply needed fetches to the fetch owner.
	 *
	 * @param fetchOwner The owner of any generated fetches.
	 */
	public void applyMissingFetches(FetchOwner fetchOwner);
}

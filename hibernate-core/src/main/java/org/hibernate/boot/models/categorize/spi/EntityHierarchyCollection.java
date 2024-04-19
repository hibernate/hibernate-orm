/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.categorize.spi;

import java.util.Collection;
import java.util.function.Consumer;

import org.hibernate.models.spi.ClassDetails;

/**
 * Aggregation of all {@linkplain EntityHierarchy hierarchies}.
 *
 * @see org.hibernate.boot.models.categorize.internal.EntityHierarchyBuilder#createEntityHierarchies
 *
 * @author Steve Ebersole
 */
public interface EntityHierarchyCollection {
	/**
	 * All hierarchies
	 */
	Collection<EntityHierarchy> getHierarchies();

	/**
	 * Visit each hierarchy
	 */
	void forEachHierarchy(Consumer<EntityHierarchy> consumer);

	/**
	 * Determine the hierarchy of which a given entity or mapped-superclass is part.
	 */
	EntityHierarchy determineEntityHierarchy(ClassDetails classToLookFor);
}

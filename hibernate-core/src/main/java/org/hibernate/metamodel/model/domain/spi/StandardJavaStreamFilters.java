/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.util.function.Predicate;

/**
 * Navigable / PersistentAttribute related Java Stream filters.
 *
 * @author Steve Ebersole
 */
public class StandardJavaStreamFilters {
	/**
	 * Functional predicate for filtering a stream of attributes, removing plural attributes
	 */
	public static final Predicate<PersistentAttributeDescriptor<?,?>> NON_PLURAL_ATTRIBUTES
			= persistentAttribute -> !PluralPersistentAttribute.class.isInstance( persistentAttribute );


	/**
	 * Disallow instantiation
	 */
	private StandardJavaStreamFilters() {
	}
}

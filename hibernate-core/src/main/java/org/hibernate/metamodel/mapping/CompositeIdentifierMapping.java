/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import java.util.Collection;

/**
 * Support for composite identifier mappings
 *
 * @author Andrea Boriero
 */
public interface CompositeIdentifierMapping extends EntityIdentifierMapping {
	/**
	 * The number of attributes associated with this composite
	 */
	int getAttributeCount();

	/**
	 * The attributes associated with this composite
	 */
	Collection<SingularAttributeMapping> getAttributes();
}

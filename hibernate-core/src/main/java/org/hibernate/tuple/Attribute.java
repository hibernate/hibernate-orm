/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple;

import org.hibernate.type.Type;

/**
 * @deprecated Replaced by {@link org.hibernate.metamodel.mapping.AttributeMapping}
 */
@Deprecated(forRemoval = true)
public interface Attribute {
	String getName();
	Type getType();
}

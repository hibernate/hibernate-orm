/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.hql.internal.ast.tree;

import org.hibernate.type.Type;

/**
 * Represents a reference to one of the "collection properties".
 *
 * @see org.hibernate.hql.internal.CollectionProperties
 *
 * @author Steve Ebersole
 */
public interface CollectionPropertyReference {
	Type getType();

	String[] toColumns(String tableAlias);
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.ordering.internal;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.model.relational.spi.PhysicalColumn;

/**
 * Given a column reference, resolve the table alias to apply to the column to qualify it.
 */
public interface OrderByAliasResolver {
	SessionFactoryImplementor getSessionFactory();

	/**
	 * Given a column reference, resolve the table alias to apply to the column to qualify it.
	 *
	 */
	String resolveTableAlias(PhysicalColumn referencedColumn);
}

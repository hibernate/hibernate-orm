/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.persister.common.spi;

import java.util.Collection;

/**
 * Represents a table in the mapping.  The name "table reference" comes from ANSI SQL
 * to describe the fact that the "table" might be a derived table (in-line view) or
 * a physical table reference.
 *
 * @author Steve Ebersole
 */
public interface Table {
	String getTableExpression();

	Column getColumn(String name);

	Collection<Column> getColumns();
}

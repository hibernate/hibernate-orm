/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.relational.spi;

import java.util.Collection;


/**
 * Represents a table in the mapping in terms of what ANSI SQL calls a
 * "table reference".  Specifically, this models the commonality between
 * a physical table reference and a derived table (in-line view).
 *
 * @author Steve Ebersole
 */
public interface Table {
	String getTableExpression();

	PrimaryKey getPrimaryKey();

	boolean hasPrimaryKey();

	boolean isAbstract();

	boolean isExportable();

	Collection<Column> getColumns();

	Column getColumn(String name);

	Collection<ForeignKey> getForeignKeys();

	Collection<UniqueKey> getUniqueKeys();
}

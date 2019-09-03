/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.relational;

/**
 * A table (or view, etc) that is physically defined on the database.
 *
 * @author Steve Ebersole
 */
public class PhysicalTable implements Table {
	private final Identifier catalogName;
	private final Identifier schemaName;
	private final Identifier tableName;

	public PhysicalTable(
			Identifier catalogName,
			Identifier schemaName,
			Identifier tableName) {
		this.catalogName = catalogName;
		this.schemaName = schemaName;
		this.tableName = tableName;
	}

	public Identifier getCatalogName() {
		return catalogName;
	}

	public Identifier getSchemaName() {
		return schemaName;
	}

	public Identifier getTableName() {
		return tableName;
	}

	@Override
	public String getTableExpression() {
		return getTableName().render();
	}
}

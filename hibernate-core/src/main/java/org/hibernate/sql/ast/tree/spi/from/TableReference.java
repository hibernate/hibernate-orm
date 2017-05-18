/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.from;

import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.tree.spi.predicate.SqlAstNode;

/**
 * Represents a reference to a table (derived or physical) in a query's from clause.
 *
 * @author Steve Ebersole
 */
public class TableReference implements SqlAstNode {
	private final Table table;
	private final String identificationVariable;

	public TableReference(Table table, String identificationVariable) {
		this.table = table;
		this.identificationVariable = identificationVariable;
	}

	public Table getTable() {
		return table;
	}

	public String getIdentificationVariable() {
		return identificationVariable;
	}

	@Override
	public void accept(SqlAstWalker  sqlTreeWalker) {
		sqlTreeWalker.visitTableReference( this );
	}
}

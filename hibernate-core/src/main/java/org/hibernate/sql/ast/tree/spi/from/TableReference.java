/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.from;

import java.util.HashMap;
import java.util.Map;

import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.sql.ast.consume.spi.SqlAstWalker;
import org.hibernate.sql.ast.produce.spi.ColumnReferenceQualifier;
import org.hibernate.sql.ast.produce.spi.QualifiableSqlExpressable;
import org.hibernate.sql.ast.tree.spi.SqlAstNode;
import org.hibernate.sql.ast.tree.spi.expression.ColumnReference;
import org.hibernate.sql.ast.tree.spi.expression.Expression;

/**
 * Represents a reference to a table (derived or physical) in a query's from clause.
 *
 * @author Steve Ebersole
 */
public class TableReference implements SqlAstNode, ColumnReferenceQualifier {
	private final Table table;
	private final String identificationVariable;

	private final Map<Column,ColumnReference> columnReferenceResolutionMap = new HashMap<>();

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
	public String getUniqueIdentifier() {
		// the uid is for TableGroups
		return null;
	}

	@Override
	public TableReference locateTableReference(Table table) {
		if ( table.equals( getTable() ) ) {
			return this;
		}
		return null;
	}

	@Override
	public ColumnReference resolveColumnReference(Column column) {
		final ColumnReference existing = columnReferenceResolutionMap.get( column );
		if ( existing != null ) {
			return existing;
		}

		final ColumnReference columnReference = new ColumnReference( this, column );
		columnReferenceResolutionMap.put( column, columnReference );
		return columnReference;
	}

	@Override
	public void accept(SqlAstWalker  sqlTreeWalker) {
		sqlTreeWalker.visitTableReference( this );
	}

	@Override
	public Expression qualify(QualifiableSqlExpressable sqlSelectable) {
		assert sqlSelectable instanceof Column;
		return resolveColumnReference( (Column) sqlSelectable );
	}
}

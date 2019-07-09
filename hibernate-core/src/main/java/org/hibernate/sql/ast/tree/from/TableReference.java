/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.from;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.sql.ast.spi.SqlAstWalker;
import org.hibernate.sql.ast.tree.SqlAstNode;
import org.hibernate.sql.ast.tree.expression.ColumnReference;

/**
 * Represents a reference to a table (derived or physical) in a query's from clause.
 *
 * @author Steve Ebersole
 */
public class TableReference implements SqlAstNode {
	private final String tableName;
	private final String identificationVariable;

	private final boolean isOptional;

	private final Map<Identifier, ColumnReference> columnReferenceResolutionMap = new HashMap<>();

	public TableReference(String tableName, String identificationVariable, boolean isOptional) {
		this.tableName = tableName;
		this.identificationVariable = identificationVariable;
		this.isOptional = isOptional;
	}

	public String getTableName() {
		return tableName;
	}

	public String getIdentificationVariable() {
		return identificationVariable;
	}

	public boolean isOptional() {
		return isOptional;
	}

	@Override
	public void accept(SqlAstWalker sqlTreeWalker) {
		sqlTreeWalker.visitTableReference( this );
	}

//	@Override
//	public TableReference locateTableReference(Table table) {
//		if ( table.equals( getTableName() ) ) {
//			return this;
//		}
//		return null;
//	}
//
//	@Override
//	public ColumnReference resolveColumnReference(Column column) {
//		final ColumnReference existing = columnReferenceResolutionMap.get( column );
//		if ( existing != null ) {
//			return existing;
//		}
//
//		final ColumnReference columnReference = new ColumnReference( this, column );
//		columnReferenceResolutionMap.put( column, columnReference );
//		return columnReference;
//	}
//
//	@Override
//	public ColumnReference resolveColumnReference(String columnName) {
//		return resolveColumnReference( getTableName().getColumn( columnName ) );
//	}
//
//	@Override
//	public Column resolveColumn(String columnName) {
//		return getTableName().getColumn( columnName );
//	}
//
//	@Override
//	public Expression qualify(QualifiableSqlExpressable sqlSelectable) {
//		assert sqlSelectable instanceof Column;
//		return resolveColumnReference( (Column) sqlSelectable );
//	}
//
//	@Override
//	public String toLoggableFragment() {
//		return getTableName().toLoggableFragment() + "(" + getIdentificationVariable() + ')';
//	}

	@Override
	public String toString() {
		return getTableName() + "(" + getIdentificationVariable() + ')';
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		TableReference that = (TableReference) o;
		return Objects.equals( identificationVariable, that.identificationVariable );
	}

	@Override
	public int hashCode() {
		return Objects.hash( identificationVariable );
	}
}

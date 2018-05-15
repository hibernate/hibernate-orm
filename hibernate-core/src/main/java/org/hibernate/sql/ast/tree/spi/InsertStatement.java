/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.sql.ast.tree.spi.expression.ColumnReference;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.ast.tree.spi.from.TableReference;

/**
 * @author Steve Ebersole
 */
public class InsertStatement implements MutationStatement {
	private TableReference targetTable;
	private List<ColumnReference> targetColumnReferences;
	private List<Expression> values;

	public InsertStatement(TableReference targetTable) {
		this.targetTable = targetTable;
	}

	public TableReference getTargetTable() {
		return targetTable;
	}

	public void setTargetTable(TableReference targetTable) {
		this.targetTable = targetTable;
	}

	public List<ColumnReference> getTargetColumnReferences() {
		return targetColumnReferences;
	}

	public void setTargetColumnReferences(List<ColumnReference> targetColumnReferences) {
		this.targetColumnReferences = targetColumnReferences;
	}

	public void addTargetColumnReference(ColumnReference columnReference) {
		if ( targetColumnReferences == null ) {
			targetColumnReferences = new ArrayList<>();
		}
		targetColumnReferences.add( columnReference );
	}

	public List<Expression> getValues() {
		return values;
	}

	public void setValues(List<Expression> values) {
		this.values = values;
	}

	public void addValue(Expression expression) {
		if ( values == null ) {
			values = new ArrayList<>();
		}
		values.add( expression );
	}
}

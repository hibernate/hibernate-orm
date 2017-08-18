/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.relational.spi;

import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class DerivedColumn implements Column {
	private final Table table;
	private final String expression;
	private SqlTypeDescriptor sqlTypeDescriptor;

	public DerivedColumn(Table table, String expression, SqlTypeDescriptor sqlTypeDescriptor) {
		this.table = table;
		this.expression = expression;
		this.sqlTypeDescriptor = sqlTypeDescriptor;
	}

	public String getExpression() {
		return expression;
	}

	@Override
	public Table getSourceTable() {
		return table;
	}

	@Override
	public String toLoggableString() {
		return "DerivedColumn( " + expression + ")";
	}

	@Override
	public String toString() {
		return toLoggableString();
	}

	@Override
	public String render(String identificationVariable) {
		return expression;
	}

	@Override
	public SqlTypeDescriptor getSqlTypeDescriptor() {
		return sqlTypeDescriptor;
	}
}

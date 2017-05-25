/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.relational.spi;

import org.hibernate.naming.Identifier;
import org.hibernate.type.descriptor.sql.spi.SqlTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class PhysicalColumn implements Column {
	private final Table table;
	private final Identifier name;
	private final SqlTypeDescriptor sqlTypeDescriptor;

	public PhysicalColumn(Table table, Identifier name, SqlTypeDescriptor sqlTypeDescriptor) {
		this.table = table;
		this.name = name;
		this.sqlTypeDescriptor = sqlTypeDescriptor;
	}

	public Identifier getName() {
		return name;
	}

	@Override
	public Table getSourceTable() {
		return table;
	}

	@Override
	public String getExpression() {
		return name.getCanonicalName();
	}

	@Override
	public SqlTypeDescriptor getSqlTypeDescriptor() {
		return sqlTypeDescriptor;
	}

	@Override
	public String render(String identificationVariable) {
		return identificationVariable + '.' + name;
	}

	@Override
	public String toString() {
		return "PhysicalColumn(" + table.getTableExpression() + " : " + name + ")";
	}

	@Override
	public String toLoggableString() {
		return "PhysicalColumn(" + name + ");";
	}
}

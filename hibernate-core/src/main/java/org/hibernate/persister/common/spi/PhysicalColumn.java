/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.persister.common.spi;

/**
 * @author Steve Ebersole
 */
public class PhysicalColumn implements Column {
	private final Table table;
	private final String name;
	private final int jdbcType;

	public PhysicalColumn(Table table, String name, int jdbcType) {
		this.table = table;
		this.name = name;
		this.jdbcType = jdbcType;
	}

	public String getName() {
		return name;
	}

	@Override
	public Table getSourceTable() {
		return table;
	}

	@Override
	public String getExpression() {
		return name;
	}

	@Override
	public int getJdbcType() {
		return jdbcType;
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

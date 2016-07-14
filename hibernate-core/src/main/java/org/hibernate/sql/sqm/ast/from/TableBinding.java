/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.sqm.ast.from;

import org.hibernate.persister.common.spi.Table;

/**
 * Represents a binding of a table (derived or physical) into a from-clause.
 *
 * @author Steve Ebersole
 */
public class TableBinding {
	private final Table table;
	private final String identificationVariable;

	public TableBinding(Table table, String identificationVariable) {
		this.table = table;
		this.identificationVariable = identificationVariable;
	}

	public Table getTable() {
		return table;
	}

	public String getIdentificationVariable() {
		return identificationVariable;
	}
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.sqm.ast.from;

import org.hibernate.persister.common.spi.Column;

/**
 * Represents a binding of a column (derived or physical) into a SQL statement
 *
 * @author Steve Ebersole
 */
public class ColumnBinding {
	private final Column column;
	private final String identificationVariable;

	public ColumnBinding(Column column, TableBinding tableBinding) {
		this.column = column;
		this.identificationVariable = tableBinding.getIdentificationVariable();
	}

	public Column getColumn() {
		return column;
	}

	public String getIdentificationVariable() {
		return identificationVariable;
	}
}

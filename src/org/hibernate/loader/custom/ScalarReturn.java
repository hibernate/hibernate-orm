package org.hibernate.loader.custom;

import org.hibernate.type.Type;

/**
 * Represent a scalar (aka simple value) return within a query result.
 *
 * @author Steve Ebersole
 */
public class ScalarReturn implements Return {
	private final Type type;
	private final String columnAlias;

	public ScalarReturn(Type type, String columnAlias) {
		this.type = type;
		this.columnAlias = columnAlias;
	}

	public Type getType() {
		return type;
	}

	public String getColumnAlias() {
		return columnAlias;
	}
}

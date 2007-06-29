package org.hibernate.engine.query.sql;

import org.hibernate.type.Type;

/**
 * Describes a scalar return in a native SQL query.
 *
 * @author gloegl
 */
public class NativeSQLQueryScalarReturn implements NativeSQLQueryReturn {
	private Type type;
	private String columnAlias;

	public NativeSQLQueryScalarReturn(String alias, Type type) {
		this.type = type;
		this.columnAlias = alias;
	}

	public String getColumnAlias() {
		return columnAlias;
	}

	public Type getType() {
		return type;
	}

}

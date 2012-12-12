package org.hibernate.cfg;

/**
 * @author Lukasz Antoniak (lukasz dot antoniak at gmail dot com)
 */
public enum NullOrderType {
	NONE(null), FIRST("nulls first"), LAST("nulls last");

	private final String sqlClause;

	private NullOrderType(String sqlClause) {
		this.sqlClause = sqlClause;
	}

	public String getSqlClause() {
		return sqlClause;
	}

	public static NullOrderType getType(String type) {
		if ( "first".equals( type ) ) {
			return FIRST;
		}
		else if ( "last".equals( type ) ) {
			return LAST;
		}
		return NONE;
	}
}

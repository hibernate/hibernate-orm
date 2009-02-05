package org.hibernate.dialect;

/**
 * An SQL dialect for DB2/400.  This class provides support for DB2 Universal Database for iSeries,
 * also known as DB2/400.
 *
 * @author Peter DeGregorio (pdegregorio)
 */
public class DB2400Dialect extends DB2Dialect {

	public boolean supportsSequences() {
		return false;
	}

	public String getIdentitySelectString() {
		return "select identity_val_local() from sysibm.sysdummy1";
	}

	public boolean supportsLimit() {
		return true;
	}

	public boolean supportsLimitOffset() {
		return false;
	}

	public boolean useMaxForLimit() {
		return true;
	}

	public boolean supportsVariableLimit() {
		return false;
	}

	public String getLimitString(String sql, int offset, int limit) {
		if ( offset > 0 ) {
			throw new UnsupportedOperationException( "query result offset is not supported" );
		}
		return new StringBuffer( sql.length() + 40 )
				.append( sql )
				.append( " fetch first " )
				.append( limit )
				.append( " rows only " )
				.toString();
	}

}
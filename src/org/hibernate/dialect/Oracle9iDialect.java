package org.hibernate.dialect;

import java.sql.Types;

import org.hibernate.sql.CaseFragment;
import org.hibernate.sql.ANSICaseFragment;

/**
 * A dialect for Oracle 9i databases.
 * <p/>
 * Unlike the older (deprecated) {@Link Oracl9Dialect), this version specifies
 * to not use "ANSI join syntax" because 9i does not seem to properly
 * handle it in all cases.
 *
 * @author Steve Ebersole
 */
public class Oracle9iDialect extends Oracle8iDialect {
	protected void registerCharacterTypeMappings() {
		registerColumnType( Types.CHAR, "char(1 char)" );
		registerColumnType( Types.VARCHAR, 4000, "varchar2($l char)" );
		registerColumnType( Types.VARCHAR, "long" );
	}

	protected void registerDateTimeTypeMappings() {
		registerColumnType( Types.DATE, "date" );
		registerColumnType( Types.TIME, "date" );
		registerColumnType( Types.TIMESTAMP, "timestamp" );
	}

	public CaseFragment createCaseFragment() {
		// Oracle did add support for ANSI CASE statements in 9i
		return new ANSICaseFragment();
	}

	public String getLimitString(String sql, boolean hasOffset) {
		sql = sql.trim();
		boolean isForUpdate = false;
		if ( sql.toLowerCase().endsWith(" for update") ) {
			sql = sql.substring( 0, sql.length()-11 );
			isForUpdate = true;
		}

		StringBuffer pagingSelect = new StringBuffer( sql.length()+100 );
		if (hasOffset) {
			pagingSelect.append("select * from ( select row_.*, rownum rownum_ from ( ");
		}
		else {
			pagingSelect.append("select * from ( ");
		}
		pagingSelect.append(sql);
		if (hasOffset) {
			pagingSelect.append(" ) row_ where rownum <= ?) where rownum_ > ?");
		}
		else {
			pagingSelect.append(" ) where rownum <= ?");
		}

		if ( isForUpdate ) {
			pagingSelect.append( " for update" );
		}

		return pagingSelect.toString();
	}

	public String getSelectClauseNullString(int sqlType) {
		return getBasicSelectClauseNullString( sqlType );
	}

	public String getCurrentTimestampSelectString() {
		return "select systimestamp from dual";
	}

	public String getCurrentTimestampSQLFunctionName() {
		// the standard SQL function name is current_timestamp...
		return "current_timestamp";
	}
}
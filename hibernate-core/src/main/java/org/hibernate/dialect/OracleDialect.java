/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.dialect;
import java.sql.Types;

import org.jboss.logging.Logger;

import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.sql.CaseFragment;
import org.hibernate.sql.DecodeCaseFragment;
import org.hibernate.sql.JoinFragment;
import org.hibernate.sql.OracleJoinFragment;

/**
 * An SQL dialect for Oracle, compatible with Oracle 8.
 *
 * @deprecated Use Oracle8iDialect instead.
 * @author Gavin King
 */
@Deprecated
public class OracleDialect extends Oracle9Dialect {

	private static final CoreMessageLogger LOG = Logger.getMessageLogger(CoreMessageLogger.class, OracleDialect.class.getName());

	public OracleDialect() {
		super();
		LOG.deprecatedOracleDialect();
		// Oracle8 and previous define only a "DATE" type which
		//      is used to represent all aspects of date/time
		registerColumnType( Types.TIMESTAMP, "date" );
		registerColumnType( Types.CHAR, "char(1)" );
		registerColumnType( Types.VARCHAR, 4000, "varchar2($l)" );
	}

	@Override
    public JoinFragment createOuterJoinFragment() {
		return new OracleJoinFragment();
	}
	@Override
    public CaseFragment createCaseFragment() {
		return new DecodeCaseFragment();
	}

	@Override
    public String getLimitString(String sql, boolean hasOffset) {

		sql = sql.trim();
		boolean isForUpdate = false;
		if ( sql.toLowerCase().endsWith(" for update") ) {
			sql = sql.substring( 0, sql.length()-11 );
			isForUpdate = true;
		}

		StringBuilder pagingSelect = new StringBuilder( sql.length()+100 );
		if (hasOffset) {
			pagingSelect.append("select * from ( select row_.*, rownum rownum_ from ( ");
		}
		else {
			pagingSelect.append("select * from ( ");
		}
		pagingSelect.append(sql);
		if (hasOffset) {
			pagingSelect.append(" ) row_ ) where rownum_ <= ? and rownum_ > ?");
		}
		else {
			pagingSelect.append(" ) where rownum <= ?");
		}

		if ( isForUpdate ) {
			pagingSelect.append( " for update" );
		}

		return pagingSelect.toString();
	}

	@Override
    public String getSelectClauseNullString(int sqlType) {
		switch(sqlType) {
			case Types.VARCHAR:
			case Types.CHAR:
				return "to_char(null)";
			case Types.DATE:
			case Types.TIMESTAMP:
			case Types.TIME:
				return "to_date(null)";
			default:
				return "to_number(null)";
		}
	}

	@Override
    public String getCurrentTimestampSelectString() {
		return "select sysdate from dual";
	}

	@Override
    public String getCurrentTimestampSQLFunctionName() {
		return "sysdate";
	}
}

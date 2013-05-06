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

import org.hibernate.HibernateException;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.dialect.function.VarArgsSQLFunction;
import org.hibernate.type.StandardBasicTypes;

/**
 * A dialect for the Teradata database created by MCR as part of the
 * dialect certification process.
 *
 * @author Jay Nance
 */
public class TeradataDialect extends Dialect {
	private static final int PARAM_LIST_SIZE_LIMIT = 1024;

	/**
	 * Constructor
	 */
	public TeradataDialect() {
		super();
		//registerColumnType data types
		registerColumnType( Types.NUMERIC, "NUMERIC($p,$s)" );
		registerColumnType( Types.DOUBLE, "DOUBLE PRECISION" );
		registerColumnType( Types.BIGINT, "NUMERIC(18,0)" );
		registerColumnType( Types.BIT, "BYTEINT" );
		registerColumnType( Types.TINYINT, "BYTEINT" );
		registerColumnType( Types.VARBINARY, "VARBYTE($l)" );
		registerColumnType( Types.BINARY, "BYTEINT" );
		registerColumnType( Types.LONGVARCHAR, "LONG VARCHAR" );
		registerColumnType( Types.CHAR, "CHAR(1)" );
		registerColumnType( Types.DECIMAL, "DECIMAL" );
		registerColumnType( Types.INTEGER, "INTEGER" );
		registerColumnType( Types.SMALLINT, "SMALLINT" );
		registerColumnType( Types.FLOAT, "FLOAT" );
		registerColumnType( Types.VARCHAR, "VARCHAR($l)" );
		registerColumnType( Types.DATE, "DATE" );
		registerColumnType( Types.TIME, "TIME" );
		registerColumnType( Types.TIMESTAMP, "TIMESTAMP" );
		// hibernate seems to ignore this type...
		registerColumnType( Types.BOOLEAN, "BYTEINT" );
		registerColumnType( Types.BLOB, "BLOB" );
		registerColumnType( Types.CLOB, "CLOB" );

		registerFunction( "year", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "extract(year from ?1)" ) );
		registerFunction( "length", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "character_length(?1)" ) );
		registerFunction( "concat", new VarArgsSQLFunction( StandardBasicTypes.STRING, "(", "||", ")" ) );
		registerFunction( "substring", new SQLFunctionTemplate( StandardBasicTypes.STRING, "substring(?1 from ?2 for ?3)" ) );
		registerFunction( "locate", new SQLFunctionTemplate( StandardBasicTypes.STRING, "position(?1 in ?2)" ) );
		registerFunction( "mod", new SQLFunctionTemplate( StandardBasicTypes.STRING, "?1 mod ?2" ) );
		registerFunction( "str", new SQLFunctionTemplate( StandardBasicTypes.STRING, "cast(?1 as varchar(255))" ) );

		// bit_length feels a bit broken to me. We have to cast to char in order to
		// pass when a numeric value is supplied. But of course the answers given will
		// be wildly different for these two datatypes. 1234.5678 will be 9 bytes as
		// a char string but will be 8 or 16 bytes as a true numeric.
		// Jay Nance 2006-09-22
		registerFunction(
				"bit_length", new SQLFunctionTemplate( StandardBasicTypes.INTEGER, "octet_length(cast(?1 as char))*4" )
		);

		// The preference here would be
		//   SQLFunctionTemplate( StandardBasicTypes.TIMESTAMP, "current_timestamp(?1)", false)
		// but this appears not to work.
		// Jay Nance 2006-09-22
		registerFunction( "current_timestamp", new SQLFunctionTemplate( StandardBasicTypes.TIMESTAMP, "current_timestamp" ) );
		registerFunction( "current_time", new SQLFunctionTemplate( StandardBasicTypes.TIMESTAMP, "current_time" ) );
		registerFunction( "current_date", new SQLFunctionTemplate( StandardBasicTypes.TIMESTAMP, "current_date" ) );
		// IBID for current_time and current_date

		registerKeyword( "password" );
		registerKeyword( "type" );
		registerKeyword( "title" );
		registerKeyword( "year" );
		registerKeyword( "month" );
		registerKeyword( "summary" );
		registerKeyword( "alias" );
		registerKeyword( "value" );
		registerKeyword( "first" );
		registerKeyword( "role" );
		registerKeyword( "account" );
		registerKeyword( "class" );

		// Tell hibernate to use getBytes instead of getBinaryStream
		getDefaultProperties().setProperty( Environment.USE_STREAMS_FOR_BINARY, "false" );
		// No batch statements
		getDefaultProperties().setProperty( Environment.STATEMENT_BATCH_SIZE, NO_BATCH );
	}

	/**
	 * Teradata does not support <tt>FOR UPDATE</tt> syntax
	 * <p/>
	 * {@inheritDoc}
	 */
	@Override
	public String getForUpdateString() {
		return "";
	}

	@Override
	public boolean supportsIdentityColumns() {
		return false;
	}

	@Override
	public boolean supportsSequences() {
		return false;
	}

	@Override
	public String getAddColumnString() {
		return "Add Column";
	}

	@Override
	public boolean supportsTemporaryTables() {
		return true;
	}

	@Override
	public String getCreateTemporaryTableString() {
		return "create global temporary table";
	}

	@Override
	public String getCreateTemporaryTablePostfix() {
		return " on commit preserve rows";
	}

	@Override
	public Boolean performTemporaryTableDDLInIsolation() {
		return Boolean.TRUE;
	}

	@Override
	public boolean dropTemporaryTableAfterUse() {
		return false;
	}

	@Override
	public String getTypeName(int code, long length, int precision, int scale) throws HibernateException {
		// We might want a special case for 19,2. This is very common for money types
		// and here it is converted to 18,1
		final float f = precision > 0 ? (float) scale / (float) precision : 0;
		final int p = ( precision > 18 ? 18 : precision );
		final int s = ( precision > 18 ? (int) ( 18.0 * f ) : ( scale > 18 ? 18 : scale ) );

		return super.getTypeName( code, length, p, s );
	}

	@Override
	public boolean supportsCascadeDelete() {
		return false;
	}

	@Override
	public boolean supportsCircularCascadeDeleteConstraints() {
		return false;
	}

	@Override
	public boolean areStringComparisonsCaseInsensitive() {
		return true;
	}

	@Override
	public boolean supportsEmptyInList() {
		return false;
	}

	@Override
	public String getSelectClauseNullString(int sqlType) {
		String v = "null";

		switch ( sqlType ) {
			case Types.BIT:
			case Types.TINYINT:
			case Types.SMALLINT:
			case Types.INTEGER:
			case Types.BIGINT:
			case Types.FLOAT:
			case Types.REAL:
			case Types.DOUBLE:
			case Types.NUMERIC:
			case Types.DECIMAL:
				v = "cast(null as decimal)";
				break;
			case Types.CHAR:
			case Types.VARCHAR:
			case Types.LONGVARCHAR:
				v = "cast(null as varchar(255))";
				break;
			case Types.DATE:
			case Types.TIME:
			case Types.TIMESTAMP:
				v = "cast(null as timestamp)";
				break;
			case Types.BINARY:
			case Types.VARBINARY:
			case Types.LONGVARBINARY:
			case Types.NULL:
			case Types.OTHER:
			case Types.JAVA_OBJECT:
			case Types.DISTINCT:
			case Types.STRUCT:
			case Types.ARRAY:
			case Types.BLOB:
			case Types.CLOB:
			case Types.REF:
			case Types.DATALINK:
			case Types.BOOLEAN:
				break;
			default:
				break;
		}
		return v;
	}

	@Override
	public String getCreateMultisetTableString() {
		return "create multiset table ";
	}

	@Override
	public boolean supportsLobValueChangePropogation() {
		return false;
	}

	@Override
	public boolean doesReadCommittedCauseWritersToBlockReaders() {
		return true;
	}

	@Override
	public boolean doesRepeatableReadCauseReadersToBlockWriters() {
		return true;
	}

	@Override
	public boolean supportsBindAsCallableArgument() {
		return false;
	}

	@Override
	public int getInExpressionCountLimit() {
		return PARAM_LIST_SIZE_LIMIT;
	}
}

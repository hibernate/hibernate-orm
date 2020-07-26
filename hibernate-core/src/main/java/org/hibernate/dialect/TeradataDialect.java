/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.dialect.function.VarArgsSQLFunction;
import org.hibernate.hql.spi.id.IdTableSupport;
import org.hibernate.hql.spi.id.IdTableSupportStandardImpl;
import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;
import org.hibernate.hql.spi.id.global.GlobalTemporaryTableBulkIdStrategy;
import org.hibernate.hql.spi.id.local.AfterUseAction;
import org.hibernate.type.StandardBasicTypes;

/**
 * A dialect for the Teradata database created by MCR as part of the
 * dialect certification process.
 *
 * @author Jay Nance
 */
public class TeradataDialect extends Dialect implements IdTableSupport {

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
		registerColumnType( Types.BOOLEAN, "BYTEINT" );  // hibernate seems to ignore this type...
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
		// be wildly different for these two data types. 1234.5678 will be 9 bytes as
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
	 * Does this dialect support the <tt>FOR UPDATE</tt> syntax?
	 *
	 * @return empty string ... Teradata does not support <tt>FOR UPDATE</tt> syntax
	 */
	@Override
	public String getForUpdateString() {
		return "";
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
	public MultiTableBulkIdStrategy getDefaultMultiTableBulkIdStrategy() {
		return new GlobalTemporaryTableBulkIdStrategy( this, AfterUseAction.CLEAN );
	}

	@Override
	public String generateIdTableName(String baseName) {
		return IdTableSupportStandardImpl.INSTANCE.generateIdTableName( baseName );
	}

	@Override
	public String getCreateIdTableCommand() {
		return "create global temporary table";
	}

	@Override
	public String getCreateIdTableStatementOptions() {
		return " on commit preserve rows";
	}

	@Override
	public String getDropIdTableCommand() {
		return "drop table";
	}

	@Override
	public String getTruncateIdTableCommand() {
		return "delete from";
	}

	/**
	 * Get the name of the database type associated with the given
	 * <tt>java.sql.Types</tt> typecode.
	 *
	 * @param code <tt>java.sql.Types</tt> typecode
	 * @param length the length or precision of the column
	 * @param precision the precision of the column
	 * @param scale the scale of the column
	 *
	 * @return the database type name
	 *
	 * @throws HibernateException
	 */
	public String getTypeName(int code, int length, int precision, int scale) throws HibernateException {
		/*
		 * We might want a special case for 19,2. This is very common for money types
		 * and here it is converted to 18,1
		 */
		float f = precision > 0 ? ( float ) scale / ( float ) precision : 0;
		int p = ( precision > 18 ? 18 : precision );
		int s = ( precision > 18 ? ( int ) ( 18.0 * f ) : ( scale > 18 ? 18 : scale ) );

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

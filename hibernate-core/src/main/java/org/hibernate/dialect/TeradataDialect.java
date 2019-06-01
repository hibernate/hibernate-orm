/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.Types;

import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.query.TemporalUnit;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.mutation.spi.SqmMutationStrategy;
import org.hibernate.query.sqm.mutation.spi.idtable.GlobalTempTableExporter;
import org.hibernate.query.sqm.mutation.spi.idtable.GlobalTemporaryTableStrategy;
import org.hibernate.query.sqm.mutation.spi.idtable.IdTable;
import org.hibernate.tool.schema.spi.Exporter;
import org.hibernate.type.spi.StandardSpiBasicTypes;

import static org.hibernate.query.TemporalUnit.NANOSECOND;

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

		registerColumnType( Types.BOOLEAN, "byteint" );
		registerColumnType( Types.BIT, 1, "byteint" );
		registerColumnType( Types.BIT, "byteint" );

		registerColumnType( Types.TINYINT, "byteint" );
		registerColumnType( Types.BIGINT, "numeric(19,0)" );

		registerColumnType( Types.BINARY, "byte($l)" );
		registerColumnType( Types.VARBINARY, "varbyte($l)" );
		registerColumnType( Types.LONGVARBINARY, "varbyte($l)" );

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

	@Override
	public int getDefaultDecimalPrecision() {
		return 18;
	}

	public void timestampdiff(TemporalUnit unit, Renderer from, Renderer to, Appender sqlAppender, boolean fromTimestamp, boolean toTimestamp) {
		//TODO: TOTALLY UNTESTED CODE!
		if (unit == NANOSECOND) {
			sqlAppender.append("1e9*");

		}
		sqlAppender.append("((");
		to.render();
		sqlAppender.append(" - ");
		from.render();
		sqlAppender.append(") ");
		switch (unit) {
			case NANOSECOND:
				sqlAppender.append("second(19,9)");
				break;
			case WEEK:
				sqlAppender.append("day(19,0)");
				break;
			case QUARTER:
				sqlAppender.append("month(19,0)");
				break;
			default:
				sqlAppender.append( unit.toString() );
				sqlAppender.append("(19,0)");
		}
		sqlAppender.append(")");
		switch (unit) {
			case WEEK:
				sqlAppender.append("/7");
				break;
			case QUARTER:
				sqlAppender.append("/3");
				break;
		}
	}

	@Override
	public void timestampadd(TemporalUnit unit, Renderer magnitude, Renderer to, Appender sqlAppender, boolean timestamp) {
		//TODO: TOTALLY UNTESTED CODE!
		sqlAppender.append("(");
		to.render();
		boolean subtract = false;
//		if ( magnitude.startsWith("-") ) {
//			subtract = true;
//			magnitude = magnitude.substring(1);
//		}
		sqlAppender.append(subtract ? " - " : " + ");
		switch ( unit ) {
			case NANOSECOND:
				sqlAppender.append("(");
				magnitude.render();
				sqlAppender.append(")/1e9 * interval '1' second");
				break;
			case QUARTER:
				sqlAppender.append("(");
				magnitude.render();
				sqlAppender.append(") * interval '3' month");
				break;
			case WEEK:
				sqlAppender.append("(");
				magnitude.render();
				sqlAppender.append(") * interval '7' day");
				break;
			default:
//				if ( magnitude.matches("\\d+") ) {
//					sqlAppender.append("interval '");
//					sqlAppender.append( magnitude );
//					sqlAppender.append("'");
//				}
//				else {
					sqlAppender.append("(");
					magnitude.render();
					sqlAppender.append(") * interval '1'");
//				}
				sqlAppender.append(" ");
				sqlAppender.append( unit.toString() );
		}
		sqlAppender.append(")");
	}

	@Override
	public void initializeFunctionRegistry(QueryEngine queryEngine) {
		super.initializeFunctionRegistry( queryEngine );

		CommonFunctionFactory.concat_pipeOperator( queryEngine );
		CommonFunctionFactory.octetLength( queryEngine );
		CommonFunctionFactory.moreHyperbolic( queryEngine );
		CommonFunctionFactory.instr( queryEngine );
		CommonFunctionFactory.substr( queryEngine );
		CommonFunctionFactory.substring_substr( queryEngine );
		//also natively supports ANSI-style substring()
		CommonFunctionFactory.position( queryEngine );

		queryEngine.getSqmFunctionRegistry().patternTemplateBuilder( "mod", "(?1 mod ?2)" )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.setExactArgumentCount( 2 )
				.register();

	}

	/**
	 * Does this dialect support the <tt>FOR UPDATE</tt> syntax?
	 *
	 * @return empty string ... Teradata does not support <tt>FOR UPDATE<tt> syntax
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
	public SqmMutationStrategy getDefaultIdTableStrategy() {
		return new GlobalTemporaryTableStrategy( generateIdTableExporter() );
	}

	private Exporter<IdTable> generateIdTableExporter() {
		return new GlobalTempTableExporter() {
			@Override
			public String getCreateCommand() {
				return "create global temporary table";
			}

			@Override
			public String getCreateOptions() {
				return " on commit preserve rows";
			}

			@Override
			protected String getTruncateIdTableCommand() {
				return "delete from";
			}
		};
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
			case Types.TIMESTAMP_WITH_TIMEZONE:
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

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.Types;
import java.util.Locale;

import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.pagination.AbstractLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.LimitHelper;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.tool.schema.extract.internal.SequenceNameExtractorImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;

/**
 * An SQL dialect for Interbase.
 *
 * @author Gavin King
 */
public class InterbaseDialect extends Dialect {

	private static final AbstractLimitHandler LIMIT_HANDLER = new AbstractLimitHandler() {
		@Override
		public String processSql(String sql, RowSelection selection) {
			final boolean hasOffset = LimitHelper.hasFirstRow( selection );
			return hasOffset ? sql + " rows ? to ?" : sql + " rows ?";
		}

		@Override
		public boolean supportsLimit() {
			return true;
		}
	};

	/**
	 * Constructs a InterbaseDialect
	 */
	public InterbaseDialect() {
		super();
		registerColumnType( Types.BIT, 1, "smallint" );
		registerColumnType( Types.BIT, "smallint" );
		registerColumnType( Types.BOOLEAN, "smallint" );

		registerColumnType( Types.TINYINT, "smallint" );
		registerColumnType( Types.BIGINT, "numeric(19,0)" );

		registerColumnType( Types.VARBINARY, "blob" );
		registerColumnType( Types.BLOB, "blob" );
		registerColumnType( Types.CLOB, "blob sub_type 1" );

		//no precision
		registerColumnType( Types.TIMESTAMP, "timestamp" );
		registerColumnType( Types.TIMESTAMP_WITH_TIMEZONE, "timestamp" );

		getDefaultProperties().setProperty( Environment.STATEMENT_BATCH_SIZE, NO_BATCH );
	}

	@Override
	public void initializeFunctionRegistry(QueryEngine queryEngine) {
		super.initializeFunctionRegistry( queryEngine );
		CommonFunctionFactory.concat_pipeOperator( queryEngine );
	}

	@Override
	public String getAddColumnString() {
		return "add";
	}

	@Override
	public String getSequenceNextValString(String sequenceName) {
		return "select " + getSelectSequenceNextValString( sequenceName ) + " from RDB$DATABASE";
	}

	@Override
	public String getSelectSequenceNextValString(String sequenceName) {
		return "gen_id( " + sequenceName + ", 1 )";
	}

	@Override
	public String getCreateSequenceString(String sequenceName) {
		return "create generator " + sequenceName;
	}

	@Override
	public String getDropSequenceString(String sequenceName) {
		return "delete from RDB$GENERATORS where RDB$GENERATOR_NAME = '" + sequenceName.toUpperCase(Locale.ROOT) + "'";
	}

	@Override
	public String getQuerySequencesString() {
		return "select RDB$GENERATOR_NAME from RDB$GENERATORS";
	}

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return SequenceNameExtractorImpl.INSTANCE;
	}

	@Override
	public String getForUpdateString() {
		return " with lock";
	}

	@Override
	public String getForUpdateString(String aliases) {
		return " for update of " + aliases + " with lock";
	}

	@Override
	public boolean supportsSequences() {
		return true;
	}

	@Override
	public LimitHandler getLimitHandler() {
		return LIMIT_HANDLER;
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean supportsLimit() {
		return true;
	}

	@Override
	@SuppressWarnings("deprecation")
	public String getLimitString(String sql, boolean hasOffset) {
		return hasOffset ? sql + " rows ? to ?" : sql + " rows ?";
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean bindLimitParametersFirst() {
		return false;
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean bindLimitParametersInReverseOrder() {
		return false;
	}

	@Override
	public String getCurrentTimestampSelectString() {
		// TODO : not sure which (either?) is correct, could not find docs on how to do this.
		// did find various blogs and forums mentioning that select CURRENT_TIMESTAMP
		// does not work...
		return "{?= call CURRENT_TIMESTAMP }";
//		return "select CURRENT_TIMESTAMP from RDB$DATABASE";
	}

	@Override
	public boolean isCurrentTimestampSelectStringCallable() {
		return true;
	}

	@Override
	public String getFromDual() {
		return "from RDB$DATABASE";
	}

}

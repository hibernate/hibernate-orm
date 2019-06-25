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
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.RowsLimitHandler;
import org.hibernate.tool.schema.extract.internal.SequenceNameExtractorImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;

/**
 * An SQL dialect for Interbase.
 *
 * @author Gavin King
 */
public class InterbaseDialect extends Dialect {

	public InterbaseDialect() {
		super();

		registerColumnType( Types.BIT, 1, "boolean" );
		registerColumnType( Types.BIT, "smallint" );

		registerColumnType( Types.TINYINT, "smallint" );
		registerColumnType( Types.BIGINT, "numeric(19,0)" );

		registerColumnType( Types.REAL, "float");
		registerColumnType( Types.FLOAT, "double precision");

		registerColumnType( Types.VARBINARY, "blob" );
		registerColumnType( Types.BLOB, "blob" );
		registerColumnType( Types.CLOB, "blob sub_type 1" );

		//no precision
		registerColumnType( Types.TIMESTAMP, "timestamp" );
		registerColumnType( Types.TIMESTAMP_WITH_TIMEZONE, "timestamp" );

		getDefaultProperties().setProperty( Environment.STATEMENT_BATCH_SIZE, NO_BATCH );
		getDefaultProperties().setProperty( Environment.QUERY_LITERAL_RENDERING, "literal" );
	}

	@Override
	public int getPreferredSqlTypeCodeForBoolean() {
		return Types.BOOLEAN;
	}

	@Override
	public int getDefaultDecimalPrecision() {
		//the extremely low maximum
		return 18;
	}

	@Override
	public String getAddColumnString() {
		return "add";
	}

	@Override
	public boolean supportsSequences() {
		return true;
	}

	@Override
	public String getSequenceNextValString(String sequenceName) {
		return "select " + getSelectSequenceNextValString( sequenceName ) + " " + getFromDual();
	}

	@Override
	public String getSelectSequenceNextValString(String sequenceName) {
		return "gen_id(" + sequenceName + ",1)";
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
	public LimitHandler getLimitHandler() {
		return RowsLimitHandler.INSTANCE;
	}

	@Override
	public String getCurrentTimestampSelectString() {
		return "select current_timestamp " + getFromDual();
	}

	@Override
	public boolean isCurrentTimestampSelectStringCallable() {
		return false;
	}

	@Override
	public String getFromDual() {
		return "from RDB$DATABASE";
	}

}

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
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.identity.Ingres10IdentityColumnSupport;
import org.hibernate.dialect.identity.Ingres9IdentityColumnSupport;
import org.hibernate.dialect.pagination.AbstractLimitHandler;
import org.hibernate.dialect.pagination.LimitHelper;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.query.TemporalUnit;
import org.hibernate.dialect.pagination.FirstLimitHandler;
import org.hibernate.dialect.pagination.LegacyFirstLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.naming.Identifier;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.mutation.spi.idtable.StandardIdTableSupport;
import org.hibernate.query.sqm.mutation.spi.SqmMutationStrategy;
import org.hibernate.query.sqm.mutation.spi.idtable.GlobalTempTableExporter;
import org.hibernate.query.sqm.mutation.spi.idtable.GlobalTemporaryTableStrategy;
import org.hibernate.query.sqm.mutation.spi.idtable.IdTable;
import org.hibernate.query.sqm.mutation.spi.idtable.IdTableSupport;
import org.hibernate.tool.schema.extract.internal.SequenceNameExtractorImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.tool.schema.spi.Exporter;
import org.hibernate.type.spi.StandardSpiBasicTypes;

/**
 * An SQL dialect for Ingres 9.2.
 * <p/>
 * Known limitations: <ul>
 *     <li>
 *         Only supports simple constants or columns on the left side of an IN,
 *         making {@code (1,2,3) in (...)} or {@code (subselect) in (...)} non-supported.
 *     </li>
 *     <li>
 *         Supports only 39 digits in decimal.
 *     </li>
 *     <li>
 *         Explicitly set USE_GET_GENERATED_KEYS property to false.
 *     </li>
 *     <li>
 *         Perform string casts to varchar; removes space padding.
 *     </li>
 * </ul>
 * 
 * @author Ian Booth
 * @author Bruce Lunsford
 * @author Max Rydahl Andersen
 * @author Raymond Fan
 */
public class IngresDialect extends Dialect {

	private static final LimitHandler LIMIT_HANDLER = new AbstractLimitHandler() {
		@Override
		public String processSql(String sql, RowSelection selection) {
			final String soff = " offset " + selection.getFirstRow();
			final String slim = " fetch first " + getMaxOrLimit( selection ) + " rows only";
			final StringBuilder sb = new StringBuilder( sql.length() + soff.length() + slim.length() )
					.append( sql );
			if (LimitHelper.hasFirstRow( selection )) {
				sb.append( soff );
			}
			if (LimitHelper.hasMaxRows( selection )) {
				sb.append( slim );
			}
			return sb.toString();
		}

		@Override
		public boolean supportsLimit() {
			return true;
		}

		@Override
		public boolean supportsVariableLimit() {
			return false;
		}
	};

	private final int version;

	int getVersion() {
		return version;
	}

	public IngresDialect(DialectResolutionInfo info) {
		this( info.getDatabaseMajorVersion() * 100 + info.getDatabaseMinorVersion() * 10 );
	}

	public IngresDialect() {
		this(920);
	}

	/**
	 * Constructs a IngresDialect
	 */
	public IngresDialect(int version) {
		super();
		this.version = version;

		if ( getVersion() < 1000 ) {
			registerColumnType( Types.BIT, 1, "tinyint" );
			registerColumnType( Types.BOOLEAN, "tinyint" );
		}
		else {
			registerColumnType( Types.BIT, 1, "boolean" );
			registerColumnType( Types.BOOLEAN, "boolean" );
		}
		registerColumnType( Types.BIT, "tinyint" );

		registerColumnType( Types.NUMERIC, "decimal($p, $s)" ); //Ingres has no 'numeric' type

		registerColumnType( Types.BINARY, 32000, "byte($l)" );
		registerColumnType( Types.VARBINARY, 32000, "varbyte($l)" );
		//note: 'long byte' is really a blob type
		registerColumnType( Types.VARBINARY, "long byte" );
		registerColumnType( Types.LONGVARBINARY, "long byte" );

		//TODO: should we be using nchar/nvarchar/long nvarchar
		//      here? I think Ingres char/varchar types don't
		//      support Unicode. Copy what AbstractHANADialect
		//      does with a Hibernate property to config this.
		registerColumnType( Types.CHAR, 32000, "char($l)" );
		registerColumnType( Types.VARCHAR, 32000, "varchar($l)" );
		//note: 'long varchar' is really a clob type
		registerColumnType( Types.VARCHAR, "long varchar" );
		registerColumnType( Types.LONGVARCHAR, "long varchar" );

		registerColumnType( Types.NCHAR, 32000, "nchar($l)" );
		registerColumnType( Types.NVARCHAR, 32000, "nvarchar($l)" );
		//note: 'long nvarchar' is really a clob type
		registerColumnType( Types.NVARCHAR, "long nvarchar" );
		registerColumnType( Types.LONGNVARCHAR, "long nvarchar" );

		if ( getVersion() >= 930 ) {
			// Not completely necessary, given that Ingres
			// can be configured to set DATE = ANSIDATE
			registerColumnType( Types.DATE, "ansidate" );
		}

		// Ingres driver supports getGeneratedKeys but only in the following
		// form:
		// The Ingres DBMS returns only a single table key or a single object
		// key per insert statement. Ingres does not return table and object
		// keys for INSERT AS SELECT statements. Depending on the keys that are
		// produced by the statement executed, auto-generated key parameters in
		// execute(), executeUpdate(), and prepareStatement() methods are
		// ignored and getGeneratedKeys() returns a result-set containing no
		// rows, a single row with one column, or a single row with two columns.
		// Ingres JDBC Driver returns table and object keys as BINARY values.
		getDefaultProperties().setProperty( Environment.USE_GET_GENERATED_KEYS, "false" );

		if ( getVersion() < 1000 ) {
			// There is no support for a native boolean type that accepts values
			// of true, false or unknown. Using the tinyint type requires
			// substitutions of true and false.
			getDefaultProperties().setProperty( Environment.QUERY_SUBSTITUTIONS, "true=1,false=0" );
		}
	}

	@Override
	public int getPreferredSqlTypeCodeForBoolean() {
		return getVersion() < 1000 ? Types.BIT : Types.BOOLEAN;
	}

	@Override
	public String toBooleanValueString(boolean bool) {
		return getVersion() < 1000
				? super.toBooleanValueString( bool )
				: String.valueOf( bool );
	}


	@Override
	public int getDefaultDecimalPrecision() {
		//the maximum
		return 39;
	}

	@Override
	public void initializeFunctionRegistry(QueryEngine queryEngine) {
		super.initializeFunctionRegistry( queryEngine );

		// ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
		// Common functions

		CommonFunctionFactory.log( queryEngine );
		CommonFunctionFactory.rand( queryEngine );
		CommonFunctionFactory.soundex( queryEngine );
		CommonFunctionFactory.octetLength( queryEngine );
		CommonFunctionFactory.repeat( queryEngine );
		CommonFunctionFactory.pad( queryEngine );
		CommonFunctionFactory.trim2( queryEngine );
		CommonFunctionFactory.trunc( queryEngine );
		CommonFunctionFactory.truncate( queryEngine );
		CommonFunctionFactory.initcap( queryEngine );
		CommonFunctionFactory.yearMonthDay( queryEngine );
		CommonFunctionFactory.hourMinuteSecond( queryEngine );
		CommonFunctionFactory.dayofweekmonthyear( queryEngine );
		CommonFunctionFactory.weekQuarter( queryEngine );
		CommonFunctionFactory.lastDay( queryEngine );
		CommonFunctionFactory.concat_pipeOperator( queryEngine );
		CommonFunctionFactory.substr( queryEngine );
		CommonFunctionFactory.monthsBetween( queryEngine );
		CommonFunctionFactory.substring_substr( queryEngine );
		//also natively supports ANSI-style substring()
		CommonFunctionFactory.leftRight( queryEngine );
		CommonFunctionFactory.ascii( queryEngine );
		CommonFunctionFactory.char_chr( queryEngine );
		CommonFunctionFactory.sysdate( queryEngine );
		CommonFunctionFactory.position( queryEngine );
		CommonFunctionFactory.formatdatetime_dateFormat( queryEngine );
		CommonFunctionFactory.dateTrunc( queryEngine );

		queryEngine.getSqmFunctionRegistry().registerBinaryTernaryPattern("locate", StandardSpiBasicTypes.INTEGER, "position(?1 in ?2)", "(position(?1 in substring(?2 from ?3)) + (?3) - 1)");

		queryEngine.getSqmFunctionRegistry().registerPattern( "extract", "date_part('?1', ?2)", StandardSpiBasicTypes.INTEGER );

		CommonFunctionFactory.bitandorxornot_bitAndOrXorNot(queryEngine);

		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "squeeze" )
				.setExactArgumentCount( 1 )
				.setInvariantType( StandardSpiBasicTypes.STRING )
				.register();

	}

	@Override
	public void timestampadd(TemporalUnit unit, Renderer magnitude, Renderer to, Appender sqlAppender, boolean timestamp) {
		sqlAppender.append("timestampadd(");
		sqlAppender.append( unit.toString() );
		sqlAppender.append(", ");
		magnitude.render();
		sqlAppender.append(", ");
		to.render();
		sqlAppender.append(")");
	}

	@Override
	public void timestampdiff(TemporalUnit unit, Renderer from, Renderer to, Appender sqlAppender, boolean fromTimestamp, boolean toTimestamp) {
		sqlAppender.append("timestampdiff(");
		sqlAppender.append( unit.toString() );
		sqlAppender.append(", ");
		from.render();
		sqlAppender.append(", ");
		to.render();
		sqlAppender.append(")");
	}

	@Override
	public String getSelectGUIDString() {
		return "select uuid_to_char(uuid_create())";
	}

	@Override
	public boolean dropConstraints() {
		return false;
	}

	@Override
	public String getAddColumnString() {
		return "add column";
	}

	@Override
	public String getNullColumnString() {
		return " with null";
	}

	// SEQUENCE support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public boolean supportsSequences() {
		return true;
	}

	@Override
	public String getQuerySequencesString() {
		return getVersion() < 930
				? "select seq_name from iisequence"
				: "select seq_name from iisequences";
	}

	@Override
	public boolean supportsPooledSequences() {
		return getVersion() >= 930;
	}

	@Override
	public String getSequenceNextValString(String sequenceName) {
		return "select nextval for " + sequenceName;
	}

	@Override
	public String getSelectSequenceNextValString(String sequenceName) {
		return sequenceName + ".nextval";
	}

	@Override
	public String getCreateSequenceString(String sequenceName) {
		return "create sequence " + sequenceName;
	}

	@Override
	public String getDropSequenceString(String sequenceName) {
		return "drop sequence " + sequenceName + " restrict";
	}

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return SequenceNameExtractorImpl.INSTANCE;
	}

	@Override
	public String getLowercaseFunction() {
		return "lowercase";
	}

	@Override
	public LimitHandler getLimitHandler() {
		if ( isLegacyLimitHandlerBehaviorEnabled() ) {
			return LegacyFirstLimitHandler.INSTANCE;
		}
		return getDefaultLimitHandler();
	}

	private LimitHandler getDefaultLimitHandler() {
		return getVersion() < 930 ? FirstLimitHandler.INSTANCE : LIMIT_HANDLER;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		if ( getVersion() >= 1000 ) {
			return new Ingres10IdentityColumnSupport();
		}
		else if (getVersion() >= 930) {
			return new Ingres9IdentityColumnSupport();
		}
		else {
			return super.getIdentityColumnSupport();
		}
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean supportsLimit() {
		return true;
	}

	@Override
	@SuppressWarnings("deprecation")
	public String getLimitString(String querySelect, int offset, int limit) {
		if ( getVersion() >= 930 ) {
			final StringBuilder soff = new StringBuilder( " offset " + offset );
			final StringBuilder slim = new StringBuilder( " fetch first " + limit + " rows only" );
			final StringBuilder sb = new StringBuilder( querySelect.length() + soff.length() + slim.length() )
					.append( querySelect );
			if ( offset > 0 ) {
				sb.append( soff );
			}
			if ( limit > 0 ) {
				sb.append( slim );
			}
			return sb.toString();
		}
		else {
			if ( offset > 0 ) {
				throw new UnsupportedOperationException( "query result offset is not supported" );
			}
			return new StringBuilder( querySelect.length() + 16 )
					.append( querySelect )
					.insert( 6, " first " + limit )
					.toString();
		}
	}
	// lock acquisition support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public boolean supportsOuterJoinForUpdate() {
		return getVersion() >= 930;
	}

	@Override
	public boolean forUpdateOfColumns() {
		return getVersion() >= 930;
	}

	// current timestamp support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public boolean isCurrentTimestampSelectStringCallable() {
		return false;
	}

	@Override
	public boolean supportsCurrentTimestampSelection() {
		return getVersion() >= 930;
	}

	@Override
	public String getCurrentTimestampSelectString() {
		return getVersion() >= 930
				? "select current_timestamp"
				: super.getCurrentTimestampSelectString();
	}

	@Override
	public String getCurrentTimestampSQLFunctionName() {
		return getVersion() >= 930 ? "current_timestamp" : "date(now)";
	}

	// union subclass support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public boolean supportsUnionAll() {
		return getVersion() >= 930;
	}

	// Informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public boolean doesReadCommittedCauseWritersToBlockReaders() {
		return getVersion() >= 930;
	}

	@Override
	public boolean doesRepeatableReadCauseReadersToBlockWriters() {
		return getVersion() >= 930;
	}

	// limit/offset support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	@SuppressWarnings("deprecation")
	public boolean supportsLimitOffset() {
		return getVersion() >= 930;
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean supportsVariableLimit() {
		return false;
	}

	@Override
	@SuppressWarnings("deprecation")
	public boolean useMaxForLimit() {
		return getVersion() < 930;
	}

	@Override
	public SqmMutationStrategy getDefaultIdTableStrategy() {
		return new GlobalTemporaryTableStrategy( generateIdTableSupport() );
	}

	private IdTableSupport generateIdTableSupport() {
		return new StandardIdTableSupport( generateIdTableExporter() ) {
			@Override
			protected Identifier determineIdTableName(Identifier baseName) {
				return new Identifier( "session." + super.determineIdTableName( baseName ).getText(), false );
			}
		};
	}

	private Exporter<IdTable> generateIdTableExporter() {
		return new GlobalTempTableExporter() {
			@Override
			public String getCreateCommand() {
				return "declare global temporary table";
			}

			@Override
			public String getCreateOptions() {
				return "on commit preserve rows with norecovery";
			}
		};
	}

	// Overridden informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public boolean supportsSubselectAsInPredicateLHS() {
		return false;
	}

	@Override
	public boolean supportsEmptyInList() {
		return false;
	}

	@Override
	public boolean supportsExpectedLobUsagePattern() {
		return false;
	}

	@Override
	public boolean supportsTupleDistinctCounts() {
		return false;
	}

	@Override
	public String translateDatetimeFormat(String format) {
		return MySQLDialect.datetimeFormat( format ).result();
	}

	@Override
	public String translateExtractField(TemporalUnit unit) {
		switch ( unit ) {
			case DAY_OF_MONTH: return "day";
			case DAY_OF_YEAR: return "doy";
			case DAY_OF_WEEK: return "dow";
			case WEEK: return "iso_week";
			default: return unit.toString();
		}
	}

}

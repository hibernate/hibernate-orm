/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.JDBCException;
import org.hibernate.PessimisticLockException;
import org.hibernate.boot.TempTableDdlTransactionHandling;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.hint.IndexQueryHintHandler;
import org.hibernate.dialect.identity.H2IdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.LimitOffsetLimitHandler;
import org.hibernate.dialect.pagination.OffsetFetchLimitHandler;
import org.hibernate.dialect.sequence.H2SequenceSupport;
import org.hibernate.dialect.sequence.SequenceSupport;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor;
import org.hibernate.exception.spi.ViolatedConstraintNameExtractor;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.query.TemporalUnit;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.mutation.internal.idtable.AfterUseAction;
import org.hibernate.query.sqm.mutation.internal.idtable.IdTable;
import org.hibernate.query.sqm.mutation.internal.idtable.LocalTemporaryTableStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorH2DatabaseImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.jboss.logging.Logger;

import java.sql.SQLException;

import static org.hibernate.query.TemporalUnit.SECOND;

/**
 * A dialect compatible with the H2 database.
 *
 * @author Thomas Mueller
 */
public class H2Dialect extends Dialect {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			H2Dialect.class.getName()
	);

	private final LimitHandler limitHandler;

	private final boolean dropConstraints;

//	private final String querySequenceString;
//	private final SequenceInformationExtractor sequenceInformationExtractor;

	public H2Dialect() {
		this(0, 0);
	}

	public H2Dialect(int version, int buildId) {
		super();

		limitHandler = !( version > 140 || buildId >= 199 )
				? LimitOffsetLimitHandler.INSTANCE
				: OffsetFetchLimitHandler.INSTANCE;

//		if ( buildId >= 32 ) {
//			this.sequenceInformationExtractor = SequenceInformationExtractorH2DatabaseImpl.INSTANCE;
//			this.querySequenceString = "select * from information_schema.sequences";
//		}
//		else {
//			this.sequenceInformationExtractor = SequenceInformationExtractorNoOpImpl.INSTANCE;
//			this.querySequenceString = null;
//		}

		//Note: H2 'bit' is a synonym for 'boolean', not a proper bit type
//		registerColumnType( Types.BIT, "bit" );

		if ( !( version > 120 || buildId >= 139 ) ) {
			LOG.unsupportedMultiTableBulkHqlJpaql( version / 100, version % 100 / 10, buildId );
		}

		// Prior to 1.4.200 we didn't need to drop constraints before
		// dropping tables, that just lead to error messages about
		// missing tables when we don't have a schema in the database
		dropConstraints = version > 140 && buildId >= 200;

		getDefaultProperties().setProperty( AvailableSettings.STATEMENT_BATCH_SIZE, DEFAULT_BATCH_SIZE );
		// http://code.google.com/p/h2database/issues/detail?id=235
		getDefaultProperties().setProperty( AvailableSettings.NON_CONTEXTUAL_LOB_CREATION, "true" );
	}

	private static int parseBuildId(DialectResolutionInfo info) {
		String[] bits = info.getDatabaseVersion().split("[. ]");
		return bits.length > 2 ? Integer.parseInt( bits[2] ) : 0;
	}

	public H2Dialect(DialectResolutionInfo info) {
		this(
				info.getDatabaseMajorVersion()*100
						+ info.getDatabaseMinorVersion()*10,
				parseBuildId( info )
		);
	}

	@Override
	public void initializeFunctionRegistry(QueryEngine queryEngine) {
		super.initializeFunctionRegistry( queryEngine );

		CommonFunctionFactory.pi( queryEngine );
		CommonFunctionFactory.cot( queryEngine );
		CommonFunctionFactory.radians( queryEngine );
		CommonFunctionFactory.degrees( queryEngine );
		CommonFunctionFactory.log10( queryEngine );
		CommonFunctionFactory.rand( queryEngine );
		CommonFunctionFactory.truncate( queryEngine );
		CommonFunctionFactory.soundex( queryEngine );
		CommonFunctionFactory.translate( queryEngine );
		CommonFunctionFactory.bitand( queryEngine );
		CommonFunctionFactory.bitor( queryEngine );
		CommonFunctionFactory.bitxor( queryEngine );
		CommonFunctionFactory.bitAndOr( queryEngine );
		CommonFunctionFactory.yearMonthDay( queryEngine );
		CommonFunctionFactory.hourMinuteSecond( queryEngine );
		CommonFunctionFactory.dayOfWeekMonthYear( queryEngine );
		CommonFunctionFactory.weekQuarter( queryEngine );
		CommonFunctionFactory.daynameMonthname( queryEngine );
		CommonFunctionFactory.localtimeLocaltimestamp( queryEngine );
		CommonFunctionFactory.bitLength( queryEngine );
		CommonFunctionFactory.octetLength( queryEngine );
		CommonFunctionFactory.ascii( queryEngine );
		CommonFunctionFactory.octetLength( queryEngine );
		CommonFunctionFactory.space( queryEngine );
		CommonFunctionFactory.repeat( queryEngine );
		CommonFunctionFactory.chr_char( queryEngine );
		CommonFunctionFactory.instr( queryEngine );
		CommonFunctionFactory.substr( queryEngine );
		//also natively supports ANSI-style substring()
		CommonFunctionFactory.position( queryEngine );
		CommonFunctionFactory.trim1( queryEngine );
		CommonFunctionFactory.concat_pipeOperator( queryEngine );
		CommonFunctionFactory.nowCurdateCurtime( queryEngine );
		CommonFunctionFactory.sysdate( queryEngine );
		CommonFunctionFactory.insert( queryEngine );
//		CommonFunctionFactory.everyAny( queryEngine ); //this would work too
		CommonFunctionFactory.everyAny_boolAndOr( queryEngine );
		CommonFunctionFactory.median( queryEngine );
		CommonFunctionFactory.stddevPopSamp( queryEngine );
		CommonFunctionFactory.varPopSamp( queryEngine );
		CommonFunctionFactory.format_formatdatetime( queryEngine );
		CommonFunctionFactory.rownum( queryEngine );
	}

	/**
	 * In H2, the extract() function does not return
	 * fractional seconds for the the field
	 * {@link TemporalUnit#SECOND}. We work around
	 * this here with two calls to extract().
	 */
	@Override
	public String extractPattern(TemporalUnit unit) {
		return unit == SECOND
				? "(" + super.extractPattern(unit) + "+extract(nanosecond from ?2)/1e9)"
				: super.extractPattern(unit);
	}

	@Override
	public String timestampaddPattern(TemporalUnit unit, boolean timestamp) {
		return "dateadd(?1, ?2, ?3)";

	}

	@Override
	public String timestampdiffPattern(TemporalUnit unit, boolean fromTimestamp, boolean toTimestamp) {
		return "datediff(?1, ?2, ?3)";
	}

	@Override
	public LimitHandler getLimitHandler() {
		return limitHandler;
	}

	@Override
	public boolean supportsIfExistsAfterTableName() {
		return true;
	}

	@Override
	public boolean supportsIfExistsAfterAlterTable() {
		return dropConstraints;
	}

	@Override
	public boolean supportsIfExistsBeforeConstraintName() {
		return true;
	}

	@Override
	public SequenceSupport getSequenceSupport() {
		return H2SequenceSupport.INSTANCE;
	}

	@Override
	public String getQuerySequencesString() {
		return "select * from information_schema.sequences";
	}

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return SequenceInformationExtractorH2DatabaseImpl.INSTANCE;
	}

	@Override
	public String getFromDual() {
		return "from dual";
	}

	@Override
	public SqmMultiTableMutationStrategy getFallbackSqmMutationStrategy(
			EntityMappingType entityDescriptor,
			RuntimeModelCreationContext runtimeModelCreationContext) {
		return new LocalTemporaryTableStrategy(
				new IdTable( entityDescriptor, basename -> "HT_" + basename ),
				this::getTypeName,
				AfterUseAction.CLEAN,
				TempTableDdlTransactionHandling.NONE,
				runtimeModelCreationContext.getSessionFactory()
		);
	}

	@Override
	public ViolatedConstraintNameExtractor getViolatedConstraintNameExtracter() {
		return EXTRACTOR;
	}

	private static final ViolatedConstraintNameExtractor EXTRACTOR =
			new TemplatedViolatedConstraintNameExtractor( sqle -> {
				// 23000: Check constraint violation: {0}
				// 23001: Unique index or primary key violation: {0}
				if ( sqle.getSQLState().startsWith( "23" ) ) {
					final String message = sqle.getMessage();
					final int idx = message.indexOf( "violation: " );
					if ( idx > 0 ) {
						return message.substring( idx + "violation: ".length() );
					}
				}
				return null;
			} );

	@Override
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		SQLExceptionConversionDelegate delegate = super.buildSQLExceptionConversionDelegate();
		if (delegate == null) {
			delegate = new SQLExceptionConversionDelegate() {
				@Override
				public JDBCException convert(SQLException sqlException, String message, String sql) {
					final int errorCode = JdbcExceptionHelper.extractErrorCode( sqlException );

					switch (errorCode) {
						case 40001:
							// DEADLOCK DETECTED
							return new LockAcquisitionException(message, sqlException, sql);
						case 50200:
							// LOCK NOT AVAILABLE
							return new PessimisticLockException(message, sqlException, sql);
						case 90006:
							// NULL not allowed for column [90006-145]
							final String constraintName = getViolatedConstraintNameExtracter().extractConstraintName(sqlException);
							return new ConstraintViolationException(message, sqlException, sql, constraintName);
					}

					return null;
				}
			};
		}
		return delegate;
	}

	@Override
	public boolean supportsCurrentTimestampSelection() {
		return true;
	}

	@Override
	public boolean isCurrentTimestampSelectStringCallable() {
		return false;
	}

	@Override
	public String getCurrentTimestampSelectString() {
		return "call current_timestamp()";
	}

	@Override
	public boolean supportsUnionAll() {
		return true;
	}


	// Overridden informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public boolean supportsLobValueChangePropogation() {
		return false;
	}

	@Override
	public boolean requiresParensForTupleDistinctCounts() {
		return true;
	}

	@Override
	public boolean doesReadCommittedCauseWritersToBlockReaders() {
		// see http://groups.google.com/group/h2-database/browse_thread/thread/562d8a49e2dabe99?hl=en
		return true;
	}
	
	@Override
	public boolean supportsTuplesInSubqueries() {
		return false;
	}
	
	@Override
	public boolean dropConstraints() {
		return dropConstraints;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return new H2IdentityColumnSupport();
	}

	@Override
	public String getQueryHintString(String query, String hints) {
		return IndexQueryHintHandler.INSTANCE.addQueryHints( query, hints );
	}

	@Override
	public String translateDatetimeFormat(String format) {
		return new Replacer( format, "'", "''" ).replace("e", "u").result(); //NICE!!
	}

	public String translateExtractField(TemporalUnit unit) {
		switch ( unit ) {
			case DAY_OF_MONTH: return "day";
			case WEEK: return "iso_week";
			default: return unit.toString();
		}
	}

}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.community.dialect;

import java.lang.invoke.MethodHandles;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.JDBCException;
import org.hibernate.LockMode;
import org.hibernate.StaleObjectStateException;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.dialect.*;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.function.TrimFunction;
import org.hibernate.dialect.identity.HSQLIdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.dialect.lock.OptimisticForceIncrementLockingStrategy;
import org.hibernate.dialect.lock.OptimisticLockingStrategy;
import org.hibernate.dialect.lock.PessimisticForceIncrementLockingStrategy;
import org.hibernate.dialect.lock.PessimisticReadSelectLockingStrategy;
import org.hibernate.dialect.lock.PessimisticWriteSelectLockingStrategy;
import org.hibernate.dialect.lock.SelectLockingStrategy;
import org.hibernate.dialect.pagination.LegacyHSQLLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.LimitOffsetLimitHandler;
import org.hibernate.dialect.pagination.OffsetFetchLimitHandler;
import org.hibernate.dialect.sequence.HSQLSequenceSupport;
import org.hibernate.dialect.sequence.SequenceSupport;
import org.hibernate.dialect.temptable.TemporaryTable;
import org.hibernate.dialect.temptable.TemporaryTableKind;
import org.hibernate.dialect.unique.CreateTableUniqueDelegate;
import org.hibernate.dialect.unique.UniqueDelegate;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelperBuilder;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor;
import org.hibernate.exception.spi.ViolatedConstraintNameExtractor;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.sqm.CastType;
import org.hibernate.query.sqm.IntervalType;
import org.hibernate.dialect.NullOrdering;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.query.sqm.mutation.spi.AfterUseAction;
import org.hibernate.query.sqm.mutation.spi.BeforeUseAction;
import org.hibernate.query.sqm.mutation.internal.temptable.GlobalTemporaryTableInsertStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.GlobalTemporaryTableMutationStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.LocalTemporaryTableInsertStrategy;
import org.hibernate.query.sqm.mutation.internal.temptable.LocalTemporaryTableMutationStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorHSQLDBDatabaseImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.type.spi.TypeConfiguration;

import org.jboss.logging.Logger;

import jakarta.persistence.TemporalType;

import static org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor.extractUsingTemplate;
import static org.hibernate.type.SqlTypes.BLOB;
import static org.hibernate.type.SqlTypes.CLOB;
import static org.hibernate.type.SqlTypes.DOUBLE;
import static org.hibernate.type.SqlTypes.NCLOB;
import static org.hibernate.type.SqlTypes.NUMERIC;

/**
 * A {@linkplain Dialect SQL dialect} for HSQLDB (HyperSQL) 1.8 up to (but not including) 2.6.1.
 *
 * @author Christoph Sturm
 * @author Phillip Baird
 * @author Fred Toussi
 */
public class HSQLLegacyDialect extends Dialect {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			MethodHandles.lookup(),
			CoreMessageLogger.class,
			org.hibernate.community.dialect.HSQLLegacyDialect.class.getName()
	);
	private final UniqueDelegate uniqueDelegate = new CreateTableUniqueDelegate( this );
	private final HSQLIdentityColumnSupport identityColumnSupport;

	public HSQLLegacyDialect(DialectResolutionInfo info) {
		super( info );
		this.identityColumnSupport = new HSQLIdentityColumnSupport( getVersion() );
	}

	public HSQLLegacyDialect() {
		this( DatabaseVersion.make( 1, 8 ) );
	}

	public HSQLLegacyDialect(DatabaseVersion version) {
		super( version.isSame( 1, 8 ) ? reflectedVersion( version ) : version );
		this.identityColumnSupport = new HSQLIdentityColumnSupport( getVersion() );
	}

	@Override
	protected void registerDefaultKeywords() {
		super.registerDefaultKeywords();
		if ( getVersion().isSameOrAfter( 2, 5 ) ) {
			registerKeyword( "period" );
		}
	}

	@Override
	protected String columnType(int sqlTypeCode) {
		// Note that all floating point types are synonyms for 'double'

		// Note that the HSQL type 'longvarchar' and 'longvarbinary' are
		// synonyms for 'varchar(16M)' and 'varbinary(16M)' respectively.
		// But using these types results in schema validation issue as
		// described in HHH-9693.

		switch ( sqlTypeCode ) {
			//HSQL has no 'nclob' type, but 'clob' is Unicode (See HHH-10364)
			case NCLOB:
				return "clob";
		}
		if ( getVersion().isBefore( 2 ) ) {
			switch ( sqlTypeCode ) {
				case NUMERIC:
					// Older versions of HSQL did not accept
					// precision for the 'numeric' type
					return "numeric";

				// Older versions of HSQL had no lob support
				case BLOB:
					return "longvarbinary";
				case CLOB:
					return "longvarchar";

			}
		}

		return super.columnType( sqlTypeCode );
	}

	@Override
	protected Integer resolveSqlTypeCode(String typeName, String baseTypeName, TypeConfiguration typeConfiguration) {
		switch ( baseTypeName ) {
			case "DOUBLE":
				return DOUBLE;
		}
		return super.resolveSqlTypeCode( typeName, baseTypeName, typeConfiguration );
	}

	@Override
	public int getDefaultStatementBatchSize() {
		return 15;
	}

	private static DatabaseVersion reflectedVersion(DatabaseVersion version) {
		try {
			final Class<?> props = ReflectHelper.classForName( "org.hsqldb.persist.HsqlDatabaseProperties");
			final String versionString = (String) props.getDeclaredField("THIS_VERSION").get( null );

			return new SimpleDatabaseVersion(
					Integer.parseInt( versionString.substring( 0, 1 ) ),
					Integer.parseInt( versionString.substring( 2, 3 ) ),
					Integer.parseInt( versionString.substring( 4, 5 ) )
			);
		}
		catch (Throwable e) {
			// might be a very old version, or not accessible in class path
			return version;
		}
	}

	@Override
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry(functionContributions);

		CommonFunctionFactory functionFactory = new CommonFunctionFactory(functionContributions);

		// AVG by default uses the input type, so we possibly need to cast the argument type, hence a special function
		functionFactory.avg_castingNonDoubleArguments( this, SqlAstNodeRenderingMode.DEFAULT );

		functionFactory.cot();
		functionFactory.radians();
		functionFactory.degrees();
		functionFactory.log10();
		functionFactory.rand();
		functionFactory.trunc_dateTrunc_trunc();
		functionFactory.pi();
		functionFactory.soundex();
		functionFactory.reverse();
		functionFactory.space();
		functionFactory.repeat();
		functionFactory.translate();
		functionFactory.bitand();
		functionFactory.bitor();
		functionFactory.bitxor();
		functionFactory.bitnot();
		functionFactory.yearMonthDay();
		functionFactory.hourMinuteSecond();
		functionFactory.dayofweekmonthyear();
		functionFactory.weekQuarter();
		functionFactory.daynameMonthname();
		functionFactory.lastDay();
		functionFactory.trim1();
		functionFactory.toCharNumberDateTimestamp();
		functionFactory.concat_pipeOperator();
		functionFactory.localtimeLocaltimestamp();
		functionFactory.bitLength();
		functionFactory.octetLength();
		functionFactory.ascii();
		functionFactory.chr_char();
		functionFactory.instr();
		functionFactory.substr();
		//also natively supports ANSI-style substring()
		functionFactory.position();
		functionFactory.nowCurdateCurtime();
		functionFactory.insert();
		functionFactory.overlay();
		functionFactory.median();
		functionFactory.stddevPopSamp();
		functionFactory.varPopSamp();
		functionFactory.addMonths();
		functionFactory.monthsBetween();
		functionFactory.collate_quoted();

		if ( getVersion().isSameOrAfter( 2 ) ) {
			//SYSDATE is similar to LOCALTIMESTAMP but it returns the timestamp when it is called
			functionFactory.sysdate();
		}

		// from v. 2.2.0 ROWNUM() is supported in all modes as the equivalent of Oracle ROWNUM
		if ( getVersion().isSameOrAfter( 2, 2 ) ) {
			functionFactory.rownum();
		}
		functionFactory.listagg_groupConcat();
		functionFactory.array_hsql();
		functionFactory.arrayAggregate();
		functionFactory.arrayPosition_hsql();
		functionFactory.arrayPositions_hsql();
		functionFactory.arrayLength_cardinality();
		functionFactory.arrayConcat_operator();
		functionFactory.arrayPrepend_operator();
		functionFactory.arrayAppend_operator();
		functionFactory.arrayContains_hsql();
		functionFactory.arrayIntersects_hsql();
		functionFactory.arrayGet_unnest();
		functionFactory.arraySet_hsql();
		functionFactory.arrayRemove_hsql();
		functionFactory.arrayRemoveIndex_unnest( false );
		functionFactory.arraySlice_unnest();
		functionFactory.arrayReplace_unnest();
		functionFactory.arrayTrim_trim_array();
		functionFactory.arrayFill_hsql();
		functionFactory.arrayToString_hsql();

		//trim() requires parameters to be cast when used as trim character
		functionContributions.getFunctionRegistry().register( "trim", new TrimFunction(
				this,
				functionContributions.getTypeConfiguration(),
				SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER
		) );
	}

	@Override
	public String currentTime() {
		return "localtime";
	}

	@Override
	public String currentTimestamp() {
		return "localtimestamp";
	}

	@Override
	public String currentTimestampWithTimeZone() {
		return "current_timestamp";
	}

	@Override
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(
					SessionFactoryImplementor sessionFactory, Statement statement) {
				return new HSQLLegacySqlAstTranslator<>( sessionFactory, statement );
			}
		};
	}
	@Override
	public String castPattern(CastType from, CastType to) {
		String result;
		switch ( to ) {
			case INTEGER:
			case LONG:
				result = BooleanDecoder.toInteger( from );
				if ( result != null ) {
					return result;
				}
				break;
			case BOOLEAN:
				result = BooleanDecoder.toBoolean( from );
				if ( result != null ) {
					return result;
				}
				break;
			case INTEGER_BOOLEAN:
				result = BooleanDecoder.toIntegerBoolean( from );
				if ( result != null ) {
					return result;
				}
				break;
			case YN_BOOLEAN:
				result = BooleanDecoder.toYesNoBoolean( from );
				if ( result != null ) {
					return result;
				}
				break;
			case TF_BOOLEAN:
				result = BooleanDecoder.toTrueFalseBoolean( from );
				if ( result != null ) {
					return result;
				}
				break;
			case STRING:
				result = BooleanDecoder.toString( from );
				if ( result != null ) {
					return "trim(" + result + ')';
				}
				break;
		}
		return super.castPattern( from, to );
	}

	@Override
	public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType, IntervalType intervalType) {
		StringBuilder pattern = new StringBuilder();
		boolean castTo = temporalType != TemporalType.TIMESTAMP && !unit.isDateUnit();
		switch (unit) {
			case NANOSECOND:
			case NATIVE:
				pattern.append("timestampadd(sql_tsi_frac_second,?2,"); //nanos
				break;
			case WEEK:
				pattern.append("dateadd('day',?2*7,");
				break;
			default:
				pattern.append("dateadd('?1',?2,");
		}
		if (castTo) {
			pattern.append("cast(?3 as timestamp)");
		}
		else {
			pattern.append("?3");
		}
		pattern.append(")");
		return pattern.toString();
	}

	@Override
	public String timestampdiffPattern(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) {
		StringBuilder pattern = new StringBuilder();
		boolean castFrom = fromTemporalType != TemporalType.TIMESTAMP && !unit.isDateUnit();
		boolean castTo = toTemporalType != TemporalType.TIMESTAMP && !unit.isDateUnit();
		switch (unit) {
			case NANOSECOND:
			case NATIVE:
				pattern.append("timestampdiff(sql_tsi_frac_second"); //nanos
				break;
			case WEEK:
				pattern.append("(datediff('day'");
			default:
				pattern.append("datediff('?1'");
		}
		pattern.append(',');
		if (castFrom) {
			pattern.append("cast(?2 as timestamp)");
		}
		else {
			pattern.append("?2");
		}
		pattern.append(',');
		if (castTo) {
			pattern.append("cast(?3 as timestamp)");
		}
		else {
			pattern.append("?3");
		}
		pattern.append(")");
		if ( unit == TemporalUnit.WEEK ) {
			pattern.append( "/7)" );
		}
		return pattern.toString();
	}

	@Override
	public boolean supportsDistinctFromPredicate() {
		return true;
	}

	@Override
	public boolean supportsLockTimeouts() {
		return false;
	}

	@Override
	public String getForUpdateString() {
		if ( getVersion().isSameOrAfter( 2 ) ) {
			return " for update";
		}
		else {
			return "";
		}
	}

	@Override
	public LimitHandler getLimitHandler() {
		return getVersion().isBefore( 2 ) ? LegacyHSQLLimitHandler.INSTANCE
				: getVersion().isBefore( 2, 5 ) ? LimitOffsetLimitHandler.OFFSET_ONLY_INSTANCE
				: OffsetFetchLimitHandler.INSTANCE;
	}

	// Note : HSQLDB actually supports [IF EXISTS] before AND after the <tablename>
	// But as CASCADE has to be AFTER IF EXISTS in case it's after the tablename,
	// We put the IF EXISTS before the tablename to be able to add CASCADE after.
	@Override
	public boolean supportsIfExistsAfterTableName() {
		return false;
	}

	@Override
	public boolean supportsIfExistsBeforeTableName() {
		return true;
	}

	@Override
	public boolean supportsColumnCheck() {
		return getVersion().isSameOrAfter( 2 );
	}

	@Override
	public SequenceSupport getSequenceSupport() {
		return HSQLSequenceSupport.INSTANCE;
	}

	@Override
	public String getQuerySequencesString() {
		// this assumes schema support, which is present in 1.8.0 and later...
		return "select * from information_schema.sequences";
	}

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return SequenceInformationExtractorHSQLDBDatabaseImpl.INSTANCE;
	}

	@Override
	public boolean supportsStandardArrays() {
		return true;
	}

	@Override
	public ViolatedConstraintNameExtractor getViolatedConstraintNameExtractor() {
		return getVersion().isBefore( 2 ) ? EXTRACTOR_18 : EXTRACTOR_20;
	}

	private static final ViolatedConstraintNameExtractor EXTRACTOR_18 =
			new TemplatedViolatedConstraintNameExtractor( sqle -> {
				switch ( JdbcExceptionHelper.extractErrorCode( sqle ) ) {
					case -8:
						return extractUsingTemplate(
								"Integrity constraint violation ", " table:",
								sqle.getMessage()
						);
					case -9:
						return extractUsingTemplate(
								"Violation of unique index: ", " in statement [",
								sqle.getMessage()
						);
					case -104:
						return extractUsingTemplate(
								"Unique constraint violation: ", " in statement [",
								sqle.getMessage()
						);
					case -177:
						return extractUsingTemplate(
								"Integrity constraint violation - no parent ", " table:",
								sqle.getMessage()
						);
				}
				return null;
			} );

	@Override
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		return (sqlException, message, sql) -> {
			final int errorCode = JdbcExceptionHelper.extractErrorCode( sqlException );
			final String constraintName;

			switch ( errorCode ) {
				case -104:
					// Unique constraint violation
					constraintName = getViolatedConstraintNameExtractor().extractConstraintName(sqlException);
					return new ConstraintViolationException(
							message,
							sqlException,
							sql,
							ConstraintViolationException.ConstraintKind.UNIQUE,
							constraintName
					);
			}

			return null;
		};
	}

	/**
	 * HSQLDB 2.0 messages have changed
	 * messages may be localized - therefore use the common, non-locale element " table: "
	 */
	private static final ViolatedConstraintNameExtractor EXTRACTOR_20 =
			new TemplatedViolatedConstraintNameExtractor( sqle -> {
				switch ( JdbcExceptionHelper.extractErrorCode( sqle ) ) {
					case -8:
					case -9:
					case -104:
					case -177:
						return extractUsingTemplate(
								"; ", " table: ",
								sqle.getMessage()
						);
				}
				return null;
			} );

	@Override
	public String getSelectClauseNullString(int sqlType, TypeConfiguration typeConfiguration) {
		String literal;
		switch ( sqlType ) {
			case Types.LONGVARCHAR:
			case Types.VARCHAR:
			case Types.CHAR:
				literal = "cast(null as varchar(100))";
				break;
			case Types.LONGVARBINARY:
			case Types.VARBINARY:
			case Types.BINARY:
				literal = "cast(null as varbinary(100))";
				break;
			case Types.CLOB:
				literal = "cast(null as clob)";
				break;
			case Types.BLOB:
				literal = "cast(null as blob)";
				break;
			case Types.DATE:
				literal = "cast(null as date)";
				break;
			case Types.TIMESTAMP:
			case Types.TIMESTAMP_WITH_TIMEZONE:
				literal = "cast(null as timestamp)";
				break;
			case Types.BOOLEAN:
				literal = "cast(null as boolean)";
				break;
			case Types.BIT:
				literal = "cast(null as bit)";
				break;
			case Types.TIME:
				literal = "cast(null as time)";
				break;
			default:
				literal = "cast(null as int)";
		}
		return literal;
	}

	@Override
	public NullOrdering getNullOrdering() {
		return NullOrdering.FIRST;
	}

	@Override
	public SqmMultiTableMutationStrategy getFallbackSqmMutationStrategy(
			EntityMappingType rootEntityDescriptor,
			RuntimeModelCreationContext runtimeModelCreationContext) {

		// Hibernate uses this information for temporary tables that it uses for its own operations
		// therefore the appropriate strategy is taken with different versions of HSQLDB

		// All versions of HSQLDB support GLOBAL TEMPORARY tables where the table
		// definition is shared by all users but data is private to the session
		// HSQLDB 2.0 also supports session-based LOCAL TEMPORARY tables where
		// the definition and data is private to the session and table declaration
		// can happen in the middle of a transaction

		if ( getVersion().isBefore( 2 ) ) {
			return new GlobalTemporaryTableMutationStrategy(
					TemporaryTable.createIdTable(
							rootEntityDescriptor,
							basename -> TemporaryTable.ID_TABLE_PREFIX + basename,
							this,
							runtimeModelCreationContext
					),
					runtimeModelCreationContext.getSessionFactory()
			);
		}
		else {
			return new LocalTemporaryTableMutationStrategy(
					// With HSQLDB 2.0, the table name is qualified with MODULE to assist the drop
					// statement (in-case there is a global name beginning with HT_)
					TemporaryTable.createIdTable(
							rootEntityDescriptor,
							basename -> "MODULE." + TemporaryTable.ID_TABLE_PREFIX + basename,
							this,
							runtimeModelCreationContext
					),
					runtimeModelCreationContext.getSessionFactory()
			);
		}
	}

	@Override
	public SqmMultiTableInsertStrategy getFallbackSqmInsertStrategy(
			EntityMappingType rootEntityDescriptor,
			RuntimeModelCreationContext runtimeModelCreationContext) {

		// Hibernate uses this information for temporary tables that it uses for its own operations
		// therefore the appropriate strategy is taken with different versions of HSQLDB

		// All versions of HSQLDB support GLOBAL TEMPORARY tables where the table
		// definition is shared by all users but data is private to the session
		// HSQLDB 2.0 also supports session-based LOCAL TEMPORARY tables where
		// the definition and data is private to the session and table declaration
		// can happen in the middle of a transaction

		if ( getVersion().isBefore( 2 ) ) {
			return new GlobalTemporaryTableInsertStrategy(
					TemporaryTable.createEntityTable(
							rootEntityDescriptor,
							name -> TemporaryTable.ENTITY_TABLE_PREFIX + name,
							this,
							runtimeModelCreationContext
					),
					runtimeModelCreationContext.getSessionFactory()
			);
		}
		else {
			return new LocalTemporaryTableInsertStrategy(
					// With HSQLDB 2.0, the table name is qualified with MODULE to assist the drop
					// statement (in-case there is a global name beginning with HT_)
					TemporaryTable.createEntityTable(
							rootEntityDescriptor,
							name -> "MODULE." + TemporaryTable.ENTITY_TABLE_PREFIX + name,
							this,
							runtimeModelCreationContext
					),
					runtimeModelCreationContext.getSessionFactory()
			);
		}
	}

	@Override
	public TemporaryTableKind getSupportedTemporaryTableKind() {
		return getVersion().isBefore( 2 ) ? TemporaryTableKind.GLOBAL : TemporaryTableKind.LOCAL;
	}

	@Override
	public String getTemporaryTableCreateCommand() {
		return getVersion().isBefore( 2 ) ? super.getTemporaryTableCreateCommand() : "declare local temporary table";
	}

	@Override
	public AfterUseAction getTemporaryTableAfterUseAction() {
		// Version 1.8 GLOBAL TEMPORARY table definitions persist beyond the end
		// of the session (by default, data is cleared at commit).
		return getVersion().isBefore( 2 ) ? AfterUseAction.CLEAN : AfterUseAction.DROP;
	}

	@Override
	public BeforeUseAction getTemporaryTableBeforeUseAction() {
		return getVersion().isBefore( 2 ) ? BeforeUseAction.NONE : BeforeUseAction.CREATE;
	}

	// current timestamp support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * HSQLDB 1.8.x requires CALL CURRENT_TIMESTAMP but this should not
	 * be treated as a callable statement. It is equivalent to
	 * "select current_timestamp from dual" in some databases.
	 * HSQLDB 2.0 also supports VALUES CURRENT_TIMESTAMP
	 * <p>
	 * {@inheritDoc}
	 */
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
		return "call current_timestamp";
	}

	@Override
	public boolean doesRoundTemporalOnOverflow() {
		// HSQLDB does truncation
		return false;
	}

	/**
	 * For HSQLDB 2.0, this is a copy of the base class implementation.
	 * For HSQLDB 1.8, only READ_UNCOMMITTED is supported.
	 * <p>
	 * {@inheritDoc}
	 */
	@Override
	public LockingStrategy getLockingStrategy(EntityPersister lockable, LockMode lockMode) {
		switch (lockMode) {
			case PESSIMISTIC_FORCE_INCREMENT:
				return new PessimisticForceIncrementLockingStrategy( lockable, lockMode);
			case PESSIMISTIC_WRITE:
				return new PessimisticWriteSelectLockingStrategy( lockable, lockMode);
			case PESSIMISTIC_READ:
				return new PessimisticReadSelectLockingStrategy( lockable, lockMode);
			case OPTIMISTIC:
				return new OptimisticLockingStrategy( lockable, lockMode);
			case OPTIMISTIC_FORCE_INCREMENT:
				return new OptimisticForceIncrementLockingStrategy( lockable, lockMode);
		}
		if ( getVersion().isBefore( 2 ) ) {
			return new ReadUncommittedLockingStrategy( lockable, lockMode );
		}
		else {
			return new SelectLockingStrategy( lockable, lockMode );
		}
	}

	private static class ReadUncommittedLockingStrategy extends SelectLockingStrategy {
		private ReadUncommittedLockingStrategy(EntityPersister lockable, LockMode lockMode) {
			super( lockable, lockMode );
		}

		@Override
		public void lock(Object id, Object version, Object object, int timeout, EventSource session)
				throws StaleObjectStateException, JDBCException {
			if ( getLockMode().greaterThan( LockMode.READ ) ) {
				LOG.hsqldbSupportsOnlyReadCommittedIsolation();
			}
			super.lock( id, version, object, timeout, session );
		}
	}

	@Override
	public boolean supportsCommentOn() {
		return getVersion().isSameOrAfter( 2 );
	}

	// Overridden informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public boolean doesReadCommittedCauseWritersToBlockReaders() {
		return getVersion().isSameOrAfter( 2 );
	}

	@Override
	public boolean doesRepeatableReadCauseReadersToBlockWriters() {
		return getVersion().isSameOrAfter( 2 );
	}

	@Override
	public boolean supportsLobValueChangePropagation() {
		return false;
	}

	@Override
	public void appendBooleanValueString(SqlAppender appender, boolean bool) {
		appender.appendSql( bool );
	}

	@Override
	public boolean supportsTupleCounts() {
		return true;
	}

	@Override
	public boolean supportsTupleDistinctCounts() {
		// from v. 2.2.9 is added support for COUNT(DISTINCT ...) with multiple arguments
		return getVersion().isSameOrAfter( 2, 2, 9 );
	}

	@Override
	public boolean supportsOffsetInSubquery() {
		return true;
	}

	@Override
	public boolean supportsLateral() {
		return getVersion().isSameOrAfter( 2, 6, 1 );
	}

	@Override
	public boolean requiresFloatCastingOfIntegerDivision() {
		return true;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return identityColumnSupport;
	}

	@Override
	public NameQualifierSupport getNameQualifierSupport() {
		return NameQualifierSupport.SCHEMA;
	}

	@Override
	public boolean supportsNamedParameters(DatabaseMetaData databaseMetaData) {
		return false;
	}

	@Override
	public FunctionalDependencyAnalysisSupport getFunctionalDependencyAnalysisSupport() {
		return FunctionalDependencyAnalysisSupportImpl.TABLE_REFERENCE;
	}

	// Do not drop constraints explicitly, just do this by cascading instead.
	@Override
	public boolean dropConstraints() {
		return false;
	}

	@Override
	public String getCascadeConstraintsString() {
		return " cascade ";
	}

	@Override
	public void appendDatetimeFormat(SqlAppender appender, String format) {
		appender.appendSql(
				OracleDialect.datetimeFormat( format, false, false )
						// HSQL is case sensitive i.e. requires MONTH and DAY instead of Month and Day
						.replace("MMMM", "MONTH")
						.replace("EEEE", "DAY")
						.replace("SSSSSS", "FF")
						.replace("SSSSS", "FF")
						.replace("SSSS", "FF")
						.replace("SSS", "FF")
						.replace("SS", "FF")
						.replace("S", "FF")
						.result()
		);
	}

	@Override
	public String translateExtractField(TemporalUnit unit) {
		//TODO: does not support MICROSECOND, but on the
		//      other hand it doesn't support microsecond
		//      precision in timestamps either so who cares?
		switch (unit) {
			case WEEK: return "week_of_year"; //this is the ISO week number, I believe
			default: return unit.toString();
		}
	}

	@Override
	public IdentifierHelper buildIdentifierHelper(IdentifierHelperBuilder builder, DatabaseMetaData dbMetaData)
			throws SQLException {
		builder.setAutoQuoteInitialUnderscore(true);
		return super.buildIdentifierHelper(builder, dbMetaData);
	}

	@Override
	public UniqueDelegate getUniqueDelegate() {
		return uniqueDelegate;
	}

	@Override
	public DmlTargetColumnQualifierSupport getDmlTargetColumnQualifierSupport() {
		return DmlTargetColumnQualifierSupport.TABLE_ALIAS;
	}
}

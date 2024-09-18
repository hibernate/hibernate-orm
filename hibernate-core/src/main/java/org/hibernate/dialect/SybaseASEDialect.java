/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.Length;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.QueryTimeoutException;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.TopLimitHandler;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor;
import org.hibernate.exception.spi.ViolatedConstraintNameExtractor;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.internal.util.config.ConfigurationHelper;
import org.hibernate.query.sqm.IntervalType;
import org.hibernate.query.sqm.TemporalUnit;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.TimestampJdbcType;
import org.hibernate.type.descriptor.jdbc.TinyIntJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.descriptor.sql.internal.CapacityDependentDdlType;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;

import jakarta.persistence.TemporalType;

import static org.hibernate.cfg.DialectSpecificSettings.SYBASE_ANSI_NULL;
import static org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor.extractUsingTemplate;
import static org.hibernate.type.SqlTypes.BOOLEAN;
import static org.hibernate.type.SqlTypes.DATE;
import static org.hibernate.type.SqlTypes.NCLOB;
import static org.hibernate.type.SqlTypes.TIME;
import static org.hibernate.type.SqlTypes.TIMESTAMP;
import static org.hibernate.type.SqlTypes.TIMESTAMP_WITH_TIMEZONE;

/**
 * A {@linkplain Dialect SQL dialect} for Sybase Adaptive Server Enterprise 16 and above.
 */
public class SybaseASEDialect extends SybaseDialect {

	private static final DatabaseVersion MINIMUM_VERSION = DatabaseVersion.make( 16, 0 );

	private final SizeStrategy sizeStrategy = new SizeStrategyImpl() {
		@Override
		public Size resolveSize(
				JdbcType jdbcType,
				JavaType<?> javaType,
				Integer precision,
				Integer scale,
				Long length) {
			switch ( jdbcType.getDdlTypeCode() ) {
				case Types.NCLOB:
				case Types.CLOB:
				case Types.BLOB:
					return super.resolveSize(
							jdbcType,
							javaType,
							precision,
							scale,
							length == null ? getDefaultLobLength() : length
					);
				case Types.FLOAT:
					// Sybase ASE allows FLOAT with a precision up to 48
					if ( precision != null ) {
						return Size.precision( Math.min( Math.max( precision, 1 ), 48 ) );
					}
			}
			return super.resolveSize( jdbcType, javaType, precision, scale, length );
		}
	};

	private final boolean ansiNull;

	public SybaseASEDialect() {
		this( MINIMUM_VERSION );
	}

	public SybaseASEDialect(DatabaseVersion version) {
		super(version);
		ansiNull = false;
	}

	public SybaseASEDialect(DialectResolutionInfo info) {
		super(info);
		ansiNull = isAnsiNull( info );
	}

	@Override
	protected String columnType(int sqlTypeCode) {
		switch ( sqlTypeCode ) {
			case BOOLEAN: {
				// On Sybase ASE, the 'bit' type cannot be null,
				// and cannot have indexes (while we don't use
				// tinyint to store signed bytes, we can use it
				// to store boolean values)
				return "tinyint";
			}
			case DATE: {
				return "date";
			}
			case TIME: {
				return "time";
			}
			case NCLOB: {
				// Sybase uses `unitext` instead of the T-SQL `ntext` type name
				return "unitext";
			}
			default: {
				return super.columnType( sqlTypeCode );
			}
		}
	}

	@Override
	protected void registerColumnTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.registerColumnTypes( typeContributions, serviceRegistry );
		final DdlTypeRegistry ddlTypeRegistry = typeContributions.getTypeConfiguration().getDdlTypeRegistry();

		// According to Wikipedia bigdatetime and bigtime were added in 15.5
		// But with jTDS we can't use them as the driver can't handle the types
		if ( getDriverKind() != SybaseDriverKind.JTDS ) {
			ddlTypeRegistry.addDescriptor(
					CapacityDependentDdlType.builder( TIME, "bigtime", "bigtime", this )
							.withTypeCapacity( 3, "time" )
							.build()
			);
			ddlTypeRegistry.addDescriptor(
					CapacityDependentDdlType.builder( TIMESTAMP, "bigdatetime", "bigdatetime", this )
							.withTypeCapacity( 3, "datetime" )
							.build()
			);
			ddlTypeRegistry.addDescriptor(
					CapacityDependentDdlType.builder( TIMESTAMP_WITH_TIMEZONE, "bigdatetime", "bigdatetime", this )
							.withTypeCapacity( 3, "datetime" )
							.build()
			);
		}
	}

	@Override
	public int getMaxVarcharLength() {
		// the maximum length of a VARCHAR or VARBINARY
		// column depends on the page size and ASE version
		// and is actually a limit on the whole row length,
		// not the individual column length -- anyway, the
		// largest possible page size is 16k, so that's a
		// hard upper limit
		return 16_384;
	}

	@Override
	public long getDefaultLobLength() {
		return Length.LONG32;
	}

	private static boolean isAnsiNull(DialectResolutionInfo info) {
		final DatabaseMetaData databaseMetaData = info.getDatabaseMetadata();
		if ( databaseMetaData != null ) {
			try (java.sql.Statement s = databaseMetaData.getConnection().createStatement() ) {
				final ResultSet rs = s.executeQuery( "SELECT @@options" );
				if ( rs.next() ) {
					final byte[] optionBytes = rs.getBytes( 1 );
					// By trial and error, enabling and disabling ansinull revealed that this bit is the indicator
					return ( optionBytes[4] & 2 ) == 2;
				}
			}
			catch (SQLException ex) {
				// Ignore
			}
		}
		// default to the dialect-specific configuration setting
		return ConfigurationHelper.getBoolean( SYBASE_ANSI_NULL, info.getConfigurationValues(), false );
	}

	@Override
	public boolean isAnsiNullOn() {
		return ansiNull;
	}

	@Override
	public int getFloatPrecision() {
		return 15;
	}

	@Override
	public int getDoublePrecision() {
		return 48;
	}

	@Override
	public SizeStrategy getSizeStrategy() {
		return sizeStrategy;
	}

	@Override
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(
					SessionFactoryImplementor sessionFactory, Statement statement) {
				return new SybaseASESqlAstTranslator<>( sessionFactory, statement );
			}
		};
	}

	/**
	 * The Sybase ASE {@code BIT} type does not allow
	 * null values, so we don't use it.
	 *
	 * @return false
	 */
	@Override
	public boolean supportsBitType() {
		return false;
	}

	@Override
	public boolean supportsDistinctFromPredicate() {
		return getVersion().isSameOrAfter( 16, 3 );
	}

	@Override
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes( typeContributions, serviceRegistry );

		final JdbcTypeRegistry jdbcTypeRegistry = typeContributions.getTypeConfiguration()
				.getJdbcTypeRegistry();
		jdbcTypeRegistry.addDescriptor( Types.BOOLEAN, TinyIntJdbcType.INSTANCE );
		// At least the jTDS driver does not support this type code
		if ( getDriverKind() == SybaseDriverKind.JTDS ) {
			jdbcTypeRegistry.addDescriptor( Types.TIMESTAMP_WITH_TIMEZONE, TimestampJdbcType.INSTANCE );
		}
	}

	@Override
	public String currentDate() {
		return "current_date()";
	}

	@Override
	public String currentTime() {
		return "current_time()";
	}

	@Override
	public String currentTimestamp() {
		return "current_bigdatetime()";
	}

	/**
	 * Sybase ASE in principle supports microsecond
	 * precision for {code bigdatetime}, but
	 * unfortunately its duration arithmetic
	 * functions have a nasty habit of overflowing.
	 * So to give ourselves a little extra headroom,
	 * we will use {@code millisecond} as the native
	 * unit of precision.
	 */
	@Override
	public long getFractionalSecondPrecisionInNanos() {
		return 1_000_000;
	}

	@Override
	public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType, IntervalType intervalType) {
		switch ( unit ) {
			case NANOSECOND:
				return "dateadd(ms,?2/1000000,?3)";
//				return "dateadd(mcs,?2/1000,?3)";
			case NATIVE:
				return "dateadd(ms,?2,?3)";
//				return "dateadd(mcs,?2,?3)";
			default:
				return "dateadd(?1,?2,?3)";
		}
	}

	@Override
	public String timestampdiffPattern(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) {
		switch ( unit ) {
			case NANOSECOND:
				return "(cast(datediff(ms,?2,?3) as numeric(21))*1000000)";
//				return "(cast(datediff(mcs,?2,?3) as numeric(21))*1000)";
//				}
			case NATIVE:
				return "cast(datediff(ms,?2,?3) as numeric(21))";
//				return "cast(datediff(mcs,cast(?2 as bigdatetime),cast(?3 as bigdatetime)) as numeric(21))";
			default:
				return "datediff(?1,?2,?3)";
		}
	}

	@Override
	protected void registerDefaultKeywords() {
		super.registerDefaultKeywords();
		registerKeyword( "add" );
		registerKeyword( "all" );
		registerKeyword( "alter" );
		registerKeyword( "and" );
		registerKeyword( "any" );
		registerKeyword( "arith_overflow" );
		registerKeyword( "as" );
		registerKeyword( "asc" );
		registerKeyword( "at" );
		registerKeyword( "authorization" );
		registerKeyword( "avg" );
		registerKeyword( "begin" );
		registerKeyword( "between" );
		registerKeyword( "break" );
		registerKeyword( "browse" );
		registerKeyword( "bulk" );
		registerKeyword( "by" );
		registerKeyword( "cascade" );
		registerKeyword( "case" );
		registerKeyword( "char_convert" );
		registerKeyword( "check" );
		registerKeyword( "checkpoint" );
		registerKeyword( "close" );
		registerKeyword( "clustered" );
		registerKeyword( "coalesce" );
		registerKeyword( "commit" );
		registerKeyword( "compute" );
		registerKeyword( "confirm" );
		registerKeyword( "connect" );
		registerKeyword( "constraint" );
		registerKeyword( "continue" );
		registerKeyword( "controlrow" );
		registerKeyword( "convert" );
		registerKeyword( "count" );
		registerKeyword( "count_big" );
		registerKeyword( "create" );
		registerKeyword( "current" );
		registerKeyword( "cursor" );
		registerKeyword( "database" );
		registerKeyword( "dbcc" );
		registerKeyword( "deallocate" );
		registerKeyword( "declare" );
		registerKeyword( "decrypt" );
		registerKeyword( "default" );
		registerKeyword( "delete" );
		registerKeyword( "desc" );
		registerKeyword( "determnistic" );
		registerKeyword( "disk" );
		registerKeyword( "distinct" );
		registerKeyword( "drop" );
		registerKeyword( "dummy" );
		registerKeyword( "dump" );
		registerKeyword( "else" );
		registerKeyword( "encrypt" );
		registerKeyword( "end" );
		registerKeyword( "endtran" );
		registerKeyword( "errlvl" );
		registerKeyword( "errordata" );
		registerKeyword( "errorexit" );
		registerKeyword( "escape" );
		registerKeyword( "except" );
		registerKeyword( "exclusive" );
		registerKeyword( "exec" );
		registerKeyword( "execute" );
		registerKeyword( "exist" );
		registerKeyword( "exit" );
		registerKeyword( "exp_row_size" );
		registerKeyword( "external" );
		registerKeyword( "fetch" );
		registerKeyword( "fillfactor" );
		registerKeyword( "for" );
		registerKeyword( "foreign" );
		registerKeyword( "from" );
		registerKeyword( "goto" );
		registerKeyword( "grant" );
		registerKeyword( "group" );
		registerKeyword( "having" );
		registerKeyword( "holdlock" );
		registerKeyword( "identity" );
		registerKeyword( "identity_gap" );
		registerKeyword( "identity_start" );
		registerKeyword( "if" );
		registerKeyword( "in" );
		registerKeyword( "index" );
		registerKeyword( "inout" );
		registerKeyword( "insensitive" );
		registerKeyword( "insert" );
		registerKeyword( "install" );
		registerKeyword( "intersect" );
		registerKeyword( "into" );
		registerKeyword( "is" );
		registerKeyword( "isolation" );
		registerKeyword( "jar" );
		registerKeyword( "join" );
		registerKeyword( "key" );
		registerKeyword( "kill" );
		registerKeyword( "level" );
		registerKeyword( "like" );
		registerKeyword( "lineno" );
		registerKeyword( "load" );
		registerKeyword( "lock" );
		registerKeyword( "materialized" );
		registerKeyword( "max" );
		registerKeyword( "max_rows_per_page" );
		registerKeyword( "min" );
		registerKeyword( "mirror" );
		registerKeyword( "mirrorexit" );
		registerKeyword( "modify" );
		registerKeyword( "national" );
		registerKeyword( "new" );
		registerKeyword( "noholdlock" );
		registerKeyword( "nonclustered" );
		registerKeyword( "nonscrollable" );
		registerKeyword( "non_sensitive" );
		registerKeyword( "not" );
		registerKeyword( "null" );
		registerKeyword( "nullif" );
		registerKeyword( "numeric_truncation" );
		registerKeyword( "of" );
		registerKeyword( "off" );
		registerKeyword( "offsets" );
		registerKeyword( "on" );
		registerKeyword( "once" );
		registerKeyword( "online" );
		registerKeyword( "only" );
		registerKeyword( "open" );
		registerKeyword( "option" );
		registerKeyword( "or" );
		registerKeyword( "order" );
		registerKeyword( "out" );
		registerKeyword( "output" );
		registerKeyword( "over" );
		registerKeyword( "artition" );
		registerKeyword( "perm" );
		registerKeyword( "permanent" );
		registerKeyword( "plan" );
		registerKeyword( "prepare" );
		registerKeyword( "primary" );
		registerKeyword( "print" );
		registerKeyword( "privileges" );
		registerKeyword( "proc" );
		registerKeyword( "procedure" );
		registerKeyword( "processexit" );
		registerKeyword( "proxy_table" );
		registerKeyword( "public" );
		registerKeyword( "quiesce" );
		registerKeyword( "raiserror" );
		registerKeyword( "read" );
		registerKeyword( "readpast" );
		registerKeyword( "readtext" );
		registerKeyword( "reconfigure" );
		registerKeyword( "references" );
		registerKeyword( "remove" );
		registerKeyword( "reorg" );
		registerKeyword( "replace" );
		registerKeyword( "replication" );
		registerKeyword( "reservepagegap" );
		registerKeyword( "return" );
		registerKeyword( "returns" );
		registerKeyword( "revoke" );
		registerKeyword( "role" );
		registerKeyword( "rollback" );
		registerKeyword( "rowcount" );
		registerKeyword( "rows" );
		registerKeyword( "rule" );
		registerKeyword( "save" );
		registerKeyword( "schema" );
		registerKeyword( "scroll" );
		registerKeyword( "scrollable" );
		registerKeyword( "select" );
		registerKeyword( "semi_sensitive" );
		registerKeyword( "set" );
		registerKeyword( "setuser" );
		registerKeyword( "shared" );
		registerKeyword( "shutdown" );
		registerKeyword( "some" );
		registerKeyword( "statistics" );
		registerKeyword( "stringsize" );
		registerKeyword( "stripe" );
		registerKeyword( "sum" );
		registerKeyword( "syb_identity" );
		registerKeyword( "syb_restree" );
		registerKeyword( "syb_terminate" );
		registerKeyword( "top" );
		registerKeyword( "table" );
		registerKeyword( "temp" );
		registerKeyword( "temporary" );
		registerKeyword( "textsize" );
		registerKeyword( "to" );
		registerKeyword( "tracefile" );
		registerKeyword( "tran" );
		registerKeyword( "transaction" );
		registerKeyword( "trigger" );
		registerKeyword( "truncate" );
		registerKeyword( "tsequal" );
		registerKeyword( "union" );
		registerKeyword( "unique" );
		registerKeyword( "unpartition" );
		registerKeyword( "update" );
		registerKeyword( "use" );
		registerKeyword( "user" );
		registerKeyword( "user_option" );
		registerKeyword( "using" );
		registerKeyword( "values" );
		registerKeyword( "varying" );
		registerKeyword( "view" );
		registerKeyword( "waitfor" );
		registerKeyword( "when" );
		registerKeyword( "where" );
		registerKeyword( "while" );
		registerKeyword( "with" );
		registerKeyword( "work" );
		registerKeyword( "writetext" );
		registerKeyword( "xmlextract" );
		registerKeyword( "xmlparse" );
		registerKeyword( "xmltest" );
		registerKeyword( "xmlvalidate" );
	}

// Overridden informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


	@Override
	public boolean supportsCascadeDelete() {
		return false;
	}

	@Override
	public int getMaxAliasLength() {
		return 30;
	}

	@Override
	public int getMaxIdentifierLength() {
		return 255;
	}

	@Override
	public boolean supportsLockTimeouts() {
		return false;
	}

	@Override
	public boolean supportsOrderByInSubquery() {
		return false;
	}

	@Override
	public boolean supportsUnionInSubquery() {
		// At least not according to HHH-3637
		return false;
	}

	@Override
	public boolean supportsPartitionBy() {
		return false;
	}

	@Override
	public String getTableTypeString() {
		//HHH-7298 I don't know if this would break something or cause
		//some side effects, but it is required to use 'select for update'
		return " lock datarows";
	}

	@Override
	public boolean supportsLobValueChangePropagation() {
		return false;
	}

	@Override
	public boolean supportsSkipLocked() {
		// It does support skipping locked rows only for READ locking
		return false;
	}

	@Override
	public String appendLockHint(LockOptions mode, String tableName) {
		final String lockHint = super.appendLockHint( mode, tableName );
		return !mode.getLockMode().greaterThan( LockMode.READ ) && mode.getTimeOut() == LockOptions.SKIP_LOCKED
				? lockHint + " readpast"
				: lockHint;
	}

	@Override
	public String toQuotedIdentifier(String name) {
		if ( name == null || name.isEmpty() ) {
			return name;
		}
		if ( name.charAt( 0 ) == '#' ) {
			// Temporary tables must start with a '#' character,
			// but Sybase doesn't support quoting of such identifiers,
			// so we simply don't apply quoting in this case
			return name;
		}
		return super.toQuotedIdentifier( name );
	}

	@Override
	public ViolatedConstraintNameExtractor getViolatedConstraintNameExtractor() {
		return EXTRACTOR;
	}

	/**
	 * Constraint-name extractor for Sybase ASE constraint violation exceptions.
	 * Orginally contributed by Denny Bartelt.
	 */
	private static final ViolatedConstraintNameExtractor EXTRACTOR =
			new TemplatedViolatedConstraintNameExtractor( sqle -> {
				final String sqlState = JdbcExceptionHelper.extractSqlState( sqle );
				final int errorCode = JdbcExceptionHelper.extractErrorCode( sqle );
				if ( sqlState != null ) {
					switch ( sqlState ) {
						case "S1000":
						case "23000":
							switch ( errorCode ) {
								case 2601:
									// UNIQUE VIOLATION
									return extractUsingTemplate( "with unique index '", "'", sqle.getMessage() );
								case 546:
									// Foreign key violation
									return extractUsingTemplate( "constraint name = '", "'", sqle.getMessage() );
							}
							break;
					}
				}
				return null;
			} );

	@Override
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		return (sqlException, message, sql) -> {
			final String sqlState = JdbcExceptionHelper.extractSqlState( sqlException );
			final int errorCode = JdbcExceptionHelper.extractErrorCode( sqlException );
			if ( sqlState != null ) {
				switch ( sqlState ) {
					case "HY008":
						return new QueryTimeoutException( message, sqlException, sql );
					case "JZ0TO":
					case "JZ006":
						return new LockTimeoutException( message, sqlException, sql );
					case "S1000":
					case "23000":
						switch ( errorCode ) {
							case 515:
								// Attempt to insert NULL value into column; column does not allow nulls.
								return new ConstraintViolationException(
										message,
										sqlException,
										sql,
										getViolatedConstraintNameExtractor().extractConstraintName( sqlException )
								);
							case 546:
								// Foreign key violation
								return new ConstraintViolationException(
										message,
										sqlException,
										sql,
										getViolatedConstraintNameExtractor().extractConstraintName( sqlException )
								);
							case 2601:
								// Unique constraint violation
								return new ConstraintViolationException(
										message,
										sqlException,
										sql,
										ConstraintViolationException.ConstraintKind.UNIQUE,
										getViolatedConstraintNameExtractor().extractConstraintName( sqlException )
								);
						}
						break;
					case "ZZZZZ":
						if ( 515 == errorCode ) {
							// Attempt to insert NULL value into column; column does not allow nulls.
							return new ConstraintViolationException(
									message,
									sqlException,
									sql,
									getViolatedConstraintNameExtractor().extractConstraintName( sqlException )
							);
						}
						break;
				}
			}
			return null;
		};
	}

	@Override
	public LimitHandler getLimitHandler() {
		return new TopLimitHandler(false);
	}

	@Override
	public String getDual() {
		return "(select 1 c1)";
	}
}

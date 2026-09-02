/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.identifier.spi.KeywordRegistration;

import org.hibernate.dialect.temporaltype.spi.TemporalOperationSupport;

import org.hibernate.dialect.temporaltype.spi.CurrentTemporalSupport;

import org.hibernate.SPI;
import static org.hibernate.SPI.Role.USE;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import org.hibernate.dialect.type.spi.StandardDdlTypes;

import org.hibernate.dialect.type.spi.TypeSizingProfile;

import org.hibernate.dialect.sql.ast.spi.MutationKind;
import org.hibernate.dialect.sql.ast.spi.MutationSyntaxCapability;
import org.hibernate.dialect.sql.ast.spi.MutationSyntaxSupport;
import org.hibernate.dialect.sql.ast.spi.PredicateSupport;
import org.hibernate.dialect.sql.ast.spi.SetOperationSupport;
import org.hibernate.dialect.sql.ast.spi.ValuesListSupport;
import org.hibernate.dialect.sql.ast.spi.SubquerySupport;
import org.hibernate.dialect.sql.ast.spi.SingleRowTableSupport;

import jakarta.persistence.TemporalType;
import org.hibernate.QueryTimeoutException;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.jdbc.spi.SybaseDriverKind;
import org.hibernate.dialect.aggregate.spi.AggregateSupport;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.lock.spi.StandardLockingSupports;
import org.hibernate.dialect.pagination.spi.LimitHandler;
import org.hibernate.dialect.pagination.spi.TopLimitHandler;
import org.hibernate.dialect.type.spi.SizeStrategy;
import org.hibernate.dialect.type.spi.StandardSizeStrategy;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor;
import org.hibernate.exception.spi.ViolatedConstraintNameExtractor;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.query.sqm.SetOperator;
import org.hibernate.dialect.temporaltype.spi.IntervalType;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslatorFactory;
import org.hibernate.dialect.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.TimestampJdbcType;
import org.hibernate.type.descriptor.jdbc.TinyIntJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import static org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor.extractUsingTemplate;
import static org.hibernate.jdbc.spi.JdbcExceptionHelper.extractErrorCode;
import static org.hibernate.jdbc.spi.JdbcExceptionHelper.extractSqlState;
import static org.hibernate.type.SqlTypes.BIGINT;
import static org.hibernate.type.SqlTypes.BOOLEAN;
import static org.hibernate.type.SqlTypes.DATE;
import static org.hibernate.type.SqlTypes.TIME;
import static org.hibernate.type.SqlTypes.TIMESTAMP;
import static org.hibernate.type.SqlTypes.TIMESTAMP_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.XML_ARRAY;

/**
 * A {@linkplain Dialect SQL dialect} for Sybase Adaptive Server Enterprise 11.9 and above.
 */
public class SybaseASELegacyDialect extends SybaseLegacyDialect implements CurrentTemporalSupport, TemporalOperationSupport {

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public TemporalOperationSupport getTemporalOperationSupport() {
		return this;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public CurrentTemporalSupport getCurrentTemporalSupport() {
		return this;
	}
	private final TypeSizingProfile typeSizingProfile = TypeSizingProfile.builder( super.getTypeSizingProfile() )
			.floatPrecision( 15 ).doublePrecision( 48 )
			.maxVarcharLength( 16_384 ).maxVarcharCapacity( 16_384 )
			.maxNVarcharLength( 16_384 ).maxNVarcharCapacity( 16_384 )
			.maxVarbinaryLength( 16_384 ).maxVarbinaryCapacity( 16_384 )
			.build();

	@Override public TypeSizingProfile getTypeSizingProfile() { return typeSizingProfile; }

	private final SizeStrategy sizeStrategy = new StandardSizeStrategy( this ) {
		@Override
		public Size resolveSize(
				JdbcType jdbcType,
				JavaType<?> javaType,
				Integer precision,
				Integer scale,
				Long length) {
			switch ( jdbcType.getDefaultSqlTypeCode() ) {
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

	public SybaseASELegacyDialect() {
		this( DatabaseVersion.make( 11 ) );
	}

	public SybaseASELegacyDialect(DatabaseVersion version) {
		super(version);
		ansiNull = false;
	}

	public SybaseASELegacyDialect(DialectResolutionInfo info) {
		super(info);
		ansiNull = isAnsiNull( info.getDatabaseMetadata() );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	protected String columnType(int sqlTypeCode) {
		return switch ( sqlTypeCode ) {
			// On Sybase ASE, the 'bit' type cannot be null,
			// and cannot have indexes (while we don't use
			// tinyint to store signed bytes, we can use it
			// to store boolean values)
			case BOOLEAN ->  "tinyint";
			// Sybase ASE didn't introduce 'bigint' until version 15.0
			case BIGINT -> getVersion().isBefore( 15 ) ? "numeric(19,0)" : super.columnType( sqlTypeCode );
			case DATE -> getVersion().isSameOrAfter( 12 ) ? "date" : super.columnType( sqlTypeCode );
			case TIME -> getVersion().isSameOrAfter( 12 ) ? "time" : super.columnType( sqlTypeCode );
			default -> super.columnType( sqlTypeCode );
		};
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	protected void registerColumnTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.registerColumnTypes( typeContributions, serviceRegistry );
		final DdlTypeRegistry ddlTypeRegistry = typeContributions.getTypeConfiguration().getDdlTypeRegistry();

		// According to Wikipedia bigdatetime and bigtime were added in 15.5
		// But with jTDS we can't use them as the driver can't handle the types
		if ( getVersion().isSameOrAfter( 15, 5 ) && getDriverKind() != SybaseDriverKind.JTDS ) {
			ddlTypeRegistry.addDescriptor(
					StandardDdlTypes.builder( TIME, "bigtime", this ).castTypeName( "bigtime" )
							.withTypeCapacity( 3, "time" )
							.build()
			);
			ddlTypeRegistry.addDescriptor(
					StandardDdlTypes.builder( TIMESTAMP, "bigdatetime", this ).castTypeName( "bigdatetime" )
							.withTypeCapacity( 3, "datetime" )
							.build()
			);
			ddlTypeRegistry.addDescriptor(
					StandardDdlTypes.builder( TIMESTAMP_WITH_TIMEZONE, "bigdatetime", this ).castTypeName( "bigdatetime" )
							.withTypeCapacity( 3, "datetime" )
							.build()
			);
		}
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public int getPreferredSqlTypeCodeForArray() {
		return XML_ARRAY;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry( functionContributions );

		CommonFunctionFactory functionFactory = new CommonFunctionFactory( functionContributions);

		functionFactory.unnest_sybasease();
		functionFactory.generateSeries_sybasease( getMaximumSeriesSize() );
		functionFactory.xmltable_sybasease();
	}

	/**
	 * Sybase ASE doesn't support the {@code generate_series} function or {@code lateral} recursive CTEs,
	 * so it has to be emulated with the {@code xmltable} and {@code replicate} functions.
	 */
	protected int getMaximumSeriesSize() {
		// The maximum possible value for replicating an XML tag, so that the resulting string stays below the 16K limit
		// https://infocenter.sybase.com/help/index.jsp?topic=/com.sybase.infocenter.dc32300.1570/html/sqlug/sqlug31.htm
		return 4094;
	}

	private static boolean isAnsiNull(DatabaseMetaData databaseMetaData) {
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
		return false;
	}

	public boolean isAnsiNullOn() {
		return ansiNull;
	}

	@Override
	public SizeStrategy getSizeStrategy() {
		return sizeStrategy;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <S extends Statement, T extends JdbcOperation> SqlAstTranslator<T> createTranslator(
					SqlAstTranslationRequest<S, T> request) {
				return new SybaseASELegacySqlAstTranslator<>( request );
			}
		};
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public AggregateSupport getAggregateSupport() {
		return new SybaseASEDialect( getVersion() ).getAggregateSupport();
	}

	@Override
	public PredicateSupport getPredicateSupport() {
		return PredicateSupport.builder( super.getPredicateSupport() )
				.capability( PredicateSupport.Capability.DISTINCT_FROM, getVersion().isSameOrAfter( 16, 3 ) )
				.build();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes( typeContributions, serviceRegistry );

		final JdbcTypeRegistry jdbcTypeRegistry = typeContributions.getTypeConfiguration()
				.getJdbcTypeRegistry();
		jdbcTypeRegistry.addDescriptor( Types.BOOLEAN, TinyIntJdbcType.INSTANCE );
		jdbcTypeRegistry.addDescriptor( Types.TIMESTAMP_WITH_TIMEZONE, TimestampJdbcType.INSTANCE );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String currentDate() {
		return "current_date()";
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String currentTime() {
		return "current_time()";
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String currentTimestamp() {
		return "current_bigdatetime()";
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public long fractionalSecondPrecisionInNanos() {
		// Sybase supports microsecond precision
		// but when we use it we just get numerical
		// overflows from timestamp arithmetic
		return 1_000_000;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType, IntervalType intervalType) {
		return switch ( unit ) {
			case NANOSECOND -> "dateadd(ms,?2/1000000,?3)";
//				return "dateadd(mcs,?2/1000,?3)";
			case NATIVE -> "dateadd(ms,?2,?3)";
//				return "dateadd(mcs,?2,?3)";
			default -> "dateadd(?1,?2,?3)";
		};
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String timestampdiffPattern(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) {
		return switch ( unit ) {
			case NANOSECOND -> "(cast(datediff(ms,?2,?3) as numeric(21))*1000000)";
//				return "(cast(datediff(mcs,?2,?3) as numeric(21))*1000)";
//				}
			case NATIVE -> "cast(datediff(ms,?2,?3) as numeric(21))";
//				return "cast(datediff(mcs,cast(?2 as bigdatetime),cast(?3 as bigdatetime)) as numeric(21))";
			default -> "datediff(?1,?2,?3)";
		};
	}


	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	protected void contributeKeywords(KeywordRegistration registration) {
		super.contributeKeywords( registration );
		registration.registerKeyword( "add" );
		registration.registerKeyword( "all" );
		registration.registerKeyword( "alter" );
		registration.registerKeyword( "and" );
		registration.registerKeyword( "any" );
		registration.registerKeyword( "arith_overflow" );
		registration.registerKeyword( "as" );
		registration.registerKeyword( "asc" );
		registration.registerKeyword( "at" );
		registration.registerKeyword( "authorization" );
		registration.registerKeyword( "avg" );
		registration.registerKeyword( "begin" );
		registration.registerKeyword( "between" );
		registration.registerKeyword( "break" );
		registration.registerKeyword( "browse" );
		registration.registerKeyword( "bulk" );
		registration.registerKeyword( "by" );
		registration.registerKeyword( "cascade" );
		registration.registerKeyword( "case" );
		registration.registerKeyword( "char_convert" );
		registration.registerKeyword( "check" );
		registration.registerKeyword( "checkpoint" );
		registration.registerKeyword( "close" );
		registration.registerKeyword( "clustered" );
		registration.registerKeyword( "coalesce" );
		registration.registerKeyword( "commit" );
		registration.registerKeyword( "compute" );
		registration.registerKeyword( "confirm" );
		registration.registerKeyword( "connect" );
		registration.registerKeyword( "constraint" );
		registration.registerKeyword( "continue" );
		registration.registerKeyword( "controlrow" );
		registration.registerKeyword( "convert" );
		registration.registerKeyword( "count" );
		registration.registerKeyword( "count_big" );
		registration.registerKeyword( "create" );
		registration.registerKeyword( "current" );
		registration.registerKeyword( "cursor" );
		registration.registerKeyword( "database" );
		registration.registerKeyword( "dbcc" );
		registration.registerKeyword( "deallocate" );
		registration.registerKeyword( "declare" );
		registration.registerKeyword( "decrypt" );
		registration.registerKeyword( "default" );
		registration.registerKeyword( "delete" );
		registration.registerKeyword( "desc" );
		registration.registerKeyword( "determnistic" );
		registration.registerKeyword( "disk" );
		registration.registerKeyword( "distinct" );
		registration.registerKeyword( "drop" );
		registration.registerKeyword( "dummy" );
		registration.registerKeyword( "dump" );
		registration.registerKeyword( "else" );
		registration.registerKeyword( "encrypt" );
		registration.registerKeyword( "end" );
		registration.registerKeyword( "endtran" );
		registration.registerKeyword( "errlvl" );
		registration.registerKeyword( "errordata" );
		registration.registerKeyword( "errorexit" );
		registration.registerKeyword( "escape" );
		registration.registerKeyword( "except" );
		registration.registerKeyword( "exclusive" );
		registration.registerKeyword( "exec" );
		registration.registerKeyword( "execute" );
		registration.registerKeyword( "exist" );
		registration.registerKeyword( "exit" );
		registration.registerKeyword( "exp_row_size" );
		registration.registerKeyword( "external" );
		registration.registerKeyword( "fetch" );
		registration.registerKeyword( "fillfactor" );
		registration.registerKeyword( "for" );
		registration.registerKeyword( "foreign" );
		registration.registerKeyword( "from" );
		registration.registerKeyword( "goto" );
		registration.registerKeyword( "grant" );
		registration.registerKeyword( "group" );
		registration.registerKeyword( "having" );
		registration.registerKeyword( "holdlock" );
		registration.registerKeyword( "identity" );
		registration.registerKeyword( "identity_gap" );
		registration.registerKeyword( "identity_start" );
		registration.registerKeyword( "if" );
		registration.registerKeyword( "in" );
		registration.registerKeyword( "index" );
		registration.registerKeyword( "inout" );
		registration.registerKeyword( "insensitive" );
		registration.registerKeyword( "insert" );
		registration.registerKeyword( "install" );
		registration.registerKeyword( "intersect" );
		registration.registerKeyword( "into" );
		registration.registerKeyword( "is" );
		registration.registerKeyword( "isolation" );
		registration.registerKeyword( "jar" );
		registration.registerKeyword( "join" );
		registration.registerKeyword( "key" );
		registration.registerKeyword( "kill" );
		registration.registerKeyword( "level" );
		registration.registerKeyword( "like" );
		registration.registerKeyword( "lineno" );
		registration.registerKeyword( "load" );
		registration.registerKeyword( "lock" );
		registration.registerKeyword( "materialized" );
		registration.registerKeyword( "max" );
		registration.registerKeyword( "max_rows_per_page" );
		registration.registerKeyword( "min" );
		registration.registerKeyword( "mirror" );
		registration.registerKeyword( "mirrorexit" );
		registration.registerKeyword( "modify" );
		registration.registerKeyword( "national" );
		registration.registerKeyword( "new" );
		registration.registerKeyword( "noholdlock" );
		registration.registerKeyword( "nonclustered" );
		registration.registerKeyword( "nonscrollable" );
		registration.registerKeyword( "non_sensitive" );
		registration.registerKeyword( "not" );
		registration.registerKeyword( "null" );
		registration.registerKeyword( "nullif" );
		registration.registerKeyword( "numeric_truncation" );
		registration.registerKeyword( "of" );
		registration.registerKeyword( "off" );
		registration.registerKeyword( "offsets" );
		registration.registerKeyword( "on" );
		registration.registerKeyword( "once" );
		registration.registerKeyword( "online" );
		registration.registerKeyword( "only" );
		registration.registerKeyword( "open" );
		registration.registerKeyword( "option" );
		registration.registerKeyword( "or" );
		registration.registerKeyword( "order" );
		registration.registerKeyword( "out" );
		registration.registerKeyword( "output" );
		registration.registerKeyword( "over" );
		registration.registerKeyword( "artition" );
		registration.registerKeyword( "perm" );
		registration.registerKeyword( "permanent" );
		registration.registerKeyword( "plan" );
		registration.registerKeyword( "prepare" );
		registration.registerKeyword( "primary" );
		registration.registerKeyword( "print" );
		registration.registerKeyword( "privileges" );
		registration.registerKeyword( "proc" );
		registration.registerKeyword( "procedure" );
		registration.registerKeyword( "processexit" );
		registration.registerKeyword( "proxy_table" );
		registration.registerKeyword( "public" );
		registration.registerKeyword( "quiesce" );
		registration.registerKeyword( "raiserror" );
		registration.registerKeyword( "read" );
		registration.registerKeyword( "readpast" );
		registration.registerKeyword( "readtext" );
		registration.registerKeyword( "reconfigure" );
		registration.registerKeyword( "references" );
		registration.registerKeyword( "remove" );
		registration.registerKeyword( "reorg" );
		registration.registerKeyword( "replace" );
		registration.registerKeyword( "replication" );
		registration.registerKeyword( "reservepagegap" );
		registration.registerKeyword( "return" );
		registration.registerKeyword( "returns" );
		registration.registerKeyword( "revoke" );
		registration.registerKeyword( "role" );
		registration.registerKeyword( "rollback" );
		registration.registerKeyword( "rowcount" );
		registration.registerKeyword( "rows" );
		registration.registerKeyword( "rule" );
		registration.registerKeyword( "save" );
		registration.registerKeyword( "schema" );
		registration.registerKeyword( "scroll" );
		registration.registerKeyword( "scrollable" );
		registration.registerKeyword( "select" );
		registration.registerKeyword( "semi_sensitive" );
		registration.registerKeyword( "set" );
		registration.registerKeyword( "setuser" );
		registration.registerKeyword( "shared" );
		registration.registerKeyword( "shutdown" );
		registration.registerKeyword( "some" );
		registration.registerKeyword( "statistics" );
		registration.registerKeyword( "stringsize" );
		registration.registerKeyword( "stripe" );
		registration.registerKeyword( "sum" );
		registration.registerKeyword( "syb_identity" );
		registration.registerKeyword( "syb_restree" );
		registration.registerKeyword( "syb_terminate" );
		registration.registerKeyword( "top" );
		registration.registerKeyword( "table" );
		registration.registerKeyword( "temp" );
		registration.registerKeyword( "temporary" );
		registration.registerKeyword( "textsize" );
		registration.registerKeyword( "to" );
		registration.registerKeyword( "tracefile" );
		registration.registerKeyword( "tran" );
		registration.registerKeyword( "transaction" );
		registration.registerKeyword( "trigger" );
		registration.registerKeyword( "truncate" );
		registration.registerKeyword( "tsequal" );
		registration.registerKeyword( "union" );
		registration.registerKeyword( "unique" );
		registration.registerKeyword( "unpartition" );
		registration.registerKeyword( "update" );
		registration.registerKeyword( "use" );
		registration.registerKeyword( "user" );
		registration.registerKeyword( "user_option" );
		registration.registerKeyword( "using" );
		registration.registerKeyword( "values" );
		registration.registerKeyword( "varying" );
		registration.registerKeyword( "view" );
		registration.registerKeyword( "waitfor" );
		registration.registerKeyword( "when" );
		registration.registerKeyword( "where" );
		registration.registerKeyword( "while" );
		registration.registerKeyword( "with" );
		registration.registerKeyword( "work" );
		registration.registerKeyword( "writetext" );
		registration.registerKeyword( "xmlextract" );
		registration.registerKeyword( "xmlparse" );
		registration.registerKeyword( "xmltest" );
		registration.registerKeyword( "xmlvalidate" );
	}

// Overridden informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


	@Override
	@SPI({ USE, IMPLEMENT })
	public boolean supportsOnDeleteAction(org.hibernate.annotations.OnDeleteAction action) {
		return action == org.hibernate.annotations.OnDeleteAction.NO_ACTION;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public int getMaxAliasLength() {
		return 30;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public int getMaxIdentifierLength() {
		return 255;
		}

	@Override
	public LockingSupport getLockingSupport() {
		return StandardLockingSupports.legacySybaseAse();
	}

	@Override
	public SubquerySupport getSubquerySupport() {
		return SubquerySupport.builder( super.getSubquerySupport() )
				.feature( SubquerySupport.Feature.ORDER_BY, false )
				.build();
	}

	@Override
	public SetOperationSupport getSetOperationSupport() {
		// At least not according to HHH-3637
		return SetOperationSupport.builder()
				.operator( SetOperator.INTERSECT, false )
				.operator( SetOperator.INTERSECT_ALL, false )
				.operator( SetOperator.EXCEPT_ALL, false )
				.capability( SetOperationSupport.Capability.UNION_IN_SUBQUERY, false )
				.build();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String tableCreationOptions() {
		//HHH-7298 I don't know if this would break something or cause some side affects
		//but it is required to use 'select for update'
		return getVersion().isBefore( 15, 7 ) ? super.tableCreationOptions() : " lock datarows";
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
	@SPI({ IMPLEMENT, SUPPLY })
	public ViolatedConstraintNameExtractor getViolatedConstraintNameExtractor() {
		return EXTRACTOR;
	}

	/**
	 * Constraint-name extractor for Sybase ASE constraint violation exceptions.
	 * Orginally contributed by Denny Bartelt.
	 */
	private static final ViolatedConstraintNameExtractor EXTRACTOR =
			new TemplatedViolatedConstraintNameExtractor( sqle -> {
				final String sqlState = extractSqlState( sqle );
				final int errorCode = extractErrorCode( sqle );
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
	@SPI({ IMPLEMENT, SUPPLY })
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		if ( getVersion().isBefore( 15, 7 ) ) {
			return null;
		}
		return (sqlException, message, sql) -> {
			final String sqlState = extractSqlState( sqlException );
			final int errorCode = extractErrorCode( sqlException );
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
		if ( getVersion().isBefore( 12, 5 ) ) {
			//support for SELECT TOP was introduced in Sybase ASE 12.5.3
			return super.getLimitHandler();
		}
		return new TopLimitHandler(false);
	}

	@Override
	public SingleRowTableSupport getSingleRowTableSupport() {
		return SingleRowTableSupport.builder( super.getSingleRowTableSupport() )
				.tableExpression( "(select 1 c1)" )
				.build();
	}

	@Override
	public ValuesListSupport getValuesListSupport() {
		return ValuesListSupport.NONE;
	}

	@Override
	public MutationSyntaxSupport getMutationSyntaxSupport() {
		return MutationSyntaxSupport.builder()
				.capability( MutationKind.UPDATE, MutationSyntaxCapability.FROM_CLAUSE )
				.capability( MutationKind.DELETE, MutationSyntaxCapability.JOIN )
				.build();
	}

	@Override
	public boolean supportsCrossJoin() {
		return false;
	}
}

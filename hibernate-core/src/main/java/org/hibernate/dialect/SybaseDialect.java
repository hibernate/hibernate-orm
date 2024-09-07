/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.function.CountFunction;
import org.hibernate.dialect.function.IntegralTimestampaddFunction;
import org.hibernate.dialect.function.SybaseTruncFunction;
import org.hibernate.dialect.identity.AbstractTransactSQLIdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.identity.SybaseJconnIdentityColumnSupport;
import org.hibernate.dialect.unique.SkipNullableUniqueDelegate;
import org.hibernate.dialect.unique.UniqueDelegate;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.env.spi.IdentifierCaseStrategy;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelperBuilder;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.procedure.internal.JTDSCallableStatementSupport;
import org.hibernate.procedure.internal.SybaseCallableStatementSupport;
import org.hibernate.procedure.spi.CallableStatementSupport;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.sqm.CastType;
import org.hibernate.query.sqm.IntervalType;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.sql.SqmTranslator;
import org.hibernate.query.sqm.sql.SqmTranslatorFactory;
import org.hibernate.query.sqm.sql.StandardSqmTranslatorFactory;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.ast.tree.select.SelectStatement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.type.JavaObjectType;
import org.hibernate.type.NullType;
import org.hibernate.type.descriptor.jdbc.BlobJdbcType;
import org.hibernate.type.descriptor.jdbc.ClobJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.ObjectNullAsBinaryTypeJdbcType;
import org.hibernate.type.descriptor.jdbc.TinyIntAsSmallIntJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import jakarta.persistence.TemporalType;

import static org.hibernate.type.descriptor.DateTimeUtils.appendAsDate;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsLocalTime;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsTime;
import static org.hibernate.type.descriptor.DateTimeUtils.appendAsTimestampWithMillis;


/**
 * Superclass for all Sybase dialects.
 *
 * @author Brett Meyer
 */
public class SybaseDialect extends AbstractTransactSQLDialect {

	private static final DatabaseVersion MINIMUM_VERSION = DatabaseVersion.make( 16, 0 );

	//All Sybase dialects share an IN list size limit.
	private static final int IN_LIST_SIZE_LIMIT = 250000;

	private static final int PARAM_COUNT_LIMIT = 2000;

	private final UniqueDelegate uniqueDelegate = new SkipNullableUniqueDelegate(this);
	private final SybaseDriverKind driverKind;

	@Deprecated(forRemoval = true)
	protected final boolean jtdsDriver;

	public SybaseDialect() {
		this( MINIMUM_VERSION );
	}

	public SybaseDialect(DatabaseVersion version) {
		super(version);
		this.driverKind = SybaseDriverKind.OTHER;
		this.jtdsDriver = true;
	}

	public SybaseDialect(DialectResolutionInfo info) {
		super(info);
		this.driverKind = SybaseDriverKind.determineKind( info );
		this.jtdsDriver = driverKind == SybaseDriverKind.JTDS;
	}

	@Override
	protected DatabaseVersion getMinimumSupportedVersion() {
		return MINIMUM_VERSION;
	}

	public SybaseDriverKind getDriverKind() {
		return driverKind;
	}

	@Override
	public JdbcType resolveSqlTypeDescriptor(
			String columnTypeName,
			int jdbcTypeCode,
			int precision,
			int scale,
			JdbcTypeRegistry jdbcTypeRegistry) {
		switch ( jdbcTypeCode ) {
			case Types.NUMERIC:
			case Types.DECIMAL:
				if ( precision == 19 && scale == 0 ) {
					return jdbcTypeRegistry.getDescriptor( Types.BIGINT );
				}
		}
		return super.resolveSqlTypeDescriptor(
				columnTypeName,
				jdbcTypeCode,
				precision,
				scale,
				jdbcTypeRegistry
		);
	}

	@Override
	public int resolveSqlTypeLength(
			String columnTypeName,
			int jdbcTypeCode,
			int precision,
			int scale,
			int displaySize) {
		// Sybase jconnect driver reports the "actual" precision in the display size
		switch ( jdbcTypeCode ) {
			case Types.CHAR:
			case Types.VARCHAR:
			case Types.REAL:
			case Types.DOUBLE:
				return displaySize;
		}
		return super.resolveSqlTypeLength( columnTypeName, jdbcTypeCode, precision, scale, displaySize );
	}

	@Override
	public SqmTranslatorFactory getSqmTranslatorFactory() {
		return new StandardSqmTranslatorFactory() {
			@Override
			public SqmTranslator<SelectStatement> createSelectTranslator(
					SqmSelectStatement<?> sqmSelectStatement,
					QueryOptions queryOptions,
					DomainParameterXref domainParameterXref,
					QueryParameterBindings domainParameterBindings,
					LoadQueryInfluencers loadQueryInfluencers,
					SqlAstCreationContext creationContext,
					boolean deduplicateSelectionItems) {
				return new SybaseSqmToSqlAstConverter<>(
						sqmSelectStatement,
						queryOptions,
						domainParameterXref,
						domainParameterBindings,
						loadQueryInfluencers,
						creationContext,
						deduplicateSelectionItems
				);
			}
		};
	}

	@Override
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(
					SessionFactoryImplementor sessionFactory, Statement statement) {
				return new SybaseSqlAstTranslator<>( sessionFactory, statement );
			}
		};
	}

	@Override
	public boolean supportsNullPrecedence() {
		return false;
	}

	@Override
	public int getInExpressionCountLimit() {
		return IN_LIST_SIZE_LIMIT;
	}

	@Override
	public int getParameterCountLimit() {
		return PARAM_COUNT_LIMIT;
	}

	@Override
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes(typeContributions, serviceRegistry);
		final JdbcTypeRegistry jdbcTypeRegistry = typeContributions.getTypeConfiguration()
				.getJdbcTypeRegistry();
		if ( driverKind == SybaseDriverKind.JTDS ) {
			jdbcTypeRegistry.addDescriptor( Types.TINYINT, TinyIntAsSmallIntJdbcType.INSTANCE );

			// The jTDS driver doesn't support the JDBC4 signatures using 'long length' for stream bindings
			jdbcTypeRegistry.addDescriptor( Types.CLOB, ClobJdbcType.CLOB_BINDING );

			// The jTDS driver doesn't support nationalized types
			jdbcTypeRegistry.addDescriptor( Types.NCLOB, ClobJdbcType.CLOB_BINDING );
			jdbcTypeRegistry.addDescriptor( Types.NVARCHAR, ClobJdbcType.CLOB_BINDING );
		}
		else {
			// jConnect driver only conditionally supports getClob/getNClob depending on a server setting. See
			//		- https://help.sap.com/doc/e3cb6844decf441e85e4670e1cf48c9b/16.0.3.6/en-US/SAP_jConnect_Programmers_Reference_en.pdf
			// 		- https://infocenter.sybase.com/help/index.jsp?topic=/com.sybase.infocenter.dc20155.1570/html/OS_SDK_nf/CIHJFDDH.htm
			//		- HHH-7889
			jdbcTypeRegistry.addDescriptor( Types.CLOB, ClobJdbcType.STREAM_BINDING_EXTRACTING );
			jdbcTypeRegistry.addDescriptor( Types.NCLOB, ClobJdbcType.STREAM_BINDING_EXTRACTING );
		}

		jdbcTypeRegistry.addDescriptor( Types.BLOB, BlobJdbcType.PRIMITIVE_ARRAY_BINDING );

		// Sybase requires a custom binder for binding untyped nulls with the NULL type
		typeContributions.contributeJdbcType( ObjectNullAsBinaryTypeJdbcType.INSTANCE );

		// Until we remove StandardBasicTypes, we have to keep this
		typeContributions.contributeType(
				new JavaObjectType(
						ObjectNullAsBinaryTypeJdbcType.INSTANCE,
						typeContributions.getTypeConfiguration()
								.getJavaTypeRegistry()
								.getDescriptor( Object.class )
				)
		);
		typeContributions.contributeType(
				new NullType(
						ObjectNullAsBinaryTypeJdbcType.INSTANCE,
						typeContributions.getTypeConfiguration()
								.getJavaTypeRegistry()
								.getDescriptor( Object.class )
				)
		);
	}

	@Override
	public NationalizationSupport getNationalizationSupport() {
		// At least the jTDS driver doesn't support this
		return driverKind == SybaseDriverKind.JTDS ? NationalizationSupport.IMPLICIT : super.getNationalizationSupport();
	}

	@Override
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry(functionContributions);

		CommonFunctionFactory functionFactory = new CommonFunctionFactory(functionContributions);

		functionFactory.stddev();
		functionFactory.variance();
		functionFactory.stddevPopSamp_stdevp();
		functionFactory.varPopSamp_varp();
		functionFactory.stddevPopSamp();
		functionFactory.varPopSamp();
		functionFactory.round_round();

		// For SQL-Server we need to cast certain arguments to varchar(16384) to be able to concat them
		functionContributions.getFunctionRegistry().register(
				"count",
				new CountFunction(
						this,
						functionContributions.getTypeConfiguration(),
						SqlAstNodeRenderingMode.DEFAULT,
						"count_big",
						"+",
						"varchar(16384)",
						false
				)
		);

		// AVG by default uses the input type, so we possibly need to cast the argument type, hence a special function
		functionFactory.avg_castingNonDoubleArguments( this, SqlAstNodeRenderingMode.DEFAULT );

		//this doesn't work 100% on earlier versions of Sybase
		//which were missing the third parameter in charindex()
		//TODO: we could emulate it with substring() like in Postgres
		functionFactory.locate_charindex();

		functionFactory.replace_strReplace();
		functionFactory.everyAny_minMaxCase();
		functionFactory.octetLength_pattern( "datalength(?1)" );
		functionFactory.bitLength_pattern( "datalength(?1)*8" );

		functionContributions.getFunctionRegistry().register( "timestampadd",
				new IntegralTimestampaddFunction( this, functionContributions.getTypeConfiguration() ) );
		functionContributions.getFunctionRegistry().register(
				"trunc",
				new SybaseTruncFunction( functionContributions.getTypeConfiguration() )
		);
		functionContributions.getFunctionRegistry().registerAlternateKey( "truncate", "trunc" );
	}

	@Override
	public String getNullColumnString() {
		return " null";
	}

	@Override
	public boolean canCreateSchema() {
		// As far as I can tell, it does not
		return false;
	}

	@Override
	public String getCurrentSchemaCommand() {
		return "select user_name()";
	}

	@Override
	public int getMaxIdentifierLength() {
		return 128;
	}

	@Override
	public String castPattern(CastType from, CastType to) {
		if ( to == CastType.STRING ) {
			switch ( from ) {
				case DATE:
					return "substring(convert(varchar,?1,23),1,10)";
				case TIME:
					return "convert(varchar,?1,8)";
				case TIMESTAMP:
					return "convert(varchar,?1,140)";
			}
		}
		return super.castPattern( from, to );
	}

	/* Something odd is going on with the jConnect driver when using JDBC escape syntax, so let's use native functions */

	@Override
	public void appendDateTimeLiteral(
			SqlAppender appender,
			TemporalAccessor temporalAccessor,
			TemporalType precision,
			TimeZone jdbcTimeZone) {
		switch ( precision ) {
			case DATE:
				appender.appendSql( "convert(date,'" );
				appendAsDate( appender, temporalAccessor );
				appender.appendSql( "',140)" );
				break;
			case TIME:
				appender.appendSql( "convert(time,'" );
				appendAsTime( appender, temporalAccessor, supportsTemporalLiteralOffset(), jdbcTimeZone );
				appender.appendSql( "',8)" );
				break;
			case TIMESTAMP:
				appender.appendSql( "convert(datetime,'" );
				appendAsTimestampWithMillis( appender, temporalAccessor, supportsTemporalLiteralOffset(), jdbcTimeZone );
				appender.appendSql( "',140)" );
				break;
			default:
				throw new IllegalArgumentException();
		}
	}

	@Override
	public void appendDateTimeLiteral(SqlAppender appender, Date date, TemporalType precision, TimeZone jdbcTimeZone) {
		switch ( precision ) {
			case DATE:
				appender.appendSql( "convert(date,'" );
				appendAsDate( appender, date );
				appender.appendSql( "',140)" );
				break;
			case TIME:
				appender.appendSql( "convert(time,'" );
				appendAsLocalTime( appender, date );
				appender.appendSql( "',8)" );
				break;
			case TIMESTAMP:
				appender.appendSql( "convert(datetime,'" );
				appendAsTimestampWithMillis( appender, date, jdbcTimeZone );
				appender.appendSql( "',140)" );
				break;
			default:
				throw new IllegalArgumentException();
		}
	}

	@Override
	public void appendDateTimeLiteral(
			SqlAppender appender,
			Calendar calendar,
			TemporalType precision,
			TimeZone jdbcTimeZone) {
		switch ( precision ) {
			case DATE:
				appender.appendSql( "convert(date,'" );
				appendAsDate( appender, calendar );
				appender.appendSql( "',140)" );
				break;
			case TIME:
				appender.appendSql( "convert(time,'" );
				appendAsLocalTime( appender, calendar );
				appender.appendSql( "',8)" );
				break;
			case TIMESTAMP:
				appender.appendSql( "convert(datetime,'" );
				appendAsTimestampWithMillis( appender, calendar, jdbcTimeZone );
				appender.appendSql( "',140)" );
				break;
			default:
				throw new IllegalArgumentException();
		}
	}

	@Override
	public String translateExtractField(TemporalUnit unit) {
		switch ( unit ) {
			case WEEK: return "calweekofyear"; //the ISO week number I think
			default: return super.translateExtractField(unit);
		}
	}

	@Override
	public String extractPattern(TemporalUnit unit) {
		if ( unit == TemporalUnit.EPOCH ) {
			return "datediff(second, '1970-01-01 00:00:00', ?2)";
		}
		else {
			//TODO!!
			return "datepart(?1,?2)";
		}
	}

	@Override
	public boolean supportsFractionalTimestampArithmetic() {
		return false;
	}

	@Override
	public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType, IntervalType intervalType) {
		//TODO!!
		return "dateadd(?1,?2,?3)";
	}

	@Override
	public String timestampdiffPattern(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) {
		//TODO!!
		return "datediff(?1,?2,?3)";
	}

	@Override
	public void appendDatetimeFormat(SqlAppender appender, String format) {
		throw new UnsupportedOperationException( "format() function not supported on Sybase");
	}

	@Override
	public boolean supportsStandardCurrentTimestampFunction() {
		return false;
	}

	@Override
	public IdentifierHelper buildIdentifierHelper(IdentifierHelperBuilder builder, DatabaseMetaData dbMetaData)
			throws SQLException {
		// Default to MIXED because the jconn driver doesn't seem to report anything useful
		builder.setUnquotedCaseStrategy( IdentifierCaseStrategy.MIXED );
		if ( dbMetaData == null ) {
			builder.setQuotedCaseStrategy( IdentifierCaseStrategy.MIXED );
		}

		return super.buildIdentifierHelper( builder, dbMetaData );
	}

	@Override
	public NameQualifierSupport getNameQualifierSupport() {
		return NameQualifierSupport.BOTH;
	}

	@Override
	public UniqueDelegate getUniqueDelegate() {
		return uniqueDelegate;
	}

	@Override
	public CallableStatementSupport getCallableStatementSupport() {
		return driverKind == SybaseDriverKind.JTDS ?
				JTDSCallableStatementSupport.INSTANCE :
				SybaseCallableStatementSupport.INSTANCE;
	}

	@Override
	public boolean supportsNamedParameters(DatabaseMetaData databaseMetaData) throws SQLException {
		// Only the jTDS driver supports named parameters properly
		return driverKind == SybaseDriverKind.JTDS && super.supportsNamedParameters( databaseMetaData );
	}

	@Override
	public String getAlterColumnTypeString(String columnName, String columnType, String columnDefinition) {
		return "modify " + columnName + " " + columnType;
	}

	@Override
	public boolean supportsAlterColumnType() {
		return true;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return driverKind == SybaseDriverKind.JTDS
				? AbstractTransactSQLIdentityColumnSupport.INSTANCE
				: SybaseJconnIdentityColumnSupport.INSTANCE;
	}

	@Override
	public DmlTargetColumnQualifierSupport getDmlTargetColumnQualifierSupport() {
		return DmlTargetColumnQualifierSupport.TABLE_ALIAS;
	}

	@Override
	public boolean supportsFromClauseInUpdate() {
		return true;
	}
}

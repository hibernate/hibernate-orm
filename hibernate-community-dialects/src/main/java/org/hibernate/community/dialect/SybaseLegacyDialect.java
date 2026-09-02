/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.temporaltype.spi.TemporalOperationSupport;
import org.hibernate.dialect.temporaltype.spi.TemporalOperationSupports;

import org.hibernate.dialect.temporaltype.spi.TemporalFormatSupport;

import org.hibernate.dialect.temporaltype.spi.CurrentTemporalSupport;

import org.hibernate.SPI;
import static org.hibernate.SPI.Role.USE;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;

import org.hibernate.dialect.sql.ast.spi.CteSupport;
import org.hibernate.dialect.sql.ast.spi.NullOrderingSupport;
import org.hibernate.dialect.sql.ast.spi.MutationKind;
import org.hibernate.dialect.sql.ast.spi.MutationSyntaxCapability;
import org.hibernate.dialect.sql.ast.spi.MutationSyntaxSupport;
import org.hibernate.dialect.sql.ast.spi.RowValueSupport;
import org.hibernate.dialect.sql.ast.spi.ValuesListSupport;

import jakarta.persistence.TemporalType;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.AbstractTransactSQLDialect;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.sql.ast.spi.DmlTargetColumnQualifierSupport;
import org.hibernate.dialect.type.spi.NationalizationSupport;
import org.hibernate.dialect.jdbc.spi.SybaseDriverKind;
import org.hibernate.dialect.jdbc.spi.JdbcMetadataOverrides;
import org.hibernate.dialect.jdbc.spi.ParameterLimits;
import org.hibernate.dialect.type.spi.SybaseJdbcTypes;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.function.CountFunction;
import org.hibernate.dialect.function.IntegralTimestampaddFunction;
import org.hibernate.dialect.function.SybaseTruncFunction;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.lock.spi.StandardLockingSupports;
import org.hibernate.dialect.namespace.spi.NamespaceSupport;
import org.hibernate.dialect.namespace.spi.NamespaceSupports;
import org.hibernate.dialect.schema.spi.ColumnDefinitionRequest;
import org.hibernate.dialect.unique.spi.UniqueDelegates;
import org.hibernate.dialect.unique.spi.UniqueDelegate;
import org.hibernate.dialect.type.spi.SizeStrategy;
import org.hibernate.dialect.type.spi.StandardSizeStrategy;
import org.hibernate.engine.jdbc.Size;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.env.spi.IdentifierCaseStrategy;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.dialect.identifier.spi.IdentifierHelperBuildRequest;
import org.hibernate.engine.jdbc.env.spi.NameQualifierSupport;
import org.hibernate.procedure.spi.CallableStatementSupport;
import org.hibernate.procedure.spi.CallableStatementSupports;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.query.sqm.CastType;
import org.hibernate.dialect.temporaltype.spi.IntervalType;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.spi.translation.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslatorFactory;
import org.hibernate.dialect.sql.ast.spi.SyntheticTableGroupSupport;
import org.hibernate.sql.spi.SqlAppender;
import org.hibernate.dialect.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.type.JavaObjectType;
import org.hibernate.type.NullType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BlobJdbcType;
import org.hibernate.type.descriptor.jdbc.ClobJdbcType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.NClobJdbcType;
import org.hibernate.type.descriptor.jdbc.ObjectNullAsBinaryTypeJdbcType;
import org.hibernate.type.descriptor.jdbc.TinyIntAsSmallIntJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import java.sql.Types;
import java.time.temporal.TemporalAccessor;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsDate;
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsLocalTime;
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsTime;
import static org.hibernate.dialect.literal.spi.StandardDateTimeLiteralRendering.appendAsTimestampWithMillis;


/**
 * Superclass for all Sybase dialects.
 *
 * @author Brett Meyer
 */
public class SybaseLegacyDialect extends AbstractTransactSQLDialect implements CurrentTemporalSupport, TemporalFormatSupport, TemporalOperationSupport {

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public TemporalOperationSupport getTemporalOperationSupport() {
		return this;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public TemporalFormatSupport getTemporalFormatSupport() {
		return this;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public CurrentTemporalSupport getCurrentTemporalSupport() {
		return this;
	}

	//All Sybase dialects share an IN list size limit.
	private static final int PARAM_LIST_SIZE_LIMIT = 250000;
	private final UniqueDelegate uniqueDelegate = UniqueDelegates.skipNullable( this );
	private final SybaseDriverKind driverKind;
	private final JdbcMetadataOverrides jdbcMetadataOverrides;

	@Deprecated(forRemoval = true)
	protected final boolean jtdsDriver;

	private final SizeStrategy sizeStrategy = new StandardSizeStrategy( this ) {
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
							length == null ? getTypeSizingProfile().defaultLobLength() : length
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

	public SybaseLegacyDialect() {
		this( DatabaseVersion.make( 11, 0 ) );
	}

	public SybaseLegacyDialect(DatabaseVersion version) {
		super(version);
		this.driverKind = SybaseDriverKind.OTHER;
		this.jdbcMetadataOverrides = jdbcMetadataOverrides( driverKind );
		this.jtdsDriver = true;
	}

	public SybaseLegacyDialect(DialectResolutionInfo info) {
		super(info);
		this.driverKind = SybaseDriverKind.determineKind( info );
		this.jdbcMetadataOverrides = jdbcMetadataOverrides( driverKind );
		this.jtdsDriver = driverKind == SybaseDriverKind.JTDS;
	}

	public SybaseDriverKind getDriverKind() {
		return driverKind;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
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
	@SPI({ USE, IMPLEMENT })
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
	public SyntheticTableGroupSupport getSyntheticTableGroupSupport() {
		return SyntheticTableGroupSupport.SELECT_ONE_FOR_LITERALS;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <S extends Statement, T extends JdbcOperation> SqlAstTranslator<T> createTranslator(
					SqlAstTranslationRequest<S, T> request) {
				return new SybaseLegacySqlAstTranslator<>( request );
			}
		};
	}

	@Override
	public SizeStrategy getSizeStrategy() {
		return sizeStrategy;
	}

	@Override
	public NullOrderingSupport getNullOrderingSupport() {
		return NullOrderingSupport.builder( super.getNullOrderingSupport() )
				.capability( NullOrderingSupport.Capability.NULLS_FIRST_LAST, false )
				.build();
	}

	@Override
	public ParameterLimits getParameterLimits() {
		return ParameterLimits.of( PARAM_LIST_SIZE_LIMIT );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes(typeContributions, serviceRegistry);
		final JdbcTypeRegistry jdbcTypeRegistry = typeContributions.getTypeConfiguration()
				.getJdbcTypeRegistry();
		if ( driverKind == SybaseDriverKind.JTDS ) {
			jdbcTypeRegistry.addDescriptor( TinyIntAsSmallIntJdbcType.INSTANCE );

			// The jTDS driver doesn't support the JDBC4 signatures using 'long length' for stream bindings
			jdbcTypeRegistry.addDescriptor( ClobJdbcType.CLOB_BINDING );

			// Need to register specialized JdbcType instances for jTDS because it throws an AbstractMethodError
			// when invoking nationalized methods and requires binding through UTF-16LE bytes
			jdbcTypeRegistry.addDescriptor( SybaseJdbcTypes.jtdsNClob() );
			jdbcTypeRegistry.addDescriptor( SybaseJdbcTypes.jtdsNChar() );
			jdbcTypeRegistry.addDescriptor( SybaseJdbcTypes.jtdsNVarchar() );
			jdbcTypeRegistry.addDescriptor( SybaseJdbcTypes.jtdsLongNVarchar() );
			jdbcTypeRegistry.addDescriptor( SybaseJdbcTypes.jtdsJson() );
			jdbcTypeRegistry.addDescriptor( SybaseJdbcTypes.jtdsXml() );
			jdbcTypeRegistry.addTypeConstructor( SybaseJdbcTypes.jtdsJsonArrayConstructor() );
			jdbcTypeRegistry.addTypeConstructor( SybaseJdbcTypes.jtdsXmlArrayConstructor() );
		}
		else {
			// jConnect driver only conditionally supports getClob/getNClob depending on a server setting. See
			//		- https://help.sap.com/doc/e3cb6844decf441e85e4670e1cf48c9b/16.0.3.6/en-US/SAP_jConnect_Programmers_Reference_en.pdf
			// 		- https://infocenter.sybase.com/help/index.jsp?topic=/com.sybase.infocenter.dc20155.1570/html/OS_SDK_nf/CIHJFDDH.htm
			//		- HHH-7889
			jdbcTypeRegistry.addDescriptor( ClobJdbcType.STREAM_BINDING_EXTRACTING );
			jdbcTypeRegistry.addDescriptor( NClobJdbcType.STREAM_BINDING_EXTRACTING );
		}

		jdbcTypeRegistry.addDescriptor( BlobJdbcType.PRIMITIVE_ARRAY_BINDING );

		// Sybase requires a custom binder for binding untyped nulls with the NULL type
		typeContributions.contributeJdbcType( ObjectNullAsBinaryTypeJdbcType.INSTANCE );

		// Until we remove StandardBasicTypes, we have to keep this
		typeContributions.contributeType(
				new JavaObjectType(
						ObjectNullAsBinaryTypeJdbcType.INSTANCE,
						typeContributions.getTypeConfiguration()
								.getJavaTypeRegistry()
								.resolveDescriptor( Object.class )
				)
		);
		typeContributions.contributeType(
				new NullType(
						ObjectNullAsBinaryTypeJdbcType.INSTANCE,
						typeContributions.getTypeConfiguration()
								.getJavaTypeRegistry()
								.resolveDescriptor( Object.class )
				)
		);
	}

	@Override
	public LockingSupport getLockingSupport() {
		return StandardLockingSupports.sybase();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public NationalizationSupport getNationalizationSupport() {
		// At least the jTDS driver doesn't support this
		return driverKind == SybaseDriverKind.JTDS ? NationalizationSupport.IMPLICIT : super.getNationalizationSupport();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
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
	@SPI({ USE, IMPLEMENT })
	public void appendDefinition(SqlAppender appender, ColumnDefinitionRequest request) {
		super.appendDefinition( appender, request );
		if ( request.nullable() ) {
			appender.appendSql( " null" );
		}
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public NamespaceSupport getNamespaceSupport() {
		return NamespaceSupports.none();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public int getMaxIdentifierLength() {
		return 128;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
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
	@SPI({ USE, IMPLEMENT })
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
				appendAsTime( appender, temporalAccessor, getTemporalValueSemantics().supportsLiteralOffset(), jdbcTimeZone );
				appender.appendSql( "',8)" );
				break;
			case TIMESTAMP:
				appender.appendSql( "convert(datetime,'" );
				appendAsTimestampWithMillis( appender, temporalAccessor, getTemporalValueSemantics().supportsLiteralOffset(), jdbcTimeZone );
				appender.appendSql( "',140)" );
				break;
			default:
				throw new IllegalArgumentException();
		}
	}

	@Override
	@SPI({ USE, IMPLEMENT })
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
	@SPI({ USE, IMPLEMENT })
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
	@SPI({ USE, IMPLEMENT })
	public String translateExtractField(TemporalUnit unit) {
		switch ( unit ) {
			case WEEK: return "calweekofyear"; //the ISO week number I think
			default: return TemporalOperationSupports.standard().translateExtractField(unit);
		}
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String extractPattern(TemporalUnit unit) {
		//TODO!!
		return "datepart(?1,?2)";
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType, IntervalType intervalType) {
		//TODO!!
		return "dateadd(?1,?2,?3)";
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String timestampdiffPattern(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) {
		//TODO!!
		return "datediff(?1,?2,?3)";
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendFormat(SqlAppender appender, String format) {
		throw new UnsupportedOperationException( "format() function not supported on Sybase");
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String currentTimestamp() {
		return "getdate()";
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public IdentifierHelper buildIdentifierHelper(IdentifierHelperBuildRequest request) {
		final var builder = request.builder();
		if ( !request.jdbcMetadata().isJdbcMetadataAccessible() ) {
			builder.setUnquotedCaseStrategy( IdentifierCaseStrategy.MIXED );
			builder.setQuotedCaseStrategy( IdentifierCaseStrategy.MIXED );
		}

		return super.buildIdentifierHelper( request );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public NameQualifierSupport getNameQualifierSupport() {
		// No support for schemas: https://userapps.support.sap.com/sap/support/knowledge/en/2591730
		// Authorization schemas seem to be something different: https://infocenter.sybase.com/help/index.jsp?topic=/com.sybase.infocenter.dc36272.1550/html/commands/X48762.htm
		return NameQualifierSupport.CATALOG;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public UniqueDelegate getUniqueDelegate() {
		return uniqueDelegate;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public CallableStatementSupport getCallableStatementSupport() {
		return driverKind == SybaseDriverKind.JTDS
				? CallableStatementSupports.jtds()
				: CallableStatementSupports.sybase();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public JdbcMetadataOverrides getJdbcMetadataOverrides() {
		return jdbcMetadataOverrides;
	}

	private static JdbcMetadataOverrides jdbcMetadataOverrides(SybaseDriverKind driverKind) {
		return driverKind == SybaseDriverKind.JTDS
				? JdbcMetadataOverrides.STANDARD
				: JdbcMetadataOverrides.builder()
						.namedParameterSupport( JdbcMetadataOverrides.SupportOverride.UNSUPPORTED )
						.build();
	}

	@Override
	@SPI({ USE, IMPLEMENT, SUPPLY })
	public DmlTargetColumnQualifierSupport getDmlTargetColumnQualifierSupport() {
		return DmlTargetColumnQualifierSupport.TABLE_ALIAS;
	}

	@Override
	public ValuesListSupport getValuesListSupport() {
		return ValuesListSupport.NONE;
	}

	@Override
	public MutationSyntaxSupport getMutationSyntaxSupport() {
		return MutationSyntaxSupport.of( MutationKind.UPDATE, MutationSyntaxCapability.FROM_CLAUSE );
	}

	@Override
	public RowValueSupport getRowValueSupport() {
		return RowValueSupport.NONE;
	}

	@Override
	public CteSupport getCteSupport() {
		return CteSupport.NONE;
	}

}

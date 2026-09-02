/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.schema.spi.ConstraintDropMode;
import org.hibernate.dialect.schema.spi.SchemaDropSupport;

import org.hibernate.dialect.temporaltype.spi.TemporalOperationSupport;
import org.hibernate.dialect.temporaltype.spi.TemporalOperationSupports;

import org.hibernate.dialect.temporaltype.spi.TemporalFormatSupport;

import org.hibernate.dialect.temporaltype.spi.CurrentTemporalSupport;

import org.hibernate.SPI;
import static org.hibernate.SPI.Role.USE;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import org.hibernate.dialect.type.spi.DdlTypeBuilder;

import org.hibernate.dialect.type.spi.StandardDdlTypes;

import org.hibernate.dialect.type.spi.TypeSizingProfile;

import jakarta.persistence.TemporalType;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.community.dialect.identity.internal.MimerSQLIdentityColumnSupport;
import org.hibernate.community.dialect.sequence.MimerSequenceSupport;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.lob.spi.LobSupport;
import org.hibernate.dialect.lob.spi.LobSupports;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.identity.spi.IdentityColumnSupport;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.lock.spi.StandardLockingSupports;
import org.hibernate.dialect.pagination.spi.LimitHandler;
import org.hibernate.dialect.pagination.spi.OffsetFetchLimitHandler;
import org.hibernate.dialect.sequence.spi.SequenceSupport;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.query.SemanticException;
import org.hibernate.query.common.TemporalUnit;
import org.hibernate.dialect.temporaltype.spi.IntervalType;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslatorFactory;
import org.hibernate.dialect.sql.ast.spi.RowValueSupport;
import org.hibernate.dialect.sql.ast.spi.SingleRowTableSupport;
import org.hibernate.dialect.sql.ast.spi.SubquerySupport;
import org.hibernate.sql.spi.SqlAppender;
import org.hibernate.dialect.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractors;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;

import static org.hibernate.dialect.SimpleDatabaseVersion.ZERO_VERSION;
import static org.hibernate.type.SqlTypes.BLOB;
import static org.hibernate.type.SqlTypes.CHAR;
import static org.hibernate.type.SqlTypes.CLOB;
import static org.hibernate.type.SqlTypes.LONG32NVARCHAR;
import static org.hibernate.type.SqlTypes.LONG32VARCHAR;
import static org.hibernate.type.SqlTypes.NCHAR;
import static org.hibernate.type.SqlTypes.NCLOB;
import static org.hibernate.type.SqlTypes.NVARCHAR;
import static org.hibernate.type.SqlTypes.TIMESTAMP;
import static org.hibernate.type.SqlTypes.TIMESTAMP_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.TINYINT;
import static org.hibernate.type.SqlTypes.VARCHAR;

/**
 * A dialect for Mimer SQL 11.
 *
 * @author Fredrik lund
 * @author Gavin King
 */
public class MimerSQLDialect extends Dialect implements CurrentTemporalSupport, TemporalFormatSupport, TemporalOperationSupport {
	private SchemaDropSupport schemaDropSupport;


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
	private final TypeSizingProfile typeSizingProfile = TypeSizingProfile.builder( super.getTypeSizingProfile() )
			.maxVarcharLength( 15_000 ).maxVarcharCapacity( 15_000 )
			.maxNVarcharLength( 5000 ).maxNVarcharCapacity( 5000 )
			.maxVarbinaryLength( 15_000 ).maxVarbinaryCapacity( 15_000 )
			.build();

	@Override public TypeSizingProfile getTypeSizingProfile() { return typeSizingProfile; }

	// KNOWN LIMITATIONS:

	// * no support for format()
	// * can't cast non-literal String to Binary
	// * no power(), exp(), ln(), sqrt() functions
	// * no trig functions, not even sin()
	// * can't select a parameter unless wrapped
	//   in a cast or function call

	public MimerSQLDialect() {
		super( ZERO_VERSION );
	}

	public MimerSQLDialect(DialectResolutionInfo info) {
		super( info );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	protected String columnType(int sqlTypeCode) {
		switch ( sqlTypeCode ) {
			//no 'tinyint', so use integer with 3 decimal digits
			case TINYINT:
				return "integer(3)";
			case TIMESTAMP_WITH_TIMEZONE:
				return columnType( TIMESTAMP );
			//Mimer CHARs are ASCII!!
			case CHAR:
				return columnType( NCHAR );
			case VARCHAR:
				return columnType( NVARCHAR );
			case LONG32VARCHAR:
				return columnType( LONG32NVARCHAR );
			//default length is 1M, which is quite low
			case BLOB:
				return "blob(2G)";
			case CLOB:
			case NCLOB:
				return "nclob(2G)";
			default:
				return super.columnType( sqlTypeCode );
		}
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	protected void registerColumnTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.registerColumnTypes( typeContributions, serviceRegistry );
		final DdlTypeRegistry ddlTypeRegistry = typeContributions.getTypeConfiguration().getDdlTypeRegistry();

		//precision of a Mimer 'float(p)' represents
		//decimal digits instead of binary digits
		ddlTypeRegistry.addDescriptor( StandardDdlTypes.binaryFloat( this ) );

		//Mimer CHARs are ASCII!!
		ddlTypeRegistry.addDescriptor(
				StandardDdlTypes.builder( VARCHAR, columnType( LONG32VARCHAR ), this )
						.lobKind( getLobSupport().isLobType( LONG32VARCHAR )
								? DdlTypeBuilder.LobKind.BIGGEST
								: DdlTypeBuilder.LobKind.NONE )
						.castTypeName( "nvarchar(" + getTypeSizingProfile().maxNVarcharLength() + ")" )
						.withTypeCapacity( getTypeSizingProfile().maxNVarcharLength(), columnType( VARCHAR ) )
						.build()
		);
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public DatabaseVersion determineDatabaseVersion(DialectResolutionInfo info) {
		return ZERO_VERSION;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	protected void contributeDefaultProperties(java.util.Properties properties) {
		super.contributeDefaultProperties( properties );
		properties.setProperty( org.hibernate.cfg.AvailableSettings.STATEMENT_BATCH_SIZE, Integer.toString( 50 ) );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry(functionContributions);

		CommonFunctionFactory functionFactory = new CommonFunctionFactory(functionContributions);
		functionFactory.soundex();
		functionFactory.octetLength();
		functionFactory.bitLength();
		functionFactory.trunc_truncate();
		functionFactory.repeat();
		functionFactory.pad_repeat();
		functionFactory.dayofweekmonthyear();
		functionFactory.concat_pipeOperator();
		functionFactory.position();
		functionFactory.localtimeLocaltimestamp();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <S extends Statement, T extends JdbcOperation> SqlAstTranslator<T> createTranslator(
					SqlAstTranslationRequest<S, T> request) {
				return new MimerSQLSqlAstTranslator<>( request );
			}
		};
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String currentTimestamp() {
		return "localtimestamp";
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String currentTime() {
		return "localtime";
	}

	/**
	 * Mimer supports a limited list of temporal fields in the
	 * extract() function, but we can emulate some of them by
	 * using the appropriate named functions instead of
	 * extract().
	 *
	 * Thus, the additional supported fields are
	 * {@link TemporalUnit#WEEK},
	 * {@link TemporalUnit#DAY_OF_YEAR},
	 * {@link TemporalUnit#DAY_OF_MONTH},
	 * {@link TemporalUnit#DAY_OF_YEAR}.
	 */
	@Override
	@SPI({ USE, IMPLEMENT })
	public String extractPattern(TemporalUnit unit) {
		switch (unit) {
			case WEEK:
				return "week(?2)";
			case DAY_OF_WEEK:
				return "dayofweek(?2)";
			case DAY_OF_YEAR:
				return "dayofyear(?2)";
			case DAY_OF_MONTH:
				return "day(?2)";
			default:
				return TemporalOperationSupports.standard().extractPattern(unit);
		}
	}

	@SPI({ USE, IMPLEMENT })
	public String timestampdiffPattern(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) {
		StringBuilder pattern = new StringBuilder();
		pattern.append("cast((?3-?2) ");
		switch (unit) {
			case NATIVE:
			case NANOSECOND:
			case SECOND:
				pattern.append("second(12,9)");
				break;
			case MINUTE:
				pattern.append("minute(10)");
				break;
			case HOUR:
				pattern.append("hour(8)");
				break;
			case DAY:
			case WEEK:
				pattern.append("day(7)");
				break;
			case MONTH:
			case QUARTER:
				pattern.append("month(7)");
				break;
			case YEAR:
				pattern.append("year(7)");
				break;
			default:
				throw new SemanticException("unsupported duration unit: " + unit);
		}
		pattern.append(" as bigint)");
		switch (unit) {
			case WEEK:
				pattern.append("/7");
				break;
			case QUARTER:
				pattern.append("/3");
				break;
			case NATIVE:
			case NANOSECOND:
				pattern.append("*1e9");
				break;
		}
		return pattern.toString();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType, IntervalType intervalType) {
		switch ( unit ) {
			case NATIVE:
			case NANOSECOND:
				return "(?3+(?2)/1e9*interval '1' second)";
			case QUARTER:
				return "(?3+(?2)*interval '3' month)";
			case WEEK:
				return "(?3+(?2)*interval '7' day)";
			default:
				return "(?3+(?2)*interval '1' ?1)";
		}
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public synchronized SchemaDropSupport getSchemaDropSupport() {
		if ( schemaDropSupport == null ) {
			schemaDropSupport = new SchemaDropSupport( java.util.List.of(), ConstraintDropMode.IMPLICIT, " cascade" );
		}
		return schemaDropSupport;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SequenceSupport getSequenceSupport() {
		return MimerSequenceSupport.INSTANCE;
	}

	private static final SequenceInformationExtractor SEQUENCE_INFORMATION_EXTRACTOR =
			SequenceInformationExtractors.builder( "select * from information_schema.ext_sequences" )
					.startValueColumn( "initial_value" )
					.withoutMinimumValue()
					.build();

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		return SEQUENCE_INFORMATION_EXTRACTOR;
	}

	@Override
	public LimitHandler getLimitHandler() {
		return OffsetFetchLimitHandler.INSTANCE;
	}

	@Override
	public LockingSupport getLockingSupport() {
		return StandardLockingSupports.standardWithoutOuterJoinLocking();
	}

	@Override
	public SubquerySupport getSubquerySupport() {
		return SubquerySupport.builder()
				.feature( SubquerySupport.Feature.OFFSET, true )
				.build();
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendFormat(SqlAppender appender, String format) {
		throw new UnsupportedOperationException("format() function not supported on Mimer SQL");
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public LobSupport getLobSupport() {
		return LobSupports.nonStreaming();
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return MimerSQLIdentityColumnSupport.INSTANCE;
	}

	@Override
	public SingleRowTableSupport getSingleRowTableSupport() {
		final var inherited = super.getSingleRowTableSupport();
		return SingleRowTableSupport.builder( inherited )
				.selectOnlyFromClause( " from " + inherited.getTableExpression() )
				.build();
	}

	@Override
	public RowValueSupport getRowValueSupport() {
		return RowValueSupport.NONE;
	}

}

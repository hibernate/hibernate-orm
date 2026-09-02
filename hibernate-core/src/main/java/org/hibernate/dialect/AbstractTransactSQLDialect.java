/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect;

import org.hibernate.dialect.temporaltype.spi.CurrentTimestampSelection;

import org.hibernate.dialect.temporaltype.spi.CurrentTemporalSupport;

import org.hibernate.dialect.mutation.spi.MultiTableMutationSupport;


import org.hibernate.dialect.sql.ast.spi.NullOrdering;
import org.hibernate.dialect.sql.ast.spi.NullOrderingSupport;
import org.hibernate.dialect.sql.ast.spi.SubquerySupport;

import org.hibernate.LockOptions;
import org.hibernate.SPI;
import static org.hibernate.SPI.Role.USE;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.function.CaseLeastGreatestEmulation;
import org.hibernate.dialect.function.CastingConcatFunction;
import org.hibernate.dialect.function.TransactSQLStrFunction;
import org.hibernate.dialect.temptable.spi.TemporaryTableStrategy;
import org.hibernate.dialect.temptable.internal.TransactSQLLocalTemporaryTableStrategy;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.function.spi.ExpressionCoercionSupport;
import org.hibernate.dialect.function.spi.TupleCountSupport;
import org.hibernate.dialect.identity.internal.AbstractTransactSQLIdentityColumnSupport;
import org.hibernate.dialect.identity.spi.IdentityColumnSupport;
import org.hibernate.dialect.schema.spi.IndexNameQualification;
import org.hibernate.query.sqm.TrimSpec;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.spi.translation.SqlAstNodeRenderingMode;
import org.hibernate.dialect.lock.internal.TransactSQLLockingClauseStrategy;
import org.hibernate.dialect.lock.spi.LockingClauseStrategy;
import org.hibernate.sql.spi.SqlAppender;
import org.hibernate.sql.ast.spi.query.select.QuerySpec;
import org.hibernate.type.descriptor.java.PrimitiveByteArrayJavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;
import org.hibernate.type.descriptor.jdbc.JsonAsStringJdbcType;
import org.hibernate.type.descriptor.jdbc.XmlAsStringJdbcType;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;

import java.sql.Types;

import static org.hibernate.type.SqlTypes.BLOB;
import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import static org.hibernate.type.SqlTypes.BOOLEAN;
import static org.hibernate.type.SqlTypes.CLOB;
import static org.hibernate.type.SqlTypes.DATE;
import static org.hibernate.type.SqlTypes.INTEGER;
import static org.hibernate.type.SqlTypes.NCLOB;
import static org.hibernate.type.SqlTypes.TIME;
import static org.hibernate.type.SqlTypes.TIMESTAMP;
import static org.hibernate.type.SqlTypes.TIMESTAMP_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.TIME_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.TINYINT;

/// A supported base for provider Dialects derived from the Transact-SQL family,
/// including Sybase and Microsoft SQL Server variants.
///
/// Provider subclasses must invoke a protected constructor classified
/// {@link SPI.Role#IMPLEMENT IMPLEMENT}. The generated SPI inventory identifies
/// the constructors and members covered by this type-level implementation
/// contract; unclassified implementation details are not provider extension
/// points.
///
/// @author Gavin King
@SPI({ USE, IMPLEMENT })
public abstract class AbstractTransactSQLDialect extends Dialect implements CurrentTemporalSupport {

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public CurrentTemporalSupport getCurrentTemporalSupport() {
		return this;
	}

	@SPI( IMPLEMENT )
	protected AbstractTransactSQLDialect(DatabaseVersion version) {
		super(version);
	}

	@SPI( IMPLEMENT )
	protected AbstractTransactSQLDialect(DialectResolutionInfo info) {
		super(info);
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	protected String columnType(int sqlTypeCode) {
		// note that 'real' is double precision on SQL Server, single precision on Sybase
		// but 'float' is single precision on Sybase, double precision on SQL Server
		return switch (sqlTypeCode) {
			case BOOLEAN -> "bit";

			// 'tinyint' is an unsigned type in Sybase and
			// SQL Server, holding values in the range 0-255
			// see HHH-6779
			case TINYINT -> "smallint";

			//it's called 'int' not 'integer'
			case INTEGER -> "int";

			case DATE, TIME, TIMESTAMP, TIME_WITH_TIMEZONE, TIMESTAMP_WITH_TIMEZONE -> "datetime";

			case BLOB -> "image";
			case CLOB -> "text";
			case NCLOB -> "ntext";

			default -> super.columnType( sqlTypeCode );
		};
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes( typeContributions, serviceRegistry );
		final var jdbcTypeRegistry = typeContributions.getTypeConfiguration().getJdbcTypeRegistry();
		jdbcTypeRegistry.addDescriptor( JsonAsStringJdbcType.NVARCHAR_INSTANCE );
		jdbcTypeRegistry.addDescriptor( XmlAsStringJdbcType.NVARCHAR_INSTANCE );
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	protected void contributeDefaultProperties(java.util.Properties properties) {
		super.contributeDefaultProperties( properties );
		properties.setProperty( org.hibernate.cfg.AvailableSettings.STATEMENT_BATCH_SIZE, Integer.toString( 0 ) );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public JdbcType resolveSqlTypeDescriptor(
			String columnTypeName,
			int jdbcTypeCode,
			int precision,
			int scale,
			JdbcTypeRegistry jdbcTypeRegistry) {
		if ( jdbcTypeCode == Types.BIT ) {
			return jdbcTypeRegistry.getDescriptor( Types.BOOLEAN );
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
	@SPI({ IMPLEMENT, SUPPLY })
	public int getPreferredSqlTypeCodeForBoolean() {
		return Types.BIT;
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry( functionContributions );

		final var functionFactory = new CommonFunctionFactory( functionContributions );

		functionFactory.cot();
		functionFactory.ln_log();
		functionFactory.log_loglog();
		functionFactory.log10();
		functionFactory.atan2_atn2();
		functionFactory.mod_operator();
		functionFactory.square();
		functionFactory.rand();
		functionFactory.radians();
		functionFactory.degrees();
		functionFactory.pi();
		functionFactory.reverse();
		functionFactory.space();
		functionFactory.pad_replicate();
		functionFactory.yearMonthDay();
		functionFactory.ascii();
		functionFactory.chr_char();
		functionFactory.trim1();
		functionFactory.repeat_replicate();
		functionFactory.characterLength_len();
		functionFactory.substring_substringLen();
		functionFactory.datepartDatename();
		functionFactory.lastDay_eomonth();

		functionFactory.bitandorxornot_operator();

		functionContributions.getFunctionRegistry().register( "least", new CaseLeastGreatestEmulation( true ) );
		functionContributions.getFunctionRegistry().register( "greatest", new CaseLeastGreatestEmulation( false ) );
		functionContributions.getFunctionRegistry().register( "str", new TransactSQLStrFunction( functionContributions.getTypeConfiguration() ) );
		functionContributions.getFunctionRegistry().register(
				"concat",
				new CastingConcatFunction(
						this,
						"+",
						false,
						SqlAstNodeRenderingMode.DEFAULT,
						functionContributions.getTypeConfiguration()
				)
		);
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String trimPattern(TrimSpec specification, boolean isWhitespace) {
		return replaceLtrimRtrim( specification, isWhitespace );
	}

	public static String replaceLtrimRtrim(TrimSpec specification, boolean isWhitespace) {
		return switch (specification) {
			case LEADING -> isWhitespace
					? "ltrim(?1)"
					: "substring(?1,patindex('%[^'+?2+']%',?1),len(?1+'x')-1-patindex('%[^'+?2+']%',?1)+1)";
			case TRAILING -> isWhitespace
					? "rtrim(?1)"
					: "substring(?1,1,len(?1+'x')-1-patindex('%[^'+?2+']%',reverse(?1))+1)";
			default -> isWhitespace
					? "ltrim(rtrim(?1))"
					: "substring(?1,patindex('%[^'+?2+']%',?1),len(?1+'x')-1-patindex('%[^'+?2+']%',?1)-patindex('%[^'+?2+']%',reverse(?1))+2)";
		};
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String addColumnPrefix() {
		return "add";
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public IndexNameQualification nameQualification() {
		return IndexNameQualification.UNQUALIFIED;
	}

	@Override
	public LockingClauseStrategy getLockingClauseStrategy(QuerySpec querySpec, LockOptions lockOptions) {
		return new TransactSQLLockingClauseStrategy( lockOptions.getScope(), querySpec.getRootPathsForLocking() );
	}


	@Override
	@SPI({ USE, IMPLEMENT })
	public CurrentTimestampSelection getCurrentTimestampSelection() {
		return CurrentTimestampSelection.prepared( "select getdate()" );
	}

	@Override
	public NullOrderingSupport getNullOrderingSupport() {
		return NullOrderingSupport.builder( super.getNullOrderingSupport() )
				.defaultOrdering( NullOrdering.SMALLEST )
				.build();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public ExpressionCoercionSupport getExpressionCoercionSupport() {
		return ExpressionCoercionSupport.builder()
				.requirements( ExpressionCoercionSupport.Requirement.CAST_NON_STRING_CONCATENATION_ARGUMENTS )
				.build();
	}

	@Override
	public MultiTableMutationSupport getMultiTableMutationSupport() {
		return MultiTableMutationSupport.LOCAL_TEMPORARY_TABLE;
	}

	@Override
	public TemporaryTableStrategy getLocalTemporaryTableStrategy() {
		return TransactSQLLocalTemporaryTableStrategy.INSTANCE;
	}

	// Overridden informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SubquerySupport getSubquerySupport() {
		return SubquerySupport.builder()
				.feature( SubquerySupport.Feature.EXISTS_IN_SELECT, false )
				.build();
	}

	@Override
	public TupleCountSupport getTupleCountSupport() {
		return TupleCountSupport.NONE;
	}

	/// Supply the common Transact-SQL identity profile. Subclasses may override
	/// this profile when their identity declaration or retrieval SQL differs.
	///
	/// @since 8.0
	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public IdentityColumnSupport getIdentityColumnSupport() {
		return AbstractTransactSQLIdentityColumnSupport.INSTANCE;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public void appendBinaryLiteral(SqlAppender appender, byte[] bytes) {
		appender.appendSql( "0x" );
		PrimitiveByteArrayJavaType.INSTANCE.appendString( appender, bytes );
	}
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import org.hibernate.dialect.schema.spi.ConstraintDropMode;
import org.hibernate.dialect.schema.spi.SchemaDropSupport;

import org.hibernate.dialect.temporaltype.spi.CurrentTemporalSupport;

import org.hibernate.SPI;
import static org.hibernate.SPI.Role.USE;

import static org.hibernate.SPI.Role.IMPLEMENT;
import static org.hibernate.SPI.Role.SUPPLY;
import org.hibernate.dialect.type.spi.TypeSizingProfile;



import org.hibernate.Length;
import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.community.dialect.identity.internal.SybaseAnywhereIdentityColumnSupport;
import org.hibernate.dialect.AbstractSybaseDialect;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.lob.spi.LobSupport;
import org.hibernate.dialect.lob.spi.LobSupports;
import org.hibernate.dialect.type.spi.TimeZoneSupport;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.function.spi.WindowFunctionSupport;
import org.hibernate.dialect.identity.spi.IdentityColumnSupport;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.lock.spi.StandardLockingSupports;
import org.hibernate.dialect.pagination.spi.LimitHandler;
import org.hibernate.dialect.pagination.spi.TopLimitHandler;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.sql.ast.spi.translation.SqlAstTranslator;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslatorFactory;
import org.hibernate.dialect.sql.ast.spi.SubquerySupport;
import org.hibernate.dialect.sql.ast.spi.SingleRowTableSupport;
import org.hibernate.dialect.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.Statement;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.sql.exec.spi.JdbcOperation;


import static org.hibernate.type.SqlTypes.DATE;
import static org.hibernate.type.SqlTypes.LONG32NVARCHAR;
import static org.hibernate.type.SqlTypes.LONG32VARBINARY;
import static org.hibernate.type.SqlTypes.LONG32VARCHAR;
import static org.hibernate.type.SqlTypes.NCLOB;
import static org.hibernate.type.SqlTypes.TIME;
import static org.hibernate.type.SqlTypes.TIMESTAMP;
import static org.hibernate.type.SqlTypes.TIMESTAMP_WITH_TIMEZONE;
import static org.hibernate.type.SqlTypes.TIME_WITH_TIMEZONE;

/**
 * SQL Dialect for Sybase/SQL Anywhere
 * (Tested on ASA 8.x)
 */
public class SybaseAnywhereDialect extends AbstractSybaseDialect implements CurrentTemporalSupport {
	private SchemaDropSupport schemaDropSupport;


	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public CurrentTemporalSupport getCurrentTemporalSupport() {
		return this;
	}
	private final TypeSizingProfile typeSizingProfile = TypeSizingProfile.builder( super.getTypeSizingProfile() )
			.maxVarcharLength( Length.LONG16 ).maxVarcharCapacity( Length.LONG16 )
			.maxNVarcharLength( Length.LONG16 ).maxNVarcharCapacity( Length.LONG16 )
			.maxVarbinaryLength( Length.LONG16 ).maxVarbinaryCapacity( Length.LONG16 )
			.build();

	@Override public TypeSizingProfile getTypeSizingProfile() { return typeSizingProfile; }
	private final LockingSupport lockingSupport;

	public SybaseAnywhereDialect() {
		this( DatabaseVersion.make( 8 ) );
	}

	public SybaseAnywhereDialect(DialectResolutionInfo info) {
		super(info);
		lockingSupport = StandardLockingSupports.sybaseAnywhere( getVersion() );
	}

	public SybaseAnywhereDialect(DatabaseVersion version) {
		super(version);
		lockingSupport = StandardLockingSupports.sybaseAnywhere( getVersion() );
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	protected String columnType(int sqlTypeCode) {
		return switch ( sqlTypeCode ) {
			case DATE -> "date";
			case TIME -> "time";
			case TIMESTAMP -> "timestamp";
			case TIME_WITH_TIMEZONE, TIMESTAMP_WITH_TIMEZONE -> "timestamp with time zone";
			//these types hold up to 2 GB
			case LONG32VARCHAR -> "long varchar";
			case LONG32NVARCHAR -> "long nvarchar";
			case LONG32VARBINARY -> "long binary";
			case NCLOB -> "ntext";
			default -> super.columnType( sqlTypeCode );
		};
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public LobSupport getLobSupport() {
		return LobSupports.noCapacityPromotion();
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry(functionContributions);
		final CommonFunctionFactory functionFactory = new CommonFunctionFactory(functionContributions);
		functionFactory.listagg_list( "varchar" );
		if ( getWindowFunctionSupport().supports( WindowFunctionSupport.Feature.WINDOW_FUNCTIONS ) ) {
			functionFactory.windowFunctions();
		}
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <S extends Statement, T extends JdbcOperation> SqlAstTranslator<T> createTranslator(
					SqlAstTranslationRequest<S, T> request) {
				return new SybaseAnywhereSqlAstTranslator<>( request );
			}
		};
	}

	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public TimeZoneSupport getTimeZoneSupport() {
		return TimeZoneSupport.NATIVE;
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String currentDate() {
		return "current date";
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String currentTime() {
		return "current time";
	}

	@Override
	@SPI({ USE, IMPLEMENT })
	public String currentTimestamp() {
		return "current timestamp";
	}

	/**
	 * ASA does not require to drop constraint before dropping tables, so disable it.
	 * <p>
	 * NOTE : Also, the DROP statement syntax used by Hibernate to drop constraints is
	 * not compatible with ASA.
	 * <p>
	 * {@inheritDoc}
	 */
	@Override
	@SPI({ IMPLEMENT, SUPPLY })
	public synchronized SchemaDropSupport getSchemaDropSupport() {
		if ( schemaDropSupport == null ) {
			schemaDropSupport = new SchemaDropSupport( java.util.List.of(), ConstraintDropMode.IMPLICIT, "" );
		}
		return schemaDropSupport;
	}

	@Override
	public WindowFunctionSupport getWindowFunctionSupport() {
		return getVersion().isBefore( 9 )
				? WindowFunctionSupport.NONE
				: WindowFunctionSupport.builder()
						.features(
								WindowFunctionSupport.Feature.WINDOW_FUNCTIONS,
								WindowFunctionSupport.Feature.PARTITION_BY,
								WindowFunctionSupport.Feature.ROWS_FRAME,
								WindowFunctionSupport.Feature.RANGE_FRAME
						)
						.build();
	}

	@Override
	public SubquerySupport getSubquerySupport() {
		return SubquerySupport.builder( super.getSubquerySupport() )
				.feature( SubquerySupport.Feature.LATERAL, getVersion().isSameOrAfter( 10 ) )
				.build();
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return SybaseAnywhereIdentityColumnSupport.INSTANCE;
	}

	@Override
	public LockingSupport getLockingSupport() {
		return lockingSupport;
	}

	@Override
	public LimitHandler getLimitHandler() {
		//TODO: support 'TOP ? START AT ?'
		//Note: Sybase Anywhere also supports LIMIT OFFSET,
		//      but it looks like this syntax is not enabled
		//      by default
		return TopLimitHandler.INSTANCE;
	}

	@Override
	public SingleRowTableSupport getSingleRowTableSupport() {
		final String tableExpression = "sys.dummy";
		return SingleRowTableSupport.builder( super.getSingleRowTableSupport() )
				.tableExpression( tableExpression )
				.selectOnlyFromClause( " from " + tableExpression )
				.build();
	}

}

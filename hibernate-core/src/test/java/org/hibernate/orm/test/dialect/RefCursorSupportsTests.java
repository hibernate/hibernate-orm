/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.hibernate.HibernateException;
import org.hibernate.JDBCException;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.DB2Dialect;
import org.hibernate.dialect.DB2iDialect;
import org.hibernate.dialect.DB2zDialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.PostgresPlusDialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.jdbc.spi.OracleTypes;
import org.hibernate.engine.jdbc.cursor.spi.RefCursorSupport;
import org.hibernate.engine.jdbc.cursor.spi.RefCursorSupportCreationContext;
import org.hibernate.engine.jdbc.cursor.spi.RefCursorSupportFactory;
import org.hibernate.engine.jdbc.cursor.spi.RefCursorSupports;
import org.hibernate.engine.jdbc.cursor.internal.RefCursorSupportInitiator;
import org.hibernate.engine.jdbc.env.spi.JdbcMetadata;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.jdbc.spi.SqlExceptionHelper;
import org.hibernate.service.spi.ServiceRegistryImplementor;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/// Tests the supported REF_CURSOR factory facility and stock strategies.
///
/// @author Steve Ebersole
public class RefCursorSupportsTests {
	@Test
	void serviceBootstrapInvokesFactoryOnceWithEffectiveMetadataAndConfiguredConverter() {
		final JdbcMetadata metadata = mock( JdbcMetadata.class );
		when( metadata.supportsRefCursors() ).thenReturn( false );
		final JDBCException converted = mock( JDBCException.class );
		final SQLException failure = new SQLException( "controlled" );
		final SqlExceptionHelper exceptionHelper = mock( SqlExceptionHelper.class );
		when( exceptionHelper.convert( failure, "conversion" ) ).thenReturn( converted );

		final AtomicInteger factoryCalls = new AtomicInteger();
		final RefCursorSupportCreationContext[] receivedContext = new RefCursorSupportCreationContext[1];
		final RefCursorSupport expected = mock( RefCursorSupport.class );
		final RefCursorSupportFactory factory = creationContext -> {
			factoryCalls.incrementAndGet();
			receivedContext[0] = creationContext;
			assertThat( creationContext.supportsStandardRefCursors() ).isFalse();
			return expected;
		};
		final Dialect dialect = new Dialect( DatabaseVersion.make( 1 ) ) {
			@Override
			public RefCursorSupportFactory getRefCursorSupportFactory() {
				return factory;
			}
		};
		final JdbcServices jdbcServices = mock( JdbcServices.class );
		when( jdbcServices.getDialect() ).thenReturn( dialect );
		when( jdbcServices.getJdbcMetadata() ).thenReturn( metadata );
		when( jdbcServices.getSqlExceptionHelper() ).thenReturn( exceptionHelper );
		final ServiceRegistryImplementor registry = mock( ServiceRegistryImplementor.class );
		when( registry.requireService( JdbcServices.class ) ).thenReturn( jdbcServices );

		assertThat( RefCursorSupportInitiator.INSTANCE.initiateService( Map.of(), registry ) )
				.isSameAs( expected );
		assertThat( factoryCalls ).hasValue( 1 );
		assertThat( receivedContext[0].convert( failure, "conversion" ) ).isSameAs( converted );
		verify( metadata ).supportsRefCursors();
		verify( exceptionHelper ).convert( failure, "conversion" );
	}

	@Test
	void serviceBootstrapRejectsNullFactoryAndNullFactoryResult() {
		assertThatExceptionOfType( IllegalStateException.class )
				.isThrownBy( () -> initiateWith( null ) )
				.withMessageContaining( "null RefCursorSupportFactory" );
		final RefCursorSupportFactory nullResult = creationContext -> null;
		assertThatExceptionOfType( IllegalStateException.class )
				.isThrownBy( () -> initiateWith( nullResult ) )
				.withMessageContaining( nullResult.toString() )
				.withMessageContaining( "non-null RefCursorSupport" );
	}

	@Test
	void stableFactoriesAndMetadataSelection() {
		assertThat( RefCursorSupports.metadataSelected() ).isSameAs( RefCursorSupports.metadataSelected() );
		assertThat( RefCursorSupports.standard() ).isSameAs( RefCursorSupports.standard() );
		assertThat( RefCursorSupports.unsupported() ).isSameAs( RefCursorSupports.unsupported() );
		assertThat( RefCursorSupports.postgresql() ).isSameAs( RefCursorSupports.postgresql() );
		assertThat( RefCursorSupports.postgresPlus() ).isSameAs( RefCursorSupports.postgresPlus() );
		assertThat( RefCursorSupports.hana() ).isSameAs( RefCursorSupports.hana() );

		final AtomicInteger fallbackCalls = new AtomicInteger();
		final RefCursorSupport fallback = mock( RefCursorSupport.class );
		final RefCursorSupportFactory selected = RefCursorSupports.metadataSelected( context -> {
			fallbackCalls.incrementAndGet();
			return fallback;
		} );
		assertThat( selected.createRefCursorSupport( context( true ) ) ).isNotSameAs( fallback );
		assertThat( fallbackCalls ).hasValue( 0 );
		assertThat( selected.createRefCursorSupport( context( false ) ) ).isSameAs( fallback );
		assertThat( fallbackCalls ).hasValue( 1 );

		assertThatIllegalArgumentException().isThrownBy( () -> RefCursorSupports.metadataSelected( null ) );
		assertThatExceptionOfType( IllegalStateException.class )
				.isThrownBy( () -> RefCursorSupports.metadataSelected( creationContext -> null )
						.createRefCursorSupport( context( false ) ) )
				.withMessageContaining( "non-null RefCursorSupport" );
	}

	@Test
	void standardAndJdbcTypeStrategiesUseTheirExactJdbcPaths() throws Exception {
		final CallableStatement statement = mock( CallableStatement.class );
		final ResultSet positional = mock( ResultSet.class );
		final ResultSet named = mock( ResultSet.class );
		when( statement.getObject( 3, ResultSet.class ) ).thenReturn( positional );
		when( statement.getObject( "items", ResultSet.class ) ).thenReturn( named );

		final RefCursorSupport standard = RefCursorSupports.standard().createRefCursorSupport( context( false ) );
		standard.registerRefCursorParameter( statement, 3 );
		standard.registerRefCursorParameter( statement, "items" );
		assertThat( standard.getResultSet( statement, 3 ) ).isSameAs( positional );
		assertThat( standard.getResultSet( statement, "items" ) ).isSameAs( named );
		verify( statement ).registerOutParameter( 3, Types.REF_CURSOR );
		verify( statement ).registerOutParameter( "items", Types.REF_CURSOR );

		final CallableStatement untypedStatement = mock( CallableStatement.class );
		when( untypedStatement.getObject( 4 ) ).thenReturn( positional );
		when( untypedStatement.getObject( "rows" ) ).thenReturn( named );
		final RefCursorSupport untyped = RefCursorSupports.jdbcType( 60_001 )
				.createRefCursorSupport( context( true ) );
		untyped.registerRefCursorParameter( untypedStatement, 4 );
		untyped.registerRefCursorParameter( untypedStatement, "rows" );
		assertThat( untyped.getResultSet( untypedStatement, 4 ) ).isSameAs( positional );
		assertThat( untyped.getResultSet( untypedStatement, "rows" ) ).isSameAs( named );
		verify( untypedStatement ).registerOutParameter( 4, 60_001 );
		verify( untypedStatement ).registerOutParameter( "rows", 60_001 );
	}

	@Test
	void unsupportedStrategyNeverInvokesTheStatement() {
		final CallableStatement statement = mock( CallableStatement.class );
		final RefCursorSupport unsupported = RefCursorSupports.unsupported()
				.createRefCursorSupport( context( true ) );

		assertThatExceptionOfType( UnsupportedOperationException.class )
				.isThrownBy( () -> unsupported.registerRefCursorParameter( statement, 2 ) )
				.withMessageContaining( "registration" ).withMessageContaining( "2" );
		assertThatExceptionOfType( UnsupportedOperationException.class )
				.isThrownBy( () -> unsupported.registerRefCursorParameter( statement, "cursor" ) )
				.withMessageContaining( "registration" ).withMessageContaining( "cursor" );
		assertThatExceptionOfType( UnsupportedOperationException.class )
				.isThrownBy( () -> unsupported.getResultSet( statement, 2 ) )
				.withMessageContaining( "extraction" ).withMessageContaining( "2" );
		assertThatExceptionOfType( UnsupportedOperationException.class )
				.isThrownBy( () -> unsupported.getResultSet( statement, "cursor" ) )
				.withMessageContaining( "extraction" ).withMessageContaining( "cursor" );
		verifyNoInteractions( statement );
	}

	@Test
	void maintainedDialectsSelectThePreservedAccessStrategies() throws Exception {
		final CallableStatement db2Statement = mock( CallableStatement.class );
		final ResultSet db2Positional = mock( ResultSet.class );
		final ResultSet db2Named = mock( ResultSet.class );
		when( db2Statement.getObject( 1 ) ).thenReturn( db2Positional );
		when( db2Statement.getObject( "rows" ) ).thenReturn( db2Named );
		final RefCursorSupport db2 = new DB2Dialect().getRefCursorSupportFactory()
				.createRefCursorSupport( context( false ) );
		db2.registerRefCursorParameter( db2Statement, 1 );
		db2.registerRefCursorParameter( db2Statement, "rows" );
		assertThat( db2.getResultSet( db2Statement, 1 ) ).isSameAs( db2Positional );
		assertThat( db2.getResultSet( db2Statement, "rows" ) ).isSameAs( db2Named );
		verify( db2Statement ).registerOutParameter( 1, Types.REF_CURSOR );
		verify( db2Statement ).registerOutParameter( "rows", Types.REF_CURSOR );
		assertThat( new DB2iDialect().getJdbcMetadataOverrides().getStandardRefCursorSupport() )
				.isSameAs( org.hibernate.dialect.jdbc.spi.JdbcMetadataOverrides.SupportOverride.UNSUPPORTED );
		assertThat( new DB2zDialect().getJdbcMetadataOverrides().getStandardRefCursorSupport() )
				.isSameAs( org.hibernate.dialect.jdbc.spi.JdbcMetadataOverrides.SupportOverride.UNSUPPORTED );

		final CallableStatement oracleStatement = mock( CallableStatement.class );
		final OracleDialect oracleDialect = new OracleDialect();
		assertThat( oracleDialect.getRefCursorSupportFactory() )
				.isSameAs( oracleDialect.getRefCursorSupportFactory() );
		final RefCursorSupport oracleFallback = oracleDialect.getRefCursorSupportFactory()
				.createRefCursorSupport( context( false ) );
		oracleFallback.registerRefCursorParameter( oracleStatement, 2 );
		verify( oracleStatement ).registerOutParameter( 2, OracleTypes.CURSOR );
		final CallableStatement oracleStandardStatement = mock( CallableStatement.class );
		oracleDialect.getRefCursorSupportFactory().createRefCursorSupport( context( true ) )
				.registerRefCursorParameter( oracleStandardStatement, 2 );
		verify( oracleStandardStatement ).registerOutParameter( 2, Types.REF_CURSOR );

		final CallableStatement postgresqlStatement = mock( CallableStatement.class );
		final ResultSet postgresqlResult = mock( ResultSet.class );
		when( postgresqlStatement.getObject( 1 ) ).thenReturn( postgresqlResult );
		final RefCursorSupport postgresql = new PostgreSQLDialect().getRefCursorSupportFactory()
				.createRefCursorSupport( context( false ) );
		postgresql.registerRefCursorParameter( postgresqlStatement, 1 );
		assertThat( postgresql.getResultSet( postgresqlStatement, 1 ) ).isSameAs( postgresqlResult );
		verify( postgresqlStatement ).registerOutParameter( 1, Types.OTHER );
		assertThatExceptionOfType( UnsupportedOperationException.class )
				.isThrownBy( () -> postgresql.registerRefCursorParameter( postgresqlStatement, "rows" ) )
				.withMessageContaining( "by position" );
		assertThatExceptionOfType( UnsupportedOperationException.class )
				.isThrownBy( () -> postgresql.getResultSet( postgresqlStatement, 2 ) )
				.withMessageContaining( "first parameter" );
		assertThatExceptionOfType( UnsupportedOperationException.class )
				.isThrownBy( () -> postgresql.getResultSet( postgresqlStatement, "rows" ) )
				.withMessageContaining( "by position" );

		final CallableStatement postgresPlusStatement = mock( CallableStatement.class );
		new PostgresPlusDialect().getRefCursorSupportFactory()
				.createRefCursorSupport( context( false ) )
				.registerRefCursorParameter( postgresPlusStatement, 1 );
		verify( postgresPlusStatement ).registerOutParameter( 1, Types.REF );

		final CallableStatement hanaStatement = mock( CallableStatement.class );
		final RefCursorSupport hana = new HANADialect().getRefCursorSupportFactory()
				.createRefCursorSupport( context( false ) );
		hana.registerRefCursorParameter( hanaStatement, 1 );
		hana.registerRefCursorParameter( hanaStatement, "rows" );
		verifyNoInteractions( hanaStatement );
		assertThatExceptionOfType( UnsupportedOperationException.class )
				.isThrownBy( () -> hana.getResultSet( hanaStatement, 1 ) );
		assertThatExceptionOfType( UnsupportedOperationException.class )
				.isThrownBy( () -> hana.getResultSet( hanaStatement, "rows" ) );

		assertThat( new H2Dialect().getRefCursorSupportFactory() )
				.isSameAs( RefCursorSupports.metadataSelected() );
	}

	@Test
	void failuresPreserveTheSpecifiedExceptionBoundaries() throws Exception {
		final SQLException sqlException = new SQLException( "driver failure" );
		final CallableStatement statement = mock( CallableStatement.class );
		doThrow( sqlException ).when( statement ).registerOutParameter( 5, 60_002 );
		final RefCursorSupport untyped = RefCursorSupports.jdbcType( 60_002 )
				.createRefCursorSupport( context( false ) );
		assertThatExceptionOfType( JDBCException.class )
				.isThrownBy( () -> untyped.registerRefCursorParameter( statement, 5 ) )
				.withCause( sqlException )
				.withMessageContaining( "5" );

		when( statement.getObject( 6 ) ).thenThrow( sqlException );
		assertThatExceptionOfType( JDBCException.class )
				.isThrownBy( () -> untyped.getResultSet( statement, 6 ) )
				.withCause( sqlException )
				.withMessageContaining( "6" );
		when( statement.getObject( 7 ) ).thenReturn( "not a result set" );
		assertThatExceptionOfType( ClassCastException.class )
				.isThrownBy( () -> untyped.getResultSet( statement, 7 ) );

		when( statement.getObject( 8, ResultSet.class ) ).thenThrow( sqlException );
		assertThatExceptionOfType( HibernateException.class )
				.isThrownBy( () -> RefCursorSupports.standard()
						.createRefCursorSupport( context( true ) )
						.getResultSet( statement, 8 ) )
				.withCause( sqlException )
				.withMessageContaining( "Unexpected error extracting REF_CURSOR parameter [8]" );
	}

	private static RefCursorSupportCreationContext context(boolean supportsStandardRefCursors) {
		return new RefCursorSupportCreationContext() {
			@Override
			public boolean supportsStandardRefCursors() {
				return supportsStandardRefCursors;
			}

			@Override
			public JDBCException convert(SQLException exception, String message) {
				return new JDBCException( message, exception );
			}
		};
	}

	private static RefCursorSupport initiateWith(RefCursorSupportFactory factory) {
		final Dialect dialect = new Dialect( DatabaseVersion.make( 1 ) ) {
			@Override
			public RefCursorSupportFactory getRefCursorSupportFactory() {
				return factory;
			}
		};
		final JdbcServices jdbcServices = mock( JdbcServices.class );
		when( jdbcServices.getDialect() ).thenReturn( dialect );
		final ServiceRegistryImplementor registry = mock( ServiceRegistryImplementor.class );
		when( registry.requireService( JdbcServices.class ) ).thenReturn( jdbcServices );
		return RefCursorSupportInitiator.INSTANCE.initiateService( Map.of(), registry );
	}
}

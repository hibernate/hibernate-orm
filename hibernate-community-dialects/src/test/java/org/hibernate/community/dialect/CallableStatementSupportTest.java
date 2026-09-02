/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.community.dialect;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import jakarta.persistence.ParameterMode;

import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.JDBCException;
import org.hibernate.dialect.DatabaseVersion;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.env.spi.JdbcMetadata;
import org.hibernate.engine.jdbc.cursor.spi.RefCursorSupportCreationContext;
import org.hibernate.engine.jdbc.cursor.spi.RefCursorSupports;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.procedure.spi.CallableStatementSupport;
import org.hibernate.procedure.spi.CallableStatementSupports;
import org.hibernate.procedure.spi.ProcedureCallImplementor;
import org.hibernate.procedure.spi.ProcedureParameterImplementor;
import org.hibernate.query.spi.ProcedureParameterMetadataImplementor;
import org.hibernate.sql.exec.spi.JdbcCallParameterRegistration;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

/// Verifies community Dialect callable-strategy selection through the
/// supported facility.
///
/// @author Steve Ebersole
public class CallableStatementSupportTest {
	@Test
	void legacyFamiliesSelectTheSupportedStockStrategies() {
		assertThat( new DB2LegacyDialect().getCallableStatementSupport() )
				.isSameAs( CallableStatementSupports.db2() );
		assertThat( new HANALegacyDialect().getCallableStatementSupport() )
				.isSameAs( CallableStatementSupports.standardWithRefCursors() );
		assertThat( new OracleLegacyDialect().getCallableStatementSupport() )
				.isSameAs( CallableStatementSupports.standardWithRefCursors() );
		assertThat( new PostgreSQLLegacyDialect( DatabaseVersion.make( 10 ) ).getCallableStatementSupport() )
				.isSameAs( CallableStatementSupports.postgresql( false ) );
		assertThat( new PostgreSQLLegacyDialect( DatabaseVersion.make( 11 ) ).getCallableStatementSupport() )
				.isSameAs( CallableStatementSupports.postgresql( true ) );
	}

	@Test
	void gaussDbRetainsItsConfiguredStrategy() {
		final var dialect = new GaussDBDialect();
		assertThat( dialect.getCallableStatementSupport() )
				.isSameAs( dialect.getCallableStatementSupport() )
				.isNotSameAs( CallableStatementSupports.standardWithRefCursors() );
		assertThat( interpretNamedProcedure( dialect.getCallableStatementSupport() ) )
				.isEqualTo( "{call work(value => ?)}" );
	}

	@Test
	void oracleLegacyRetainsPositionalStandardRendering() {
		assertThat( interpretNamedProcedure( new OracleLegacyDialect().getCallableStatementSupport() ) )
				.isEqualTo( "{call work(?)}" );
	}

	@Test
	void legacyMetadataAndCursorMappingsAreStable() throws Exception {
		final DB2LegacyDialect db2 = new DB2LegacyDialect();
		assertThat( db2.getJdbcMetadataOverrides().getStandardRefCursorSupport() )
				.isSameAs( org.hibernate.dialect.jdbc.spi.JdbcMetadataOverrides.SupportOverride.UNSUPPORTED );
		assertThat( db2.getRefCursorSupportFactory() ).isSameAs( db2.getRefCursorSupportFactory() );
		final CallableStatement db2Statement = mock( CallableStatement.class );
		final ResultSet db2Result = mock( ResultSet.class );
		when( db2Statement.getObject( 1 ) ).thenReturn( db2Result );
		final var db2Support = db2.getRefCursorSupportFactory().createRefCursorSupport( cursorContext( false ) );
		db2Support.registerRefCursorParameter( db2Statement, 1 );
		assertThat( db2Support.getResultSet( db2Statement, 1 ) ).isSameAs( db2Result );
		verify( db2Statement ).registerOutParameter( 1, Types.REF_CURSOR );
		assertThat( new DB2iLegacyDialect().getJdbcMetadataOverrides().getStandardRefCursorSupport() )
				.isSameAs( org.hibernate.dialect.jdbc.spi.JdbcMetadataOverrides.SupportOverride.UNSUPPORTED );
		assertThat( new DB2zLegacyDialect().getJdbcMetadataOverrides().getStandardRefCursorSupport() )
				.isSameAs( org.hibernate.dialect.jdbc.spi.JdbcMetadataOverrides.SupportOverride.UNSUPPORTED );

		final CallableStatement oracleStatement = mock( CallableStatement.class );
		new OracleLegacyDialect().getRefCursorSupportFactory()
				.createRefCursorSupport( cursorContext( false ) )
				.registerRefCursorParameter( oracleStatement, 2 );
		verify( oracleStatement ).registerOutParameter( 2, org.hibernate.dialect.jdbc.spi.OracleTypes.CURSOR );

		final CallableStatement postgresqlStatement = mock( CallableStatement.class );
		new PostgreSQLLegacyDialect().getRefCursorSupportFactory()
				.createRefCursorSupport( cursorContext( false ) )
				.registerRefCursorParameter( postgresqlStatement, 1 );
		verify( postgresqlStatement ).registerOutParameter( 1, Types.OTHER );

		final CallableStatement postgresPlusStatement = mock( CallableStatement.class );
		new PostgresPlusLegacyDialect().getRefCursorSupportFactory()
				.createRefCursorSupport( cursorContext( false ) )
				.registerRefCursorParameter( postgresPlusStatement, 1 );
		verify( postgresPlusStatement ).registerOutParameter( 1, Types.REF );

		assertThat( new HANALegacyDialect().getRefCursorSupportFactory() )
				.isSameAs( RefCursorSupports.hana() );

		final CallableStatement gaussStatement = mock( CallableStatement.class );
		final ResultSet gaussResult = mock( ResultSet.class );
		when( gaussStatement.getObject( 1 ) ).thenReturn( gaussResult );
		final var gaussSupport = new GaussDBDialect().getRefCursorSupportFactory()
				.createRefCursorSupport( cursorContext( false ) );
		gaussSupport.registerRefCursorParameter( gaussStatement, 1 );
		assertThat( gaussSupport.getResultSet( gaussStatement, 1 ) ).isSameAs( gaussResult );
		verify( gaussStatement ).registerOutParameter( 1, Types.OTHER );
		assertThatExceptionOfType( UnsupportedOperationException.class )
				.isThrownBy( () -> gaussSupport.registerRefCursorParameter( gaussStatement, "rows" ) );
		assertThatExceptionOfType( UnsupportedOperationException.class )
				.isThrownBy( () -> gaussSupport.getResultSet( gaussStatement, 2 ) );
		assertThatExceptionOfType( UnsupportedOperationException.class )
				.isThrownBy( () -> gaussSupport.getResultSet( gaussStatement, "rows" ) )
				.withMessageContaining( "GaussDB" );
	}

	@Test
	void legacySybasePreservesBothDriverBranches() {
		assertThat( new SybaseLegacyDialect( info(
				"jTDS Type 4 JDBC Driver for MS SQL Server and Sybase" ) )
				.getJdbcMetadataOverrides().getNamedParameterSupport() )
				.isSameAs( org.hibernate.dialect.jdbc.spi.JdbcMetadataOverrides.SupportOverride.REPORTED );
		assertThat( new SybaseLegacyDialect( info( "jConnect (TM) for JDBC (TM)" ) )
				.getJdbcMetadataOverrides().getNamedParameterSupport() )
				.isSameAs( org.hibernate.dialect.jdbc.spi.JdbcMetadataOverrides.SupportOverride.UNSUPPORTED );
	}

	private static RefCursorSupportCreationContext cursorContext(boolean standard) {
		return new RefCursorSupportCreationContext() {
			@Override
			public boolean supportsStandardRefCursors() {
				return standard;
			}

			@Override
			public JDBCException convert(SQLException exception, String message) {
				return new JDBCException( message, exception );
			}
		};
	}

	private static DialectResolutionInfo info(String driverName) {
		final DialectResolutionInfo info = mock( DialectResolutionInfo.class );
		when( info.getDriverName() ).thenReturn( driverName );
		when( info.getDatabaseVersion() ).thenReturn( "16.0" );
		when( info.getDatabaseMajorVersion() ).thenReturn( 16 );
		return info;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static String interpretNamedProcedure(CallableStatementSupport support) {
		final ProcedureCallImplementor<?> procedureCall = mock( ProcedureCallImplementor.class );
		final ProcedureParameterMetadataImplementor metadata = mock( ProcedureParameterMetadataImplementor.class );
		final ProcedureParameterImplementor<?> parameter = mock( ProcedureParameterImplementor.class );
		final JdbcCallParameterRegistration registration = mock( JdbcCallParameterRegistration.class );
		final SharedSessionContractImplementor session = mock( SharedSessionContractImplementor.class );
		final JdbcServices jdbcServices = mock( JdbcServices.class );
		final JdbcEnvironment jdbcEnvironment = mock( JdbcEnvironment.class );
		final JdbcMetadata jdbcMetadata = mock( JdbcMetadata.class );
		final SessionFactoryImplementor factory = mock( SessionFactoryImplementor.class );
		final SessionFactoryOptions options = mock( SessionFactoryOptions.class );

		when( procedureCall.getProcedureName() ).thenReturn( "work" );
		when( procedureCall.getParameterMetadata() ).thenReturn( metadata );
		when( procedureCall.getSession() ).thenReturn( session );
		when( metadata.getRegistrationsAsList() ).thenReturn( (List) List.of( parameter ) );
		when( metadata.getParameterCount() ).thenReturn( 1 );
		when( metadata.hasNamedParameters() ).thenReturn( true );
		when( parameter.getName() ).thenReturn( "value" );
		when( parameter.getMode() ).thenReturn( ParameterMode.IN );
		when( registration.getParameterMode() ).thenReturn( ParameterMode.IN );
		when( registration.getParameterBinder() ).thenReturn( JdbcParameterBinder.NOOP );
		when( parameter.toJdbcParameterRegistration( anyInt(), same( procedureCall ) ) ).thenReturn( registration );
		when( session.getJdbcServices() ).thenReturn( jdbcServices );
		when( jdbcServices.getJdbcEnvironment() ).thenReturn( jdbcEnvironment );
		when( jdbcEnvironment.getDialect() ).thenReturn( mock( Dialect.class ) );
		when( jdbcServices.getJdbcMetadata() ).thenReturn( jdbcMetadata );
		when( jdbcMetadata.supportsNamedParameters() ).thenReturn( true );
		when( session.getFactory() ).thenReturn( factory );
		when( factory.getSessionFactoryOptions() ).thenReturn( options );
		when( options.isPassProcedureParameterNames() ).thenReturn( true );

		return support.interpretCall( procedureCall ).getSqlString();
	}
}

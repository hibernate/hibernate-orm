/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.dialect;

import java.sql.CallableStatement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.ParameterMode;

import org.hibernate.HibernateException;
import org.hibernate.QueryException;
import org.hibernate.boot.spi.SessionFactoryOptions;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.SQLServerDialect;
import org.hibernate.dialect.SpannerPostgreSQLDialect;
import org.hibernate.dialect.type.spi.AbstractPostgreSQLStructJdbcType;
import org.hibernate.engine.jdbc.env.spi.JdbcMetadata;
import org.hibernate.engine.jdbc.env.spi.JdbcEnvironment;
import org.hibernate.engine.jdbc.spi.JdbcServices;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.procedure.spi.CallableStatementSupport;
import org.hibernate.procedure.spi.CallableStatementSupports;
import org.hibernate.procedure.spi.FunctionReturnImplementor;
import org.hibernate.procedure.spi.ProcedureCallImplementor;
import org.hibernate.procedure.spi.ProcedureParameterImplementor;
import org.hibernate.query.spi.ProcedureParameterMetadataImplementor;
import org.hibernate.sql.exec.spi.JdbcCallFunctionReturn;
import org.hibernate.sql.exec.spi.JdbcCallParameterRegistration;
import org.hibernate.sql.exec.spi.JdbcOperationQueryCall;
import org.hibernate.sql.exec.spi.JdbcParameterBinder;
import org.hibernate.type.OutputableType;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Verifies the supported callable-statement strategy facility and stock
/// protocol accessors.
///
/// @author Steve Ebersole
public class CallableStatementSupportsTests {
	@Test
	void stockStrategiesHaveStableIdentity() {
		assertThat( CallableStatementSupports.standard() ).isSameAs( CallableStatementSupports.standard() );
		assertThat( CallableStatementSupports.standardWithRefCursors() )
				.isSameAs( CallableStatementSupports.standardWithRefCursors() );
		assertThat( CallableStatementSupports.db2() ).isSameAs( CallableStatementSupports.db2() );
		assertThat( CallableStatementSupports.postgresql( true ) )
				.isSameAs( CallableStatementSupports.postgresql( true ) );
		assertThat( CallableStatementSupports.postgresql( false ) )
				.isSameAs( CallableStatementSupports.postgresql( false ) );
		assertThat( CallableStatementSupports.sybase() ).isSameAs( CallableStatementSupports.sybase() );
		assertThat( CallableStatementSupports.jtds() ).isSameAs( CallableStatementSupports.jtds() );
	}

	@Test
	void standardBuilderCapturesNamedRendererSnapshot() {
		final var builder = CallableStatementSupports.builder()
				.namedParameterRenderer( (sqlAppender, name) -> {
					sqlAppender.appendSql( name );
					sqlAppender.appendSql( " => ?" );
				} );
		final CallableStatementSupport arrow = builder.build();
		builder.namedParameterRenderer( (sqlAppender, name) -> {
			sqlAppender.appendSql( '@' );
			sqlAppender.appendSql( name );
			sqlAppender.appendSql( " = ?" );
		} );
		final CallableStatementSupport atName = builder.build();

		assertThat( interpret( arrow, procedure( "work", true, true, parameter( "value" ) ) ).getSqlString() )
				.isEqualTo( "{call work(value => ?)}" );
		assertThat( interpret( atName, procedure( "work", true, true, parameter( "value" ) ) ).getSqlString() )
				.isEqualTo( "{call work(@value = ?)}" );
		assertThatIllegalArgumentException()
				.isThrownBy( () -> CallableStatementSupports.builder().namedParameterRenderer( null ) )
				.withMessageContaining( "renderer" );
	}

	@Test
	void standardBuilderDefaultsAndRefCursorSelectionAreReplaceable() {
		assertThat( interpret(
				CallableStatementSupports.builder().build(),
				function( "calculate", Types.INTEGER )
		).getSqlString() ).isEqualTo( "{call calculate()}" );
		assertThat( interpret(
				CallableStatementSupports.builder().supportsRefCursors( true ).build(),
				function( "calculate", Types.INTEGER )
		).getSqlString() ).isEqualTo( "{?=call calculate()}" );
		assertThat( interpret(
				CallableStatementSupports.builder()
						.supportsRefCursors( true )
						.supportsRefCursors( false )
						.build(),
				function( "calculate", Types.INTEGER )
		).getSqlString() ).isEqualTo( "{call calculate()}" );
	}

	@Test
	void namedRendererUsesAllThreeGateConditions() {
		final CallableStatementSupport support = CallableStatementSupports.builder()
				.namedParameterRenderer( (sqlAppender, name) -> {
					sqlAppender.appendSql( name );
					sqlAppender.appendSql( " => ?" );
				} )
				.build();

		assertThat( interpret( support, procedure( "work", false, true, parameter( "value" ) ) ).getSqlString() )
				.isEqualTo( "{call work(?)}" );
		assertThat( interpret( support, procedure( "work", true, false, parameter( "value" ) ) ).getSqlString() )
				.isEqualTo( "{call work(?)}" );
		assertThat( interpret( support, procedure( "work", true, true, parameter( null ) ) ).getSqlString() )
				.isEqualTo( "{call work(?)}" );
		assertThat( interpret(
				CallableStatementSupports.standard(),
				procedure( "work", true, true, parameter( "value" ) )
		).getSqlString() ).isEqualTo( "{call work(?)}" );
		assertThatExceptionOfType( IllegalStateException.class )
				.isThrownBy( () -> interpret(
						CallableStatementSupports.builder()
								.namedParameterRenderer( (sqlAppender, name) -> {
									throw new IllegalStateException( "renderer failure" );
								} )
								.build(),
						procedure( "work", true, true, parameter( "value" ) )
				) )
				.withMessage( "renderer failure" );
	}

	@Test
	void standardProfilesPreserveFunctionReturnAndRefCursorBehavior() {
		assertThat( interpret(
				CallableStatementSupports.standard(),
				procedure( "work", false, false )
		).getSqlString() ).isEqualTo( "{call work()}" );
		assertThat( interpret(
				CallableStatementSupports.standard(),
				procedure( "work", false, false, parameter( null ), parameter( null ) )
		).getSqlString() ).isEqualTo( "{call work(?,?)}" );
		final CallContext explicit = function( "calculate", Types.INTEGER, parameter( "value" ) );
		final JdbcOperationQueryCall explicitCall = interpret(
				CallableStatementSupports.standardWithRefCursors(),
				explicit
		);
		assertThat( explicitCall.getSqlString() ).isEqualTo( "{?=call calculate(?)}" );
		assertThat( explicitCall.getFunctionReturn() ).isSameAs( explicit.jdbcFunctionReturn );
		assertThat( explicit.jdbcPositions ).containsExactly( 2 );

		final CallContext implicit = function( "calculate", Types.INTEGER, parameter( "value" ) );
		final JdbcOperationQueryCall implicitCall = interpret( CallableStatementSupports.standard(), implicit );
		assertThat( implicitCall.getSqlString() ).isEqualTo( "{call calculate(?)}" );
		assertThat( implicitCall.getFunctionReturn() ).isNull();
		assertThat( implicit.jdbcPositions ).containsExactly( 1 );

		assertThatExceptionOfType( QueryException.class )
				.isThrownBy( () -> interpret(
						CallableStatementSupports.standard(),
						procedure( "cursor_work", true, true, parameter( "cursor", ParameterMode.REF_CURSOR ) )
				) );
		assertThat( interpret(
				CallableStatementSupports.standardWithRefCursors(),
				procedure( "cursor_work", true, true, parameter( "cursor", ParameterMode.REF_CURSOR ) )
		).getSqlString() ).isEqualTo( "{call cursor_work(?)}" );
	}

	@Test
	void defaultRegistrationRegistersReturnThenParameters() {
		final JdbcCallFunctionReturn functionReturn = mock( JdbcCallFunctionReturn.class );
		final JdbcCallParameterRegistration first = mock( JdbcCallParameterRegistration.class );
		final JdbcCallParameterRegistration second = mock( JdbcCallParameterRegistration.class );
		final JdbcOperationQueryCall call = mock( JdbcOperationQueryCall.class );
		final CallableStatement statement = mock( CallableStatement.class );
		final SharedSessionContractImplementor session = mock( SharedSessionContractImplementor.class );
		when( call.getFunctionReturn() ).thenReturn( functionReturn );
		when( call.getParameterRegistrations() ).thenReturn( List.of( first, second ) );

		CallableStatementSupports.standard().registerParameters(
				"work",
				call,
				statement,
				mock( ProcedureParameterMetadataImplementor.class ),
				session
		);

		final InOrder order = inOrder( functionReturn, first, second );
		order.verify( functionReturn ).registerParameter( statement, session );
		order.verify( first ).registerParameter( statement, session );
		order.verify( second ).registerParameter( statement, session );
	}

	@Test
	void stockDatabaseProtocolsPreserveTheirSqlForms() {
		assertThat( interpret(
				CallableStatementSupports.db2(),
				procedure( "work", false, false, parameter( null ) )
		).getSqlString() ).isEqualTo( "{call work(?)}" );
		assertThat( interpret(
				CallableStatementSupports.db2(),
				function( "scalar_value", Types.INTEGER, parameter( null ) )
		).getSqlString() ).isEqualTo( "select scalar_value(?) from sysibm.dual" );
		assertThat( interpret(
				CallableStatementSupports.db2(),
				function( "table_value", Types.REF_CURSOR )
		).getSqlString() ).isEqualTo( "select * from table(table_value())" );

		assertThat( interpret(
				CallableStatementSupports.postgresql( true ),
				procedure( "current_proc", true, true, parameter( null ) )
		).getSqlString() ).isEqualTo( "call current_proc(?)" );
		assertThat( interpret(
				CallableStatementSupports.postgresql( false ),
				procedure( "legacy_proc", true, true, parameter( null ) )
		).getSqlString() ).isEqualTo( "{call legacy_proc(?)}" );
		assertThat( interpret(
				CallableStatementSupports.postgresql( true ),
				function( "scalar_value", Types.INTEGER, parameter( "value" ) )
		).getSqlString() ).isEqualTo( "select scalar_value(value => ?)" );
		assertThat( interpret(
				CallableStatementSupports.postgresql( true ),
				function( "table_value", Types.REF_CURSOR )
		).getSqlString() ).isEqualTo( "select * from table_value()" );
		assertThat( interpret(
				CallableStatementSupports.postgresql( false ),
				procedure( "cursor_work", false, false, parameter( null, ParameterMode.REF_CURSOR ) )
		).getSqlString() ).isEqualTo( "{?=call cursor_work()}" );

		final AbstractPostgreSQLStructJdbcType structJdbcType = mock( AbstractPostgreSQLStructJdbcType.class );
		final OutputableType<?> structType = mock( OutputableType.class );
		when( structJdbcType.getStructTypeName() ).thenReturn( "fixture_type" );
		when( structType.getJdbcType() ).thenReturn( structJdbcType );
		assertThat( interpret(
				CallableStatementSupports.postgresql( true ),
				function( "scalar_value", Types.INTEGER, parameter( null, ParameterMode.IN, structType ) )
		).getSqlString() ).isEqualTo( "select scalar_value(cast(? as fixture_type))" );
		assertThatExceptionOfType( HibernateException.class )
				.isThrownBy( () -> interpret(
						CallableStatementSupports.postgresql( true ),
						function(
								"mixed_cursor",
								Types.REF_CURSOR,
								parameter( "cursor", ParameterMode.REF_CURSOR )
						)
				) );
		assertThatExceptionOfType( HibernateException.class )
				.isThrownBy( () -> interpret(
						CallableStatementSupports.postgresql( false ),
						procedure(
								"multiple_cursors",
								false,
								false,
								parameter( null, ParameterMode.REF_CURSOR ),
								parameter( null, ParameterMode.REF_CURSOR )
						)
				) );

		assertThat( interpret(
				CallableStatementSupports.sybase(),
				procedure( "work", false, false, parameter( null ) )
		).getSqlString() ).isEqualTo( "{call work(?)}" );
		assertThat( interpret(
				CallableStatementSupports.sybase(),
				function( "scalar_value", Types.INTEGER, parameter( null ) )
		).getSqlString() ).isEqualTo( "select scalar_value(?) from (select 1) t1(c1)" );
		assertThat( interpret(
				CallableStatementSupports.jtds(),
				procedure( "work", false, false, parameter( "value" ) )
		).getSqlString() ).isEqualTo( "{call work(@value=?)}" );
		assertThatExceptionOfType( QueryException.class )
				.isThrownBy( () -> interpret(
						CallableStatementSupports.jtds(),
						function( "unsupported", Types.INTEGER )
				) );
		assertThatExceptionOfType( QueryException.class )
				.isThrownBy( () -> interpret(
						CallableStatementSupports.jtds(),
						procedure( "unsupported", false, false, parameter( null, ParameterMode.REF_CURSOR ) )
				) );
	}

	@Test
	void maintainedDialectsRetainTheirConfiguredStrategies() {
		final OracleDialect oracle = new OracleDialect();
		assertThat( oracle.getCallableStatementSupport() ).isSameAs( oracle.getCallableStatementSupport() );
		assertThat( interpret(
				oracle.getCallableStatementSupport(),
				procedure( "work", true, true, parameter( "value" ) )
		).getSqlString() ).isEqualTo( "{call work(value => ?)}" );

		final SQLServerDialect sqlServer = new SQLServerDialect();
		assertThat( sqlServer.getCallableStatementSupport() ).isSameAs( sqlServer.getCallableStatementSupport() );
		assertThat( interpret(
				sqlServer.getCallableStatementSupport(),
				procedure( "work", true, true, parameter( "value" ) )
		).getSqlString() ).isEqualTo( "{call work(@value = ?)}" );

		assertThat( new HANADialect().getCallableStatementSupport() )
				.isSameAs( CallableStatementSupports.standardWithRefCursors() );
		assertThat( new SpannerPostgreSQLDialect().getCallableStatementSupport() )
				.isSameAs( CallableStatementSupports.standard() );
	}

	private static JdbcOperationQueryCall interpret(CallableStatementSupport support, CallContext context) {
		return support.interpretCall( context.procedureCall );
	}

	private static CallContext procedure(
			String name,
			boolean supportsNamedParameters,
			boolean passParameterNames,
			ParameterSpec... parameterSpecs) {
		return call( name, supportsNamedParameters, passParameterNames, null, 0, parameterSpecs );
	}

	private static CallContext function(String name, int jdbcTypeCode, ParameterSpec... parameterSpecs) {
		return call( name, true, true, mock( FunctionReturnImplementor.class ), jdbcTypeCode, parameterSpecs );
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static CallContext call(
			String name,
			boolean supportsNamedParameters,
			boolean passParameterNames,
			FunctionReturnImplementor<?> functionReturn,
			int functionJdbcTypeCode,
			ParameterSpec... parameterSpecs) {
		final ProcedureCallImplementor<?> procedureCall = mock( ProcedureCallImplementor.class );
		final ProcedureParameterMetadataImplementor metadata = mock( ProcedureParameterMetadataImplementor.class );
		final SharedSessionContractImplementor session = mock( SharedSessionContractImplementor.class );
		final JdbcServices jdbcServices = mock( JdbcServices.class );
		final JdbcEnvironment jdbcEnvironment = mock( JdbcEnvironment.class );
		final JdbcMetadata jdbcMetadata = mock( JdbcMetadata.class );
		final SessionFactoryImplementor factory = mock( SessionFactoryImplementor.class );
		final SessionFactoryOptions options = mock( SessionFactoryOptions.class );
		final List<ProcedureParameterImplementor<?>> parameters = new ArrayList<>();
		final List<Integer> jdbcPositions = new ArrayList<>();

		when( procedureCall.getProcedureName() ).thenReturn( name );
		when( procedureCall.getParameterMetadata() ).thenReturn( metadata );
		when( procedureCall.getSession() ).thenReturn( session );
		when( procedureCall.getFunctionReturn() ).thenReturn( (FunctionReturnImplementor) functionReturn );
		when( session.getJdbcServices() ).thenReturn( jdbcServices );
		when( jdbcServices.getJdbcEnvironment() ).thenReturn( jdbcEnvironment );
		when( jdbcEnvironment.getDialect() ).thenReturn( mock( Dialect.class ) );
		when( jdbcServices.getJdbcMetadata() ).thenReturn( jdbcMetadata );
		when( jdbcMetadata.supportsNamedParameters() ).thenReturn( supportsNamedParameters );
		when( session.getFactory() ).thenReturn( factory );
		when( factory.getSessionFactoryOptions() ).thenReturn( options );
		when( options.isPassProcedureParameterNames() ).thenReturn( passParameterNames );

		for ( ParameterSpec parameterSpec : parameterSpecs ) {
			final ProcedureParameterImplementor<?> parameter = mock( ProcedureParameterImplementor.class );
			final JdbcCallParameterRegistration registration = mock( JdbcCallParameterRegistration.class );
			when( parameter.getName() ).thenReturn( parameterSpec.name );
			when( parameter.getMode() ).thenReturn( parameterSpec.mode );
			when( registration.getParameterMode() ).thenReturn( parameterSpec.mode );
			when( registration.getParameterBinder() ).thenReturn( JdbcParameterBinder.NOOP );
			doReturn( parameterSpec.type ).when( registration ).getParameterType();
			when( parameter.toJdbcParameterRegistration( anyInt(), same( procedureCall ) ) ).thenAnswer( invocation -> {
				jdbcPositions.add( invocation.getArgument( 0 ) );
				return registration;
			} );
			parameters.add( parameter );
		}
		when( metadata.getRegistrationsAsList() ).thenReturn( (List) parameters );
		when( metadata.getParameterCount() ).thenReturn( parameters.size() );
		when( metadata.hasNamedParameters() )
				.thenReturn( List.of( parameterSpecs ).stream().anyMatch( parameter -> parameter.name != null ) );

		JdbcCallFunctionReturn jdbcFunctionReturn = null;
		if ( functionReturn != null ) {
			jdbcFunctionReturn = mock( JdbcCallFunctionReturn.class );
			when( functionReturn.getJdbcTypeCode() ).thenReturn( functionJdbcTypeCode );
			when( functionReturn.toJdbcFunctionReturn( session ) ).thenReturn( jdbcFunctionReturn );
		}

		return new CallContext( procedureCall, jdbcFunctionReturn, jdbcPositions );
	}

	private static ParameterSpec parameter(String name) {
		return parameter( name, ParameterMode.IN );
	}

	private static ParameterSpec parameter(String name, ParameterMode mode) {
		return parameter( name, mode, null );
	}

	private static ParameterSpec parameter(String name, ParameterMode mode, OutputableType<?> type) {
		return new ParameterSpec( name, mode, type );
	}

	private record ParameterSpec(String name, ParameterMode mode, OutputableType<?> type) {
	}

	private record CallContext(
			ProcedureCallImplementor<?> procedureCall,
			JdbcCallFunctionReturn jdbcFunctionReturn,
			List<Integer> jdbcPositions) {
	}
}

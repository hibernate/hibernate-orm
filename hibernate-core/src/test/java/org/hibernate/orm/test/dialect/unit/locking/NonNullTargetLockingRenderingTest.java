package org.hibernate.orm.test.dialect.unit.locking;

import java.util.stream.Stream;

import org.hibernate.LockMode;
import org.hibernate.Locking;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.lock.spi.LockingSupport;
import org.hibernate.dialect.lock.spi.OuterJoinLockingType;
import org.hibernate.dialect.sql.ast.spi.SqlAstTranslationRequest;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.internal.QueryOptionsImpl;
import org.hibernate.sql.exec.internal.JdbcSelectWithActions;
import org.hibernate.sql.exec.spi.JdbcLockingApplication;
import org.hibernate.sql.exec.spi.JdbcParameterBindings;
import org.hibernate.type.spi.TypeConfiguration;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.AdditionalAnswers.delegatesTo;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/// Verifies that clause rendering and final operation disposition agree for the
/// new restriction and retain the behavior of existing capability values.
///
/// @author Steve Ebersole
class NonNullTargetLockingRenderingTest {
	static Stream<Arguments> capabilitiesAndPolicies() {
		return Stream.of( OuterJoinLockingType.values() ).flatMap( type -> Stream.of( Locking.FollowOn.values() )
				.map( policy -> arguments( type, policy ) ) );
	}

	@ParameterizedTest
	@MethodSource("capabilitiesAndPolicies")
	void preservesPoliciesAndCapabilityMeanings(OuterJoinLockingType type, Locking.FollowOn policy) {
		final var dialect = mock( PostgreSQLDialect.class, delegatesTo( new PostgreSQLDialect() ) );
		final var original = dialect.getLockingSupport();
		final var support = mock( LockingSupport.class, delegatesTo( original ) );
		final var metadata = mock( LockingSupport.Metadata.class, delegatesTo( original.getMetadata() ) );
		doReturn( type ).when( metadata ).getOuterJoinLockingType();
		doReturn( metadata ).when( support ).getMetadata();
		doReturn( support ).when( dialect ).getLockingSupport();
		final var factory = mock( SessionFactoryImplementor.class, RETURNS_DEEP_STUBS );
		when( factory.getJdbcServices().getDialect() ).thenReturn( dialect );
		when( factory.getTypeConfiguration() ).thenReturn( new TypeConfiguration() );
		when( factory.getJdbcSelectWithActionsBuilder() ).thenAnswer( call -> new JdbcSelectWithActions.Builder() );
		final var f = new NonNullLockTargetCheckerTest.Fixture( factory );
		final var options = new QueryOptionsImpl();
		options.getLockOptions().setLockMode( LockMode.PESSIMISTIC_WRITE ).setFollowOnStrategy( policy );
		final var translator = dialect.getSqlAstTranslatorFactory().buildTranslator( new SqlAstTranslationRequest.Select( factory, f.statement ) );
		if ( type == OuterJoinLockingType.UNSUPPORTED && policy == Locking.FollowOn.DISALLOW ) {
			assertThatThrownBy( () -> translator.translate( JdbcParameterBindings.NO_BINDINGS, options ) )
					.isInstanceOf( org.hibernate.query.IllegalQueryOperationException.class );
			return;
		}
		final var operation = translator.translate( JdbcParameterBindings.NO_BINDINGS, options );
		if ( policy == Locking.FollowOn.FORCE || type == OuterJoinLockingType.UNSUPPORTED && policy == Locking.FollowOn.ALLOW ) {
			assertThat( operation.getLockingApplication() ).isEqualTo( JdbcLockingApplication.FOLLOW_ON );
			assertThat( operation.getSqlString() ).doesNotContain( " for " );
		}
		else if ( type == OuterJoinLockingType.UNSUPPORTED && policy == Locking.FollowOn.IGNORE ) {
			assertThat( operation.getLockingApplication() ).isEqualTo( JdbcLockingApplication.RENDERED );
			assertThat( operation.getSqlString() ).doesNotContain( " for " );
		}
		else {
			assertThat( operation.getLockingApplication() ).isEqualTo( JdbcLockingApplication.RENDERED );
			assertThat( operation.getSqlString() ).containsPattern( "for (?:no key )?update of p$" );
			assertThat( operation.getLoadedValuesCollectorFactory() ).isNull();
		}
	}
}

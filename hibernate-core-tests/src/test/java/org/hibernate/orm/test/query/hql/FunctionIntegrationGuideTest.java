package org.hibernate.orm.test.query.hql;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.FunctionContributor;
import org.hibernate.query.sqm.produce.function.FunctionArgumentException;
import org.hibernate.type.StandardBasicTypes;

import org.hibernate.testing.orm.junit.BootstrapServiceRegistry;
import org.hibernate.testing.orm.junit.BootstrapServiceRegistry.JavaService;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hibernate.query.sqm.produce.function.FunctionParameterType.STRING;

/// Verifies the scalar registration and invocation shown in Function integration.
///
/// @author Steve Ebersole
@BootstrapServiceRegistry(javaServices = @JavaService(
		role = FunctionContributor.class,
		impl = FunctionIntegrationGuideTest.GuideFunctions.class
))
@DomainModel
@SessionFactory
class FunctionIntegrationGuideTest {
	// tag::function-integration-scalar-contributor[]
	public static class GuideFunctions implements FunctionContributor {
		@Override
		public void contributeFunctions(FunctionContributions contributions) {
			var stringType = contributions.getTypeConfiguration()
					.getBasicTypeRegistry().resolve( StandardBasicTypes.STRING );
			contributions.getFunctionRegistry()
					.namedDescriptorBuilder( "guide_lower", "lower" )
					.setExactArgumentCount( 1 )
					.setParameterTypes( STRING )
					.setInvariantType( stringType )
					.register();
		}
	}
	// end::function-integration-scalar-contributor[]

	@Test
	void scalarInvocation(SessionFactoryScope scope) {
		scope.inTransaction( manager -> {
			// tag::function-integration-scalar-invocation[]
			String result = manager.createQuery( "select guide_lower(:text)", String.class )
					.setParameter( "text", "Hibernate" )
					.getSingleResult();
			assertThat( result ).isEqualTo( "hibernate" );
			// end::function-integration-scalar-invocation[]
		} );
	}

	@Test
	void argumentCountIsValidated(SessionFactoryScope scope) {
		scope.inTransaction( manager -> assertThatThrownBy(
				() -> manager.createQuery( "select guide_lower('a', 'b')", String.class ) )
				.isInstanceOf( IllegalArgumentException.class )
				.hasCauseInstanceOf( FunctionArgumentException.class ) );
	}

	@Test
	void argumentTypeIsValidated(SessionFactoryScope scope) {
		scope.inTransaction( manager -> assertThatThrownBy(
				() -> manager.createQuery( "select guide_lower(42)", String.class ) )
				.isInstanceOf( IllegalArgumentException.class )
				.hasCauseInstanceOf( FunctionArgumentException.class ) );
	}
}

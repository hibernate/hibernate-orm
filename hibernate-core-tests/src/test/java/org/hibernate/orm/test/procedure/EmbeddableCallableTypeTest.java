package org.hibernate.orm.test.procedure;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ParameterMode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/// An embeddable without a single-column aggregate mapping cannot be a
/// callable parameter or function return, and is rejected at registration.
///
/// @author Steve Ebersole
@DomainModel(annotatedClasses = EmbeddableCallableTypeTest.Row.class)
@SessionFactory
class EmbeddableCallableTypeTest {
	@Test
	void multiColumnEmbeddableIsRejected(SessionFactoryScope scope) {
		scope.inSession( session -> {
			final var type = session.getMetamodel().embeddable( Pair.class );
			try ( var call = session.createStoredProcedureCall( "callable_probe" ) ) {
				assertThatThrownBy( () -> call.registerParameter( 1, type, ParameterMode.IN ) )
						.isInstanceOf( IllegalArgumentException.class )
						.hasMessageContaining( "callable_probe" )
						.hasMessageContaining( "parameter 1" )
						.hasMessageContaining( "single-column" );
				assertThatThrownBy( () -> call.registerParameter( "input", type, ParameterMode.IN ) )
						.isInstanceOf( IllegalArgumentException.class )
						.hasMessageContaining( "parameter input" );
				assertThatThrownBy( () -> call.markAsFunctionCall( type ) )
						.isInstanceOf( IllegalArgumentException.class )
						.hasMessageContaining( "function return" );
			}
		} );
	}

	@Entity(name = "CallableProbeRow")
	static class Row {
		@Id int id;
		Pair pair;
	}

	@Embeddable
	static class Pair {
		int first;
		int second;
	}
}

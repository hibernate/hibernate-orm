package org.hibernate.orm.test.query.named;

import java.util.stream.Stream;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Static queries of repository methods which return {@code CompletionStage}
 * are bound as if they returned the wrapped type.
 */
@DomainModel( annotatedClasses = { Book.class, AsynchronousRepository.class } )
@SessionFactory
class AsynchronousStaticQueryRegistrationTest {

	static Stream<String> methodNames() {
		return Stream.of( "stageByTitle", "stageListByTitle", "stageOptionalByTitle" );
	}

	@ParameterizedTest
	@MethodSource( "methodNames" )
	void infersResultTypeAndFromClauseFromWrappedType(String methodName, SessionFactoryScope scope) {
		final var memento = scope.getSessionFactory()
				.getQueryEngine()
				.getNamedObjectRepository()
				.getSelectionQueryMemento(
						AsynchronousRepository.class.getCanonicalName() + "#" + methodName + "(java.lang.String)" );
		assertThat( memento ).isNotNull();
		assertThat( memento.getResultType() ).isEqualTo( Book.class );
		assertThat( memento.getSelectionString() )
				.isEqualTo( "from Jpa4StaticQueryBook where title = :title" );
	}
}

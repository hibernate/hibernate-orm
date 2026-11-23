package org.hibernate.orm.test.query.named;

import java.util.List;
import java.util.stream.Stream;

import jakarta.persistence.query.StaticTypedQueryReference;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Static queries of repository methods which return {@code Uni}
 * are bound as if they returned the wrapped type.
 */
@DomainModel( annotatedClasses = { Book.class, ReactiveRepository.class } )
@SessionFactory
class ReactiveStaticQueryRegistrationTest {

	static Stream<String> methodNames() {
		return Stream.of( "uniByTitle", "uniListByTitle", "uniPageByTitle",
				"uniCursoredPageByTitle", "uniKeyedResultListByTitle" );
	}

	@ParameterizedTest
	@MethodSource( "methodNames" )
	void infersResultTypeAndFromClauseFromWrappedType(String methodName, SessionFactoryScope scope) {
		final var memento = scope.getSessionFactory()
				.getQueryEngine()
				.getNamedObjectRepository()
				.getSelectionQueryMemento( queryName( methodName ) );
		assertThat( memento ).isNotNull();
		assertThat( memento.getResultType() ).isEqualTo( Book.class );
		assertThat( memento.getSelectionString() )
				.startsWith( "from Jpa4StaticQueryBook where title = :title" );
	}

	@Test
	void infersElementTypeOfArrayWrappedInUni(SessionFactoryScope scope) {
		final var memento = scope.getSessionFactory()
				.getQueryEngine()
				.getNamedObjectRepository()
				.getSelectionQueryMemento( queryName( "uniTitlesByTitle" ) );
		assertThat( memento ).isNotNull();
		assertThat( memento.getResultType() ).isEqualTo( String.class );
	}

	@Test
	void executesQueryByStaticReference(SessionFactoryScope scope) {
		scope.inTransaction( session -> session.persist( new Book( 1, "Async" ) ) );
		scope.inTransaction( session -> {
			final var reference = new StaticTypedQueryReference<>(
					queryName( "uniByTitle" ),
					ReactiveRepository.class,
					"uniByTitle",
					Book.class,
					List.of( String.class ),
					List.of( "title" ),
					List.of( "Async" )
			);
			assertThat( session.createQuery( reference ).getSingleResult().getTitle() )
					.isEqualTo( "Async" );
		} );
	}

	private static String queryName(String methodName) {
		final String parameters = switch ( methodName ) {
			case "uniPageByTitle", "uniCursoredPageByTitle" -> "java.lang.String,jakarta.data.page.PageRequest";
			case "uniKeyedResultListByTitle" -> "java.lang.String,org.hibernate.query.KeyedPage";
			default -> "java.lang.String";
		};
		return ReactiveRepository.class.getCanonicalName() + "#" + methodName + "(" + parameters + ")";
	}
}

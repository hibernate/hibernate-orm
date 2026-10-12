package org.hibernate.orm.test.query.named;

import java.util.Optional;
import java.util.stream.Stream;

import io.smallrye.mutiny.Uni;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.query.JakartaQuery;
import org.hibernate.AnnotationException;
import org.hibernate.annotations.processing.Exclude;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.testing.orm.junit.BaseUnitTest;
import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Only a result, a {@code List}, a {@code KeyedResultList}, a {@code Page} or a {@code CursoredPage}
 * may be wrapped in {@code Uni}
 * as the return type of a static query method.
 */
@BaseUnitTest
@Exclude
class UnsupportedReactiveStaticQueryTest {

	@ParameterizedTest
	@ValueSource( classes = {
			OptionalRepository.class,
			StreamRepository.class,
			TypedQueryRepository.class
	} )
	void rejectsUnsupportedContainerWrappedInUni(Class<?> repository) {
		final var serviceRegistry = ServiceRegistryUtil.serviceRegistry();
		try {
			final var exception = assertThrows(
					AnnotationException.class,
					() -> new MetadataSources( serviceRegistry )
							.addAnnotatedClass( Book.class )
							.addAnnotatedClass( repository )
							.buildMetadata()
			);
			assertThat( exception.getMessage() )
					.contains( "wrapped in 'Uni'" )
					.contains( repository.getName() );
		}
		finally {
			StandardServiceRegistryBuilder.destroy( serviceRegistry );
		}
	}

	@Exclude
	interface OptionalRepository {
		@JakartaQuery( "from Jpa4StaticQueryBook where title = :title" )
		Uni<Optional<Book>> byTitle(String title);
	}

	@Exclude
	interface StreamRepository {
		@JakartaQuery( "from Jpa4StaticQueryBook where title = :title" )
		Uni<Stream<Book>> byTitle(String title);
	}

	@Exclude
	interface TypedQueryRepository {
		@JakartaQuery( "from Jpa4StaticQueryBook where title = :title" )
		Uni<TypedQuery<Book>> byTitle(String title);
	}
}

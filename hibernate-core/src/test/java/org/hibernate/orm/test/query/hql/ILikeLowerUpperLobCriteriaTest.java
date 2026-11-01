package org.hibernate.orm.test.query.hql;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Jpa(annotatedClasses = ILikeLowerUpperLobCriteriaTest.Text.class)
@JiraKey("HHH-18954")
class ILikeLowerUpperLobCriteriaTest {

	@Test
	void lowerAndLikeOnLob(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final var entity = new Text();
			entity.id = 1L;
			entity.text = "Aaaa aaaa ".repeat( 450 );
			entityManager.persist( entity );
		} );

		scope.inTransaction( entityManager -> {
			var criteriaBuilder = entityManager.getCriteriaBuilder();
			var criteriaQuery = criteriaBuilder.createQuery( Text.class );
			var root = criteriaQuery.from( Text.class );
			criteriaQuery.where( criteriaBuilder.like( criteriaBuilder.lower( root.get( "text" ) ), "%aaaa aaaa%" ) );

			List<Text> result = entityManager.createQuery( criteriaQuery ).getResultList();
			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).id ).isEqualTo( 1L );
		} );
	}

	@Test
	void upperAndLikeOnLob(EntityManagerFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			final var entity = new Text();
			entity.id = 2L;
			entity.text = "Bbbb bbbb ".repeat( 450 );
			entityManager.persist( entity );
		} );

		scope.inTransaction( entityManager -> {
			var criteriaBuilder = entityManager.getCriteriaBuilder();
			var criteriaQuery = criteriaBuilder.createQuery( Text.class );
			var root = criteriaQuery.from( Text.class );
			criteriaQuery.where( criteriaBuilder.like( criteriaBuilder.upper( root.get( "text" ) ), "%BBBB BBBB%" ) );

			List<Text> result = entityManager.createQuery( criteriaQuery ).getResultList();
			assertThat( result ).hasSize( 1 );
			assertThat( result.get( 0 ).id ).isEqualTo( 2L );
		} );
	}

	@Entity(name = "Text")
	static class Text {
		@Id
		private Long id;

		@Lob
		private String text;
	}
}

package org.hibernate.orm.test.embeddable;

import java.util.List;

import org.hibernate.jpa.SpecHints;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Jpa(
		annotatedClasses = {
				ElementCollectionLazyToOneTest.TheEntity.class
		}
)
@JiraKey( "HHH-17383" )
public class ElementCollectionLazyToOneTest {

	private static final Long ENTITY_1 = 1L;
	private static final Long ENTITY_2 = 2L;

	@BeforeAll
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					TheEntity e2 = new TheEntity( ENTITY_2, "e2" );
					TheEntity e1 = new TheEntity( ENTITY_1, "e1" );
					TheEmbeddable embeddable = new TheEmbeddable();
					embeddable.setContent( "abc" );
					embeddable.setEntity( e2 );
					e1.setEmbeddables( List.of( embeddable ) );
					entityManager.persist( e2 );
					entityManager.persist( e1 );
				}
		);
	}

	@Test
	public void testInitializingCollection(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					TheEntity entity = entityManager.find( TheEntity.class, ENTITY_1 );
					entityManager.createQuery( "from TheEntity e left join fetch e.embeddables" ).getResultList();

					List<TheEmbeddable> embeddables = entity.getEmbeddables();
					assertThat( embeddables.size() ).isEqualTo( 1 );
					TheEmbeddable theEmbeddable = embeddables.get( 0 );
					assertNotNull( theEmbeddable );
					assertThat( theEmbeddable.getEntity() ).isNotNull();
				}
		);
	}

	@Embeddable
	public static class TheEmbeddable {
		@Column
		private String content;
		@ManyToOne(fetch = FetchType.LAZY)
		private TheEntity entity;

		public String getContent() {
			return content;
		}

		public void setContent(String content) {
			this.content = content;
		}

		public TheEntity getEntity() {
			return entity;
		}

		public void setEntity(TheEntity entity) {
			this.entity = entity;
		}
	}

	@Entity(name = "TheEntity")
	public static class TheEntity {
		@Id
		private Long id;
		private String name;

		@ElementCollection
		private List<TheEmbeddable> embeddables;

		public TheEntity() {
		}

		public TheEntity(Long id, String name) {
			this.id = id;
			this.name = name;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public List<TheEmbeddable> getEmbeddables() {
			return embeddables;
		}

		public void setEmbeddables(List<TheEmbeddable> embeddables) {
			this.embeddables = embeddables;
		}
	}

}

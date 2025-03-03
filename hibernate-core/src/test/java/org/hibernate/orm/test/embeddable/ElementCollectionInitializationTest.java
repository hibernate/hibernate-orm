/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.embeddable;

import java.util.List;

import org.hibernate.jpa.SpecHints;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedAttributeNode;
import jakarta.persistence.NamedEntityGraph;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Jpa(
		annotatedClasses = {
				ElementCollectionInitializationTest.DemoEntity.class
		}
)
@JiraKey( "HHH-17256" )
public class ElementCollectionInitializationTest {

	private static final Long ENTITY_ID = 1l;
	private static final String DEMO_CONTENT = "content";

	@BeforeAll
	public void setUp(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					DemoEntity entity = new DemoEntity( ENTITY_ID );
					DemoEmbeddable embeddable = new DemoEmbeddable();
					embeddable.setContent( DEMO_CONTENT );
					entity.setEmbeddables( List.of( embeddable ) );
					entityManager.persist( entity );
				}
		);
	}

	@Test
	public void testInitializingCollection(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					DemoEntity entity = entityManager.find( DemoEntity.class, ENTITY_ID );

					CriteriaBuilder criteriaBuilder = entityManager.getCriteriaBuilder();
					CriteriaQuery<DemoEntity> criteriaQuery = criteriaBuilder.createQuery( DemoEntity.class );
					Root<DemoEntity> root = criteriaQuery.from( DemoEntity.class );
					criteriaQuery.where( criteriaBuilder.equal( root.get( "id" ), ENTITY_ID ) );

					TypedQuery<DemoEntity> query = entityManager.createQuery( criteriaQuery ).setHint(
							SpecHints.HINT_SPEC_FETCH_GRAPH,
							entityManager.getEntityGraph( "embeddables" )
					);

					query.getResultList();
					List<DemoEmbeddable> embeddables = entity.getEmbeddables();
					assertThat( embeddables.size() ).isEqualTo( 1 );
					DemoEmbeddable demoEmbeddable = embeddables.get( 0 );
					assertNotNull( demoEmbeddable );
					assertThat( demoEmbeddable.getContent() ).isEqualTo( DEMO_CONTENT );
				}
		);
	}

	@Embeddable
	public static class DemoEmbeddable {
		@Column
		private String content;

		public String getContent() {
			return content;
		}

		public void setContent(String content) {
			this.content = content;
		}

		@Override
		public String toString() {
			return "DemoEmbeddable(" + content + ")";
		}
	}

	@Entity(name = "DemoEntity")

	@NamedEntityGraph(
			name = "embeddables",
			attributeNodes = {
					@NamedAttributeNode("embeddables"),
			})
	public static class DemoEntity {
		@Id
		private Long id;

		@ElementCollection
		@CollectionTable
		private List<DemoEmbeddable> embeddables;

		public DemoEntity() {
		}

		public DemoEntity(Long id) {
			this.id = id;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public List<DemoEmbeddable> getEmbeddables() {
			return embeddables;
		}

		public void setEmbeddables(List<DemoEmbeddable> embeddables) {
			this.embeddables = embeddables;
		}
	}

}

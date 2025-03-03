/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.orphan.onetoone;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.Jira;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@SessionFactory
@DomainModel(annotatedClasses = {
		OneToOneLazyOrphanRemovalEmbeddedTest.OwnerEntity.class,
		OneToOneLazyOrphanRemovalEmbeddedTest.ToOneContainer.class,
		OneToOneLazyOrphanRemovalEmbeddedTest.ChildEntity.class
})
@Jira("https://hibernate.atlassian.net/browse/HHH-9663")
public class OneToOneLazyOrphanRemovalEmbeddedTest {
	@Test
	public void test(SessionFactoryScope scope) {
		// set association to null, should trigger orphan removal
		scope.inTransaction( session -> {
			final OwnerEntity owner = session.find( OwnerEntity.class, 1 );
			final ToOneContainer container = owner.getContainer();
			assertThat( container.getChild() ).isNotNull();
			container.setChild( null );
			session.merge( owner );

			assertThat( session.find( OwnerEntity.class, 1 ).getContainer() ).isNotNull();
		} );

		// check orphan removal removed the entity correctly
		scope.inTransaction( session -> {
			final OwnerEntity owner = session.find( OwnerEntity.class, 1 );
			final ToOneContainer container = owner.getContainer();
			assertThat( container.getChild() ).isNull();
			assertThat( session.find( ChildEntity.class, 1 ) ).isNull();
		} );
	}

	@BeforeAll
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			final ChildEntity child = new ChildEntity( 1L, "child_entity" );
			final ToOneContainer container = new ToOneContainer( child, 1 );
			final OwnerEntity raceDriver = new OwnerEntity( 1L, container );
			session.persist( child );
			session.persist( raceDriver );
		} );
	}

	@AfterAll
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncateMappedObjects();
	}

	@Entity(name = "OwnerEntity")
	static class OwnerEntity {
		@Id
		private Long id;

		@Embedded
		private ToOneContainer container;

		public OwnerEntity() {
		}

		public OwnerEntity(Long id, ToOneContainer container) {
			this.id = id;
			this.container = container;
		}

		public ToOneContainer getContainer() {
			return container;
		}
	}

	@Embeddable
	static class ToOneContainer {
		@OneToOne(orphanRemoval = true, fetch = FetchType.LAZY)
		private ChildEntity child;

		private Integer countValue;

		public ToOneContainer() {
		}

		public ToOneContainer(ChildEntity child, Integer countValue) {
			this.child = child;
			this.countValue = countValue;
		}

		public ChildEntity getChild() {
			return child;
		}

		public void setChild(ChildEntity child) {
			this.child = child;
		}
	}

	@Entity(name = "Engine")
	public static class ChildEntity {
		@Id
		private Long id;

		private String name;

		public ChildEntity() {
		}

		public ChildEntity(Long id, String name) {
			this.id = id;
			this.name = name;
		}
	}
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.entitygraph;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import org.hibernate.graph.RootGraph;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.hamcrest.InitializationCheckMatcher.isInitialized;

@DomainModel(
		annotatedClasses = {
				FetchWithRootGraphTest.SimpleEntity.class,
				FetchWithRootGraphTest.EntityWithReference.class
		}
)
@SessionFactory
public class FetchWithRootGraphTest {

	@BeforeEach
	void before(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					for ( long i = 0; i < 10; ++i ) {
						SimpleEntity sim = new SimpleEntity( i, "Entity #" + i );
						EntityWithReference ref = new EntityWithReference( i, sim );
						session.persist( sim );
						session.persist( ref );
					}
				}
		);
	}

	@AfterEach
	void after(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	@JiraKey(value = "HHH-13312")
	void hhh13312Test(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					RootGraph<EntityWithReference> g = session.createEntityGraph( EntityWithReference.class );
					g.addAttributeNode( "reference" );

					EntityWithReference single = session.byId( EntityWithReference.class )
							.with( g )
							.load( 3L );

					assertThat( single.getId(), is( 3L ) );
					assertThat( single.getReference(), isInitialized() );
				}
		);
	}

	@Entity(name = "SimpleEntity")
	@Table(name = "SimpleEntity")
	static class SimpleEntity {

		@Id
		private Long id;

		private String text;

		public SimpleEntity() {
		}

		public SimpleEntity(Long id, String text) {
			this.id = id;
			this.text = text;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getText() {
			return text;
		}

		public void setText(String text) {
			this.text = text;
		}
	}

	@Entity(name = "EntityWithReference")
	@Table(name = "EntityWithReference")
	static class EntityWithReference {

		@Id
		private Long id;

		@OneToOne(fetch = FetchType.LAZY)
		private SimpleEntity reference;

		public EntityWithReference() {
		}

		public EntityWithReference(Long id, SimpleEntity ref) {
			this.id = id;
			this.reference = ref;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public SimpleEntity getReference() {
			return reference;
		}

		public void setReference(SimpleEntity reference) {
			this.reference = reference;
		}
	}
}

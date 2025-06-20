/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.exec.manytoone;

import static org.assertj.core.api.Assertions.assertThat;

import org.hibernate.Hibernate;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@DomainModel(
		annotatedClasses = {
				EntityWithLazyManyToOneTest.ConcreteEntity.class,
				EntityWithLazyManyToOneTest.LazyAbstractEntityReference.class,
				EntityWithLazyManyToOneTest.LazyConcreteEntityReference.class
		}
)
@ServiceRegistry
@SessionFactory
@JiraKey( "HHH-16613" )
public class EntityWithLazyManyToOneTest {

	private Integer abstractEntityReferenceId;
	private Integer concreteEntityReferenceId;

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction( session -> {
			ConcreteEntity entity = new ConcreteEntity();
			session.persist( entity );

			LazyAbstractEntityReference reference = new LazyAbstractEntityReference( entity );
			session.persist( reference );

			this.abstractEntityReferenceId = reference.getId();

			LazyConcreteEntityReference concreteReference = new LazyConcreteEntityReference( entity );
			session.persist( concreteReference );

			this.concreteEntityReferenceId = concreteReference.getId();
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void containsLazyAbstractEntity(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					LazyAbstractEntityReference reference = session.get(
							LazyAbstractEntityReference.class, abstractEntityReferenceId );
					assertThat( reference ).isNotNull();
					assertThat( Hibernate.isInitialized( reference.getEntity() ) ).isFalse();
				}
		);
	}

	@Test
	public void containsLazyConcreteEntity(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					LazyConcreteEntityReference reference = session.get(
							LazyConcreteEntityReference.class, concreteEntityReferenceId );
					assertThat( reference ).isNotNull();
					assertThat( Hibernate.isInitialized( reference.getEntity() ) ).isFalse();
				}
		);
	}

	@Entity(name = "AbstractEntity")
	@Cacheable // NOTE that all these entities are cacheable - if removed from the entities both tests above - pass.
	public static abstract class AbstractEntity {

		@Id
		@GeneratedValue
		private Integer id;

		public AbstractEntity() {
			super();
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}
	}

	@Entity(name = "ConcreteEntity")
	@Cacheable
	public static class ConcreteEntity extends AbstractEntity {

		private String content = "text";

		public ConcreteEntity() {
			super();
		}

		public String getContent() {
			return content;
		}

		public void setContent(String content) {
			this.content = content;
		}
	}

	@Entity(name = "LazyAbstractEntityReference")
	@Table(name = "lazy_abstract_ent_ref")
	@Cacheable
	public static class LazyAbstractEntityReference {

		@Id
		@GeneratedValue
		private Integer id;

		@ManyToOne(fetch = FetchType.LAZY)
		private AbstractEntity entity;

		public LazyAbstractEntityReference() {
		}

		public LazyAbstractEntityReference(AbstractEntity entity) {
			setEntity( entity );
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public AbstractEntity getEntity() {
			return entity;
		}

		public void setEntity(AbstractEntity entity) {
			this.entity = entity;
		}
	}

	@Entity(name = "LazyConcreteEntityReference")
	@Table(name = "lazy_concrete_ent_ref")
	@Cacheable
	public static class LazyConcreteEntityReference {

		@Id
		@GeneratedValue
		private Integer id;

		@ManyToOne(fetch = FetchType.LAZY)
		private ConcreteEntity entity;

		public LazyConcreteEntityReference() {
		}

		public LazyConcreteEntityReference(ConcreteEntity entity) {
			setEntity( entity );
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public ConcreteEntity getEntity() {
			return entity;
		}

		public void setEntity(ConcreteEntity entity) {
			this.entity = entity;
		}
	}
}

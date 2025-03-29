/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.sql.exec.manytoone;

import org.hibernate.Hibernate;

import org.hibernate.testing.orm.junit.DomainModel;
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

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = {
				EntityWithEagerManyToOneTest.ConcreteEntity.class,
				EntityWithEagerManyToOneTest.LazyAbstractEntityReference.class,
				EntityWithEagerManyToOneTest.LazyConcreteEntityReference.class
		}
)
@ServiceRegistry
@SessionFactory
public class EntityWithEagerManyToOneTest {

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
		scope.inTransaction(
				session -> {
					session.createQuery( "update LazyAbstractEntityReference e set e.entity = null" ).executeUpdate();
					session.createQuery( "update LazyConcreteEntityReference e set e.entity = null" ).executeUpdate();
					session.createQuery( "delete from ConcreteEntity" ).executeUpdate();
					session.createQuery( "delete from LazyAbstractEntityReference" ).executeUpdate();
					session.createQuery( "delete from LazyConcreteEntityReference" ).executeUpdate();
				}
		);
	}

	@Test
	public void containsEagerAbstractEntity(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					LazyAbstractEntityReference reference = session.get(
							LazyAbstractEntityReference.class, abstractEntityReferenceId );
					assertThat( reference ).isNotNull();
					assertThat( Hibernate.isInitialized( reference.getEntity() ) ).isTrue();
				}
		);
	}

	@Test
	public void containsEagerConcreteEntity(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					LazyConcreteEntityReference reference = session.get(
							LazyConcreteEntityReference.class, concreteEntityReferenceId );
					assertThat( reference ).isNotNull();
					assertThat( Hibernate.isInitialized( reference.getEntity() ) ).isTrue();
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

		@ManyToOne(fetch = FetchType.EAGER)
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

		@ManyToOne(fetch = FetchType.EAGER)
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

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy;

import java.io.Serializable;

import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel(
		annotatedClasses = {
				LazyOneToOneMappedByInDoubleEmbeddedTest.EntityA.class, LazyOneToOneMappedByInDoubleEmbeddedTest.EntityB.class
		}
)
@SessionFactory
@BytecodeEnhanced
@EnhancementOptions(lazyLoading = true)
@JiraKey("HHH-15967")
public class LazyOneToOneMappedByInDoubleEmbeddedTest {

	@BeforeEach
	public void prepare(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			EntityA entityA = new EntityA( 1 );
			EntityB entityB = new EntityB( 2 );

			EmbeddedValueInA embeddedValueInA = new EmbeddedValueInA();
			EmbeddedValueInB embeddedValueInB = new EmbeddedValueInB();
			embeddedValueInA.setEntityB( entityB );
			embeddedValueInB.setEntityA( entityA );

			entityB.setEmbedded( embeddedValueInB );
			entityA.setEmbedded( embeddedValueInA );

			s.persist( entityA );
			s.persist( entityB );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.getSessionFactory().getSchemaManager().truncate();
	}

	@Test
	public void testGetEntityA(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EntityA entityA = session.get( EntityA.class, 1 );
					assertThat( entityA ).isNotNull();
					EntityB entityB = entityA.getEmbedded().getEntityB();
					assertThat( entityB ).isNotNull();
					assertThat( entityB.getEmbedded().getEntityA() ).isNotNull();
					assertThat( entityB.getEmbedded().getEntityA() ).isEqualTo( entityA );
				}
		);
	}

	@Test
	public void testGetReferenceEntityA(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EntityA entityA = session.getReference( EntityA.class, 1 );
					assertThat( entityA ).isNotNull();
					EntityB entityB = entityA.getEmbedded().getEntityB();
					assertThat( entityB ).isNotNull();
					assertThat( entityB.getEmbedded().getEntityA() ).isNotNull();
					assertThat( entityB.getEmbedded().getEntityA() ).isEqualTo( entityA );
				}
		);
	}

	@Entity(name = "EntityA")
	public static class EntityA {
		@Id
		private Integer id;

		@Embedded
		private EmbeddedValueInA embedded = new EmbeddedValueInA();

		public EntityA() {
		}

		private EntityA(Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public EmbeddedValueInA getEmbedded() {
			return embedded;
		}

		public void setEmbedded(EmbeddedValueInA embedded) {
			this.embedded = embedded;
		}
	}

	@Embeddable
	public static class EmbeddedValueInA implements Serializable {
		@OneToOne(mappedBy = "embedded.entityA", fetch = FetchType.LAZY)
		private EntityB entityB;

		public EmbeddedValueInA() {
		}

		public EntityB getEntityB() {
			return entityB;
		}

		public void setEntityB(
				EntityB entityB) {
			this.entityB = entityB;
		}
	}

	@Entity(name = "EntityB")
	public static class EntityB {
		@Id
		private Integer id;

		@Embedded
		private EmbeddedValueInB embedded = new EmbeddedValueInB();

		public EntityB() {
		}

		private EntityB(Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public EmbeddedValueInB getEmbedded() {
			return embedded;
		}

		public void setEmbedded(EmbeddedValueInB embedded) {
			this.embedded = embedded;
		}
	}

	@Embeddable
	public static class EmbeddedValueInB implements Serializable {
		@OneToOne
		private EntityA entityA;

		public EmbeddedValueInB() {
		}

		public EntityA getEntityA() {
			return entityA;
		}

		public void setEntityA(
				EntityA entityA) {
			this.entityA = entityA;
		}
	}
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.embeddable;

import java.io.Serializable;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DomainModel(
		annotatedClasses = {
				LazyToOneMappedByInDoubleEmbeddedTest.EntityA.class,
				LazyToOneMappedByInDoubleEmbeddedTest.EntityB.class,
		}
)
@SessionFactory
public class LazyToOneMappedByInDoubleEmbeddedTest {

	@BeforeAll
	public void prepare(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {

					EntityA entityA = new EntityA( 1 );
					EntityB entityB = new EntityB( 2 );

					EmbeddedValueInA embeddedValueInA = new EmbeddedValueInA();
					EmbeddedValueInB embeddedValueInB = new EmbeddedValueInB();
					embeddedValueInA.setEntityB( entityB );
					embeddedValueInB.setEntityA( entityA );

					entityB.setEmbedded( embeddedValueInB );
					entityA.setEmbedded( embeddedValueInA );

					session.persist( entityA );
					session.persist( entityB );
				} );
	}

	@Test
	public void testGetEntityA(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EntityA entityA = session.get( EntityA.class, 1 );
					assertThat( entityA ).isNotNull();
					EntityB entityB = entityA.getEmbedded().getEntityB();
					assertThat( entityB ).isNotNull();
					EntityA a = entityB.getEmbedded().getEntityA();
					assertThat( a ).isNotNull();
					assertThat( a ).isEqualTo( entityA );
					assertThat( a.getEmbedded().getEntityB() ).isEqualTo( entityB );
				}
		);
	}

	@Test
	public void testGetReferenceEntityA(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					EntityA entityA = session.getReference( EntityA.class, 1 );
					assertThat( entityA ).isNotNull();
					EntityB entityB = entityA.getEmbedded().getEntityB( );
					assertThat( entityB ).isNotNull();
					EntityA a = entityB.getEmbedded().getEntityA();
					assertThat( a ).isNotNull();
					assertThat( a ).isEqualTo( entityA );
					assertThat( a.getEmbedded().getEntityB() ).isEqualTo( entityB );
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
		@ManyToOne(fetch = FetchType.LAZY)
		private EntityB entityB;

		public EmbeddedValueInA() {
		}

		public EntityB  getEntityB() {
			return entityB;
		}

		public void setEntityB(EntityB b) {
			entityB = b;
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
		@ManyToOne(fetch = FetchType.LAZY)
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

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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
				LazyOneToOneMappedByInEmbeddedTest.EntityA.class, LazyOneToOneMappedByInEmbeddedTest.EntityB.class
		}
)
@SessionFactory
@BytecodeEnhanced
@EnhancementOptions(lazyLoading = true)
@JiraKey("HHH-15606")
public class LazyOneToOneMappedByInEmbeddedTest {

	@BeforeEach
	public void prepare(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			EntityA entityA = new EntityA( 1 );
			EntityB entityB = new EntityB( 2 );
			entityA.getEmbedded().setEntityB( entityB );
			entityB.setEntityA( entityA );
			s.persist( entityA );
			s.persist( entityB );
		} );
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			s.createMutationQuery( "delete entityb" ).executeUpdate();
			s.createMutationQuery( "delete entitya" ).executeUpdate();
		} );
	}

	@Test
	public void testGet(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			EntityA entityA = s.get( EntityA.class, "1" );

			assertThat( entityA ).isNotNull();

			EmbeddedValue embedded = entityA.getEmbedded();

			assertThat( embedded ).isNotNull();
			assertThat( embedded.getEntityB() ).isNotNull();
			assertThat( embedded.getEntityB().getEntityA() ).isEqualTo( entityA );
		} );
	}

	@Test
	public void testGetReference(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			EntityA entityA = s.getReference( EntityA.class, "1" );

			assertThat( entityA ).isNotNull();

			EmbeddedValue embedded = entityA.getEmbedded();

			assertThat( embedded ).isNotNull();
			assertThat( embedded.getEntityB() ).isNotNull();
			assertThat( embedded.getEntityB().getEntityA() ).isEqualTo( entityA );
		} );
	}

	@Entity(name = "entitya")
	public static class EntityA {
		@Id
		private Integer id;

		private String name;

		@Embedded
		private EmbeddedValue embedded = new EmbeddedValue();

		public EntityA() {
		}

		private EntityA(Integer id) {
			this.id = id;
		}


		public Integer getId() {
			return id;
		}

		public EmbeddedValue getEmbedded() {
			return embedded;
		}

		public void setEmbedded(EmbeddedValue embedded) {
			this.embedded = embedded;
		}
	}

	@Embeddable
	public static class EmbeddedValue implements Serializable {
		@OneToOne(mappedBy = "entityA", fetch = FetchType.LAZY)
		private EntityB entityB;

		public EmbeddedValue() {
		}

		public EntityB getEntityB() {
			return entityB;
		}

		public void setEntityB(
				EntityB entityB) {
			this.entityB = entityB;
		}
	}


	@Entity(name = "entityb")
	public static class EntityB {
		@Id
		private Integer id;

		private String name;

		@OneToOne
		private EntityA entityA;

		public EntityB() {
		}

		private EntityB(Integer id) {
			this.id = id;
		}

		public Integer getId() {
			return id;
		}

		public EntityA getEntityA() {
			return entityA;
		}

		public void setEntityA(EntityA entityA) {
			this.entityA = entityA;
		}
	}
}

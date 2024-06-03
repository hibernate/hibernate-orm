/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.bytecode.enhancement.lazy.proxy;

import org.hibernate.Hibernate;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

@DomainModel(
		annotatedClasses = {
				LazyOneToOneMappedByTest.EntityA.class, LazyOneToOneMappedByTest.EntityB.class
		}
)
@ServiceRegistry(
		settings = {
				@Setting( name = AvailableSettings.GENERATE_STATISTICS, value = "true" ),
		}
)
@SessionFactory
@BytecodeEnhanced
@EnhancementOptions(lazyLoading = true)
@JiraKey("HHH-15606")
public class LazyOneToOneMappedByTest {

	@BeforeEach
	public void prepare(SessionFactoryScope scope) {
		scope.inTransaction( s -> {
			EntityA entityA = new EntityA( 1, "A" );
			EntityB entityB = new EntityB( 2 );
			entityA.setEntityB( entityB );
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
		final Statistics stats = scope.getSessionFactory().getStatistics();
		stats.clear();
		scope.inTransaction( s -> {
			EntityA entityA = s.get( EntityA.class, "1" );
			assertThat( stats.getPrepareStatementCount() ).isEqualTo( 1 );
			assertThat( entityA ).isNotNull();
			assertThat( entityA.getName() ).isEqualTo( "A" );
			assertThat( stats.getPrepareStatementCount() ).isEqualTo( 1 );

			assertFalse( Hibernate.isPropertyInitialized( entityA, "entityB" ) );

			assertThat( entityA.getEntityB() ).isNotNull();
			assertThat( stats.getPrepareStatementCount() ).isEqualTo( 2 );

			assertThat( entityA.getEntityB().getEntityA() ).isEqualTo( entityA );
			assertThat( stats.getPrepareStatementCount() ).isEqualTo( 2 );
		} );
	}

	@Test
	public void testGetReference(SessionFactoryScope scope) {
		final Statistics stats = scope.getSessionFactory().getStatistics();
		stats.clear();
		scope.inTransaction( s -> {
			EntityA entityA = s.getReference( EntityA.class, "1" );
			assertThat( stats.getPrepareStatementCount() ).isEqualTo( 0 );
			assertThat( entityA ).isNotNull();
			assertThat( entityA.getName() ).isEqualTo( "A" );
			assertThat( stats.getPrepareStatementCount() ).isEqualTo( 1 );

			assertFalse( Hibernate.isPropertyInitialized( entityA, "entityB" ) );

			assertThat( entityA.getEntityB() ).isNotNull();
			assertThat( stats.getPrepareStatementCount() ).isEqualTo( 2 );

			assertThat( entityA.getEntityB().getEntityA() ).isEqualTo( entityA );
			assertThat( stats.getPrepareStatementCount() ).isEqualTo( 2 );
		} );
	}

	@Entity(name = "entitya")
	public static class EntityA {
		@Id
		private Integer id;

		private String name;

		public String getName() {
			return name;
		}

		@OneToOne(mappedBy = "entityA", fetch = FetchType.LAZY)
		private EntityB entityB;

		public EntityA() {
		}

		private EntityA(Integer id, String name) {
			this.id = id;
			this.name = name;
		}


		public Integer getId() {
			return id;
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

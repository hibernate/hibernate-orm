/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.onetoone.bidirectional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.hibernate.Hibernate;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

/**
 * Test an eager bi-directional one-to-one association
 * and how `hibernate.max_fetch_depth` prevents the resulting joins
 * from getting out of hands when fetching the (eager) association recursively.
 */
@RunWith(BytecodeEnhancerRunner.class)
@EnhancementOptions(lazyLoading = true)
@TestForIssue(jiraKey = "HHH-16219")
public class BiDirectionalOneToOneEagerMaxFetchDepthTest extends BaseCoreFunctionalTestCase {

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { EntityA.class, EntityB.class };
	}

	@Override
	protected void configure(Configuration configuration) {
		super.configure( configuration );
		configuration.setProperty( AvailableSettings.MAX_FETCH_DEPTH, "1" );
		configuration.setStatementInspector( new SQLStatementInspector() );
	}

	SQLStatementInspector statementInspector() {
		return (SQLStatementInspector) sessionFactory().getSessionFactoryOptions().getStatementInspector();
	}

	@Before
	public void prepare() {
		inTransaction( s -> {
			EntityA entityA = new EntityA();
			entityA.setId( 1 );
			EntityB entityB = new EntityB();
			entityB.setId( 2 );
			entityA.setEntityB1( entityB );
			entityB.setEntityA1( entityA );
			s.persist( entityA );
			s.persist( entityB );
		} );
	}

	@After
	public void tearDown() {
		inTransaction( s -> {
			s.createMutationQuery( "delete entitya" ).executeUpdate();
			s.createMutationQuery( "delete entityb" ).executeUpdate();
		} );
	}

	@Test
	public void testDirectLoading() {
		statementInspector().clear();
		inTransaction( s -> {
			EntityA entityA = s.get( EntityA.class, 1 );
			assertThat( entityA ).isNotNull();
			// We had 2 associations to load: both eager associations from EntityA to EntityB.
			statementInspector().assertNumberOfJoins( 0, 2 );
		} );
	}

	@Test
	public void testProxyLoading() {
		statementInspector().clear();
		inTransaction( s -> {
			EntityA entityA = s.getReference( EntityA.class, 1 );
			assertThat( entityA ).isNotNull();
			statementInspector().assertExecutedCount( 0 );

			assertFalse( Hibernate.isPropertyInitialized( entityA, "entityB" ) );
			// Load the entity
			assertThat( entityA.getEntityB1() ).isNotNull();
			// We had 2 associations to load: both eager associations from EntityA to EntityB.
			statementInspector().assertNumberOfJoins( 0, 2 );
		} );
	}

	@Test
	public void testProxyLoadingThroughLazyAssociation() {
		statementInspector().clear();
		inTransaction( s -> {
			EntityB entityB = s.get( EntityB.class, 2 );
			assertThat( entityB ).isNotNull();
			statementInspector().assertNumberOfJoins( 0, 0 );

			// Load the entity through the association
			assertThat( entityB.getEntityA1() ).isNotNull();
			// We had 3 associations to load: "EntityA1" from EntityB to EntityA which is being lazily initialized,
			// and both eager associations from EntityA to EntityB.
			statementInspector().assertNumberOfJoins( 1, 3 );
		} );
	}

	@Entity(name = "entitya")
	public static class EntityA {

		@Id
		private Integer id;

		@OneToOne
		private EntityB entityB1;

		@OneToOne
		private EntityB entityB2;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public EntityB getEntityB1() {
			return entityB1;
		}

		public void setEntityB1(EntityB entityB1) {
			this.entityB1 = entityB1;
		}

		public EntityB getEntityB2() {
			return entityB2;
		}

		public void setEntityB2(EntityB entityB2) {
			this.entityB2 = entityB2;
		}
	}

	@Entity(name = "entityb")
	public static class EntityB {
		@Id
		private Integer id;

		@OneToOne(mappedBy = "entityB1", fetch = FetchType.LAZY)
		private EntityA entityA1;

		@OneToOne(mappedBy = "entityB2", fetch = FetchType.LAZY)
		private EntityA entityA2;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public EntityA getEntityA1() {
			return entityA1;
		}

		public void setEntityA1(EntityA entityA1) {
			this.entityA1 = entityA1;
		}

		public EntityA getEntityA2() {
			return entityA2;
		}

		public void setEntityA2(EntityA entityA2) {
			this.entityA2 = entityA2;
		}
	}

}

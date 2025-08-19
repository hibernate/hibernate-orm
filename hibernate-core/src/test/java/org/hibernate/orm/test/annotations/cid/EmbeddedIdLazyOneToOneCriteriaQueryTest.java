/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.annotations.cid;

import jakarta.persistence.*;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.hibernate.Hibernate;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.hibernate.testing.orm.junit.Jira;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Jira("https://hibernate.atlassian.net/browse/HHH-19687")
@RunWith( BytecodeEnhancerRunner.class )
public class EmbeddedIdLazyOneToOneCriteriaQueryTest extends BaseCoreFunctionalTestCase {

    @Override
    protected Class<?>[] getAnnotatedClasses() {
        return new Class[]{
                EmbeddedIdLazyOneToOneCriteriaQueryTest.EntityA.class,
                EmbeddedIdLazyOneToOneCriteriaQueryTest.EntityB.class
        };
    }

	@Test
	public void query() {
		inTransaction( session -> {
			final CriteriaBuilder builder = session.getCriteriaBuilder();
			final CriteriaQuery<EntityA> criteriaQuery = builder.createQuery( EntityA.class );
			final Root<EntityA> root = criteriaQuery.from( EntityA.class );
			criteriaQuery.where( root.get( "id" ).in( 1 ) );
			criteriaQuery.select( root );

			final List<EntityA> entities = session.createQuery( criteriaQuery ).getResultList();
			assertThat( entities ).hasSize( 1 );
			assertThat( Hibernate.isPropertyInitialized( entities.get( 0 ), "entityB" ) ).isFalse();
		} );
	}

	@Before
	public void setUp() {
		inTransaction( session -> {
			final EntityA entityA = new EntityA( 1 );
			session.persist( entityA );
			final EntityB entityB = new EntityB( new EntityBId( entityA ) );
			session.persist( entityB );
		} );
	}

	@After
	public void tearDown() {
		inTransaction( session -> session.getSessionFactory().getSchemaManager().truncateMappedObjects() );
	}

	@Entity(name = "EntityA")
	static class EntityA {

		@Id
		private Integer id;

		@OneToOne(mappedBy = "id.entityA", fetch = FetchType.LAZY)
		private EntityB entityB;

		public EntityA() {
		}

		public EntityA(Integer id) {
			this.id = id;
		}

	}

	@Entity(name = "EntityB")
	static class EntityB {

		@EmbeddedId
		private EntityBId id;

		public EntityB() {
		}

		public EntityB(EntityBId id) {
			this.id = id;
		}

	}

	@Embeddable
	static class EntityBId {

		@OneToOne(fetch = FetchType.LAZY)
		private EntityA entityA;

		public EntityBId() {
		}

		public EntityBId(EntityA entityA) {
			this.entityA = entityA;
		}

	}

}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.test.criteria;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;
import javax.persistence.Tuple;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.ListJoin;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.hibernate.jpa.test.BaseEntityManagerFunctionalTestCase;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
public class TreatListJoinTest extends BaseEntityManagerFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {TestEntity.class, EntityA.class, EntityB.class};
	}

	@Before
	public void setUp() {
		EntityManager em = createEntityManager();
		try {
			em.getTransaction().begin();

			for ( int i = 0; i < 5; i++ ) {
				TestEntity e = new TestEntity();
				EntityA eA = new EntityA();
				eA.setParent( e );
				eA.valueA = "a_" + i;

				em.persist( e );
			}
			for ( int i = 0; i < 5; i++ ) {
				TestEntity e = new TestEntity();

				EntityB eB = new EntityB();
				eB.valueB = "b_" + i;
				eB.setParent( e );

				em.persist( e );
			}

			em.getTransaction().commit();
		}
		catch (Exception e) {
			if ( em.getTransaction().isActive() ) {
				em.getTransaction().rollback();
			}
			throw e;
		}
		finally {
			em.close();
		}
	}

	@Test
	public void testTreatJoin() {
		EntityManager em = createEntityManager();
		try {
			final CriteriaBuilder cb = em.getCriteriaBuilder();

			final CriteriaQuery<Tuple> query = cb.createTupleQuery();
			final Root<TestEntity> testEntity = query.from( TestEntity.class );

			final List<Selection<?>> selections = new LinkedList();
			selections.add( testEntity.get( "id" ) );

			final ListJoin<TestEntity, AbstractEntity> entities = testEntity.joinList(
					"entities",
					JoinType.LEFT
			);
			entities.on( cb.equal( entities.get( "entityType" ), EntityA.class.getName() ) );

			final ListJoin<TestEntity, EntityA> joinEntityA = cb.treat(
					entities,
					EntityA.class
			);
			selections.add( joinEntityA.get( "id" ) );
			selections.add( joinEntityA.get( "valueA" ) );

			final ListJoin<TestEntity, AbstractEntity> entitiesB = testEntity.joinList(
					"entities",
					JoinType.LEFT
			);
			entitiesB.on( cb.equal( entitiesB.get( "entityType" ), EntityB.class.getName() ) );
			final ListJoin<TestEntity, EntityB> joinEntityB = cb.treat(
					entitiesB,
					EntityB.class
			);
			selections.add( joinEntityB.get( "id" ) );
			selections.add( joinEntityB.get( "valueB" ) );

			query.multiselect( selections );

			final List<Tuple> resultList = em.createQuery( query ).getResultList();
			assertThat( resultList.size(), is( 10 ) );
		}
		finally {
			em.close();
		}
	}

	@MappedSuperclass
	public static abstract class MyEntity {
		@Id
		@GeneratedValue
		private long id;
	}

	@Entity(name = "TestEntity")
	public static class TestEntity extends MyEntity {
		@OneToMany(mappedBy = "parent", cascade = {CascadeType.ALL})
		private List<AbstractEntity> entities = new ArrayList<>();
	}

	@Entity(name = "AbstractEntity")
	public static abstract class AbstractEntity extends MyEntity {
		String entityType = getClass().getName();

		@ManyToOne
		private TestEntity parent;

		public TestEntity getParent() {
			return parent;
		}

		public void setParent(TestEntity parent) {
			this.parent = parent;
			parent.entities.add( this );
		}
	}

	@Entity(name = "EntityA")
	public static class EntityA extends AbstractEntity {
		public String valueA;

		public EntityA() {
			super.entityType = getClass().getName();
		}
	}

	@Entity(name = "EntityB")
	public static class EntityB extends AbstractEntity {
		public String valueB;

		public EntityB() {
			super.entityType = getClass().getName();
		}
	}
}

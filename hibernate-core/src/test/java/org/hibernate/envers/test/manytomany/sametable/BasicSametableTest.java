/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.manytomany.sametable;

import java.sql.Types;

import org.hibernate.Session;
import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.manytomany.sametable.Child1Entity;
import org.hibernate.envers.test.support.domains.manytomany.sametable.Child2Entity;
import org.hibernate.envers.test.support.domains.manytomany.sametable.ParentEntity;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;

/**
 * Test which checks that auditing entities which contain multiple mappings to same tables work.
 *
 * @author Adam Warski (adam at warski dot org)
 */
@Disabled("NYI - Native query support")
public class BasicSametableTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer p1_id;
	private Integer p2_id;
	private Integer c1_1_id;
	private Integer c1_2_id;
	private Integer c2_1_id;
	private Integer c2_2_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { ParentEntity.class, Child1Entity.class, Child2Entity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		// We need first to modify the columns in the middle (join table) to allow null values. Hbm2ddl doesn't seem
		// to allow this.
		alterSchema();

		inTransactions(
				// Revision 1
				entityManager -> {
					ParentEntity p1 = new ParentEntity( "parent_1" );
					ParentEntity p2 = new ParentEntity( "parent_2" );

					Child1Entity c1_1 = new Child1Entity( "child1_1" );
					Child1Entity c1_2 = new Child1Entity( "child1_2" );

					Child2Entity c2_1 = new Child2Entity( "child2_1" );
					Child2Entity c2_2 = new Child2Entity( "child2_2" );

					entityManager.persist( p1 );
					entityManager.persist( p2 );
					entityManager.persist( c1_1 );
					entityManager.persist( c1_2 );
					entityManager.persist( c2_1 );
					entityManager.persist( c2_2 );

					p1_id = p1.getId();
					p2_id = p2.getId();
					c1_1_id = c1_1.getId();
					c1_2_id = c1_2.getId();
					c2_1_id = c2_1.getId();
					c2_2_id = c2_2.getId();
				},

				// Revision 2 - (p1: c1_1, p2: c2_1)
				entityManager -> {
					entityManager.clear();

					ParentEntity p1 = entityManager.find( ParentEntity.class, p1_id );
					ParentEntity p2 = entityManager.find( ParentEntity.class, p2_id );
					Child1Entity c1_1 = entityManager.find( Child1Entity.class, c1_1_id );
					Child2Entity c2_1 = entityManager.find( Child2Entity.class, c2_1_id );

					p1.getChildren1().add( c1_1 );
					p2.getChildren2().add( c2_1 );
				},

				// Revision 3 - (p1: c1_1, c1_2, c2_2, p2: c1_1, c2_1)
				entityManager -> {
					entityManager.clear();

					ParentEntity p1 = entityManager.find( ParentEntity.class, p1_id );
					ParentEntity p2 = entityManager.find( ParentEntity.class, p2_id );
					Child1Entity c1_1 = entityManager.find( Child1Entity.class, c1_1_id );
					Child1Entity c1_2 = entityManager.find( Child1Entity.class, c1_2_id );
					Child2Entity c2_2 = entityManager.find( Child2Entity.class, c2_2_id );

					p1.getChildren1().add( c1_2 );
					p1.getChildren2().add( c2_2 );

					p2.getChildren1().add( c1_1 );

				},

				// Revision 4 - (p1: c1_2, c2_2, p2: c1_1, c2_1, c2_2)
				entityManager -> {
					entityManager.clear();

					ParentEntity p1 = entityManager.find( ParentEntity.class, p1_id );
					ParentEntity p2 = entityManager.find( ParentEntity.class, p2_id );
					Child1Entity c1_1 = entityManager.find( Child1Entity.class, c1_1_id );
					Child2Entity c2_2 = entityManager.find( Child2Entity.class, c2_2_id );

					p1.getChildren1().remove( c1_1 );
					p2.getChildren2().add( c2_2 );
				},

				// Revision 5 - (p1: c2_2, p2: c1_1, c2_1)
				entityManager -> {
					entityManager.clear();

					ParentEntity p1 = entityManager.find( ParentEntity.class, p1_id );
					ParentEntity p2 = entityManager.find( ParentEntity.class, p2_id );
					Child1Entity c1_2 = entityManager.find( Child1Entity.class, c1_2_id );
					Child2Entity c2_2 = entityManager.find( Child2Entity.class, c2_2_id );

					c2_2.getParents().remove( p2 );
					c1_2.getParents().remove( p1 );
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( ParentEntity.class, p1_id ), contains( 1, 2, 3, 4 ) );
		assertThat( getAuditReader().getRevisions( ParentEntity.class, p2_id ), contains( 1, 2, 3, 4 ) );

		assertThat( getAuditReader().getRevisions( Child1Entity.class, c1_1_id ), contains( 1 ) );
		assertThat( getAuditReader().getRevisions( Child1Entity.class, c1_2_id ), contains( 1, 5 ) );

		assertThat( getAuditReader().getRevisions( Child2Entity.class, c2_1_id ), contains( 1 ) );
		assertThat( getAuditReader().getRevisions( Child2Entity.class, c2_2_id ), contains( 1, 5 ) );
	}

	@DynamicTest
	public void testHistoryOfParent1() {
		Child1Entity c1_1 = inTransaction( em -> { return em.find( Child1Entity.class, c1_1_id ); } );
		Child1Entity c1_2 = inTransaction( em -> { return em.find( Child1Entity.class, c1_2_id ); } );
		Child2Entity c2_2 = inTransaction( em -> { return em.find( Child2Entity.class, c2_2_id ); } );

		ParentEntity rev1 = getAuditReader().find( ParentEntity.class, p1_id, 1 );
		ParentEntity rev2 = getAuditReader().find( ParentEntity.class, p1_id, 2 );
		ParentEntity rev3 = getAuditReader().find( ParentEntity.class, p1_id, 3 );
		ParentEntity rev4 = getAuditReader().find( ParentEntity.class, p1_id, 4 );
		ParentEntity rev5 = getAuditReader().find( ParentEntity.class, p1_id, 5 );

		assertThat( rev1.getChildren1(), CollectionMatchers.isEmpty() );
		assertThat( rev2.getChildren1(), containsInAnyOrder( c1_1 ) );
		assertThat( rev3.getChildren1(), containsInAnyOrder( c1_1, c1_2 ) );
		assertThat( rev4.getChildren1(), containsInAnyOrder( c1_2 ) );
		assertThat( rev5.getChildren1(), CollectionMatchers.isEmpty() );

		assertThat( rev1.getChildren2(), CollectionMatchers.isEmpty() );
		assertThat( rev2.getChildren2(), CollectionMatchers.isEmpty() );
		assertThat( rev3.getChildren2(), containsInAnyOrder( c2_2 ) );
		assertThat( rev4.getChildren2(), containsInAnyOrder( c2_2 ) );
		assertThat( rev5.getChildren2(), containsInAnyOrder( c2_2 ) );
	}

	@DynamicTest
	public void testHistoryOfParent2() {
		Child1Entity c1_1 = inTransaction( em -> { return em.find( Child1Entity.class, c1_1_id ); } );
		Child2Entity c2_1 = inTransaction( em -> { return em.find( Child2Entity.class, c2_1_id ); } );
		Child2Entity c2_2 = inTransaction( em -> { return em.find( Child2Entity.class, c2_2_id ); } );

		ParentEntity rev1 = getAuditReader().find( ParentEntity.class, p2_id, 1 );
		ParentEntity rev2 = getAuditReader().find( ParentEntity.class, p2_id, 2 );
		ParentEntity rev3 = getAuditReader().find( ParentEntity.class, p2_id, 3 );
		ParentEntity rev4 = getAuditReader().find( ParentEntity.class, p2_id, 4 );
		ParentEntity rev5 = getAuditReader().find( ParentEntity.class, p2_id, 5 );

		assertThat( rev1.getChildren1(), CollectionMatchers.isEmpty() );
		assertThat( rev2.getChildren1(), CollectionMatchers.isEmpty() );
		assertThat( rev3.getChildren1(), containsInAnyOrder( c1_1 ) );
		assertThat( rev4.getChildren1(), containsInAnyOrder( c1_1 ) );
		assertThat( rev5.getChildren1(), containsInAnyOrder( c1_1 ) );

		assertThat( rev1.getChildren2(), CollectionMatchers.isEmpty() );
		assertThat( rev2.getChildren2(), containsInAnyOrder( c2_1 ) );
		assertThat( rev3.getChildren2(), containsInAnyOrder( c2_1 ) );
		assertThat( rev4.getChildren2(), containsInAnyOrder( c2_1, c2_2 ) );
		assertThat( rev5.getChildren2(), containsInAnyOrder( c2_1 ) );
	}

	@DynamicTest
	public void testHistoryOfChild1_1() {
		ParentEntity p1 = inTransaction( em -> { return em.find( ParentEntity.class, p1_id ); } );
		ParentEntity p2 = inTransaction( em -> { return em.find( ParentEntity.class, p2_id ); } );

		Child1Entity rev1 = getAuditReader().find( Child1Entity.class, c1_1_id, 1 );
		Child1Entity rev2 = getAuditReader().find( Child1Entity.class, c1_1_id, 2 );
		Child1Entity rev3 = getAuditReader().find( Child1Entity.class, c1_1_id, 3 );
		Child1Entity rev4 = getAuditReader().find( Child1Entity.class, c1_1_id, 4 );
		Child1Entity rev5 = getAuditReader().find( Child1Entity.class, c1_1_id, 5 );

		assertThat( rev1.getParents(), CollectionMatchers.isEmpty() );
		assertThat( rev2.getParents(), containsInAnyOrder( p1 ) );
		assertThat( rev3.getParents(), containsInAnyOrder( p1, p2 ) );
		assertThat( rev4.getParents(), containsInAnyOrder( p2 ) );
		assertThat( rev5.getParents(), containsInAnyOrder( p2 ) );
	}

	// TODO: was disabled?
	@DynamicTest
	public void testHistoryOfChild1_2() {
		ParentEntity p1 = inTransaction( em -> { return em.find( ParentEntity.class, p1_id ); } );

		Child1Entity rev1 = getAuditReader().find( Child1Entity.class, c1_2_id, 1 );
		Child1Entity rev2 = getAuditReader().find( Child1Entity.class, c1_2_id, 2 );
		Child1Entity rev3 = getAuditReader().find( Child1Entity.class, c1_2_id, 3 );
		Child1Entity rev4 = getAuditReader().find( Child1Entity.class, c1_2_id, 4 );
		Child1Entity rev5 = getAuditReader().find( Child1Entity.class, c1_2_id, 5 );

		assertThat( rev1.getParents(), CollectionMatchers.isEmpty() );
		assertThat( rev2.getParents(), CollectionMatchers.isEmpty() );
		assertThat( rev3.getParents(), containsInAnyOrder( p1 ) );
		assertThat( rev4.getParents(), containsInAnyOrder( p1 ) );
		assertThat( rev5.getParents(), CollectionMatchers.isEmpty() );
	}

	@DynamicTest
	public void testHistoryOfChild2_1() {
		ParentEntity p2 = inTransaction( em -> { return em.find( ParentEntity.class, p2_id ); } );

		Child2Entity rev1 = getAuditReader().find( Child2Entity.class, c2_1_id, 1 );
		Child2Entity rev2 = getAuditReader().find( Child2Entity.class, c2_1_id, 2 );
		Child2Entity rev3 = getAuditReader().find( Child2Entity.class, c2_1_id, 3 );
		Child2Entity rev4 = getAuditReader().find( Child2Entity.class, c2_1_id, 4 );
		Child2Entity rev5 = getAuditReader().find( Child2Entity.class, c2_1_id, 5 );

		assertThat( rev1.getParents(), CollectionMatchers.isEmpty() );
		assertThat( rev2.getParents(), containsInAnyOrder( p2 ) );
		assertThat( rev3.getParents(), containsInAnyOrder( p2 ) );
		assertThat( rev4.getParents(), containsInAnyOrder( p2 ) );
		assertThat( rev5.getParents(), containsInAnyOrder( p2 ) );
	}

	@DynamicTest
	public void testHistoryOfChild2_2() {
		ParentEntity p1 = inTransaction( em -> { return em.find( ParentEntity.class, p1_id ); } );
		ParentEntity p2 = inTransaction( em -> { return em.find( ParentEntity.class, p2_id ); } );

		Child2Entity rev1 = getAuditReader().find( Child2Entity.class, c2_2_id, 1 );
		Child2Entity rev2 = getAuditReader().find( Child2Entity.class, c2_2_id, 2 );
		Child2Entity rev3 = getAuditReader().find( Child2Entity.class, c2_2_id, 3 );
		Child2Entity rev4 = getAuditReader().find( Child2Entity.class, c2_2_id, 4 );
		Child2Entity rev5 = getAuditReader().find( Child2Entity.class, c2_2_id, 5 );

		assertThat( rev1.getParents(), CollectionMatchers.isEmpty() );
		assertThat( rev2.getParents(), CollectionMatchers.isEmpty() );
		assertThat( rev3.getParents(), containsInAnyOrder( p1 ) );
		assertThat( rev4.getParents(), containsInAnyOrder( p1, p2 ) );
		assertThat( rev5.getParents(), containsInAnyOrder( p1 ) );
	}

	private void alterSchema() {
		inTransaction(
				entityManager -> {
					final Session session = entityManager.unwrap( Session.class );

					session.createNativeQuery( "DROP TABLE children" ).executeUpdate();

					session.createNativeQuery(
							"CREATE TABLE children ( parent_id " + getDialect().getTypeName( Types.INTEGER ) +
									", child1_id " + getDialect().getTypeName( Types.INTEGER ) + getDialect().getNullColumnString() +
									", child2_id " + getDialect().getTypeName( Types.INTEGER ) + getDialect().getNullColumnString() + " )"
					).executeUpdate();

					session.createNativeQuery( "DROP TABLE children_AUD" ).executeUpdate();

					session.createNativeQuery(
							"CREATE TABLE children_AUD ( REV " + getDialect().getTypeName( Types.INTEGER ) + " NOT NULL" +
									", REVEND " + getDialect().getTypeName( Types.INTEGER ) +
									", REVTYPE " + getDialect().getTypeName( Types.TINYINT ) +
									", parent_id " + getDialect().getTypeName( Types.INTEGER ) +
									", child1_id " + getDialect().getTypeName( Types.INTEGER ) + getDialect().getNullColumnString() +
									", child2_id " + getDialect().getTypeName( Types.INTEGER ) + getDialect().getNullColumnString() + " )"
					).executeUpdate();
				}
		);
	}
}
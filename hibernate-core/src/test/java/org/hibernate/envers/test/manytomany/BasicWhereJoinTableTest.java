/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.manytomany;

import org.hibernate.envers.test.EnversEntityManagerFactoryBasedFunctionalTest;
import org.hibernate.envers.test.support.domains.basic.IntNoAutoIdTestEntity;
import org.hibernate.envers.test.support.domains.manytomany.WhereJoinTableEntity;
import org.junit.jupiter.api.Disabled;

import org.hibernate.testing.hamcrest.CollectionMatchers;
import org.hibernate.testing.junit5.dynamictests.DynamicBeforeAll;
import org.hibernate.testing.junit5.dynamictests.DynamicTest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;

/**
 * @author Adam Warski (adam at warski dot org)
 */
@Disabled("NYI - @WhereJoinTable annotation support")
public class BasicWhereJoinTableTest extends EnversEntityManagerFactoryBasedFunctionalTest {
	private Integer ite1_1_id;
	private Integer ite1_2_id;
	private Integer ite2_1_id;
	private Integer ite2_2_id;

	private Integer wjte1_id;
	private Integer wjte2_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { WhereJoinTableEntity.class, IntNoAutoIdTestEntity.class };
	}

	@DynamicBeforeAll
	public void prepareAuditData() {
		inJPA(
				entityManager -> {
					IntNoAutoIdTestEntity ite1_1 = new IntNoAutoIdTestEntity( 1, 10 );
					IntNoAutoIdTestEntity ite1_2 = new IntNoAutoIdTestEntity( 1, 11 );
					IntNoAutoIdTestEntity ite2_1 = new IntNoAutoIdTestEntity( 2, 20 );
					IntNoAutoIdTestEntity ite2_2 = new IntNoAutoIdTestEntity( 2, 21 );

					WhereJoinTableEntity wjte1 = new WhereJoinTableEntity();
					wjte1.setData( "wjte1" );

					WhereJoinTableEntity wjte2 = new WhereJoinTableEntity();
					wjte1.setData( "wjte2" );

					// Revision 1
					entityManager.getTransaction().begin();

					entityManager.persist( ite1_1 );
					entityManager.persist( ite1_2 );
					entityManager.persist( ite2_1 );
					entityManager.persist( ite2_2 );
					entityManager.persist( wjte1 );
					entityManager.persist( wjte2 );

					entityManager.getTransaction().commit();
					entityManager.clear();

					// Revision 2 (wjte1: 1_1, 2_1)

					entityManager.getTransaction().begin();

					wjte1 = entityManager.find( WhereJoinTableEntity.class, wjte1.getId() );

					wjte1.getReferences1().add( ite1_1 );
					wjte1.getReferences2().add( ite2_1 );

					entityManager.getTransaction().commit();
					entityManager.clear();

					// Revision 3 (wjte1: 1_1, 2_1; wjte2: 1_1, 1_2)
					entityManager.getTransaction().begin();

					wjte2 = entityManager.find( WhereJoinTableEntity.class, wjte2.getId() );

					wjte2.getReferences1().add( ite1_1 );
					wjte2.getReferences1().add( ite1_2 );

					entityManager.getTransaction().commit();
					entityManager.clear();

					// Revision 4 (wjte1: 2_1; wjte2: 1_1, 1_2, 2_2)
					entityManager.getTransaction().begin();

					wjte1 = entityManager.find( WhereJoinTableEntity.class, wjte1.getId() );
					wjte2 = entityManager.find( WhereJoinTableEntity.class, wjte2.getId() );

					wjte1.getReferences1().remove( ite1_1 );
					wjte2.getReferences2().add( ite2_2 );

					entityManager.getTransaction().commit();
					entityManager.clear();

					ite1_1_id = ite1_1.getId();
					ite1_2_id = ite1_2.getId();
					ite2_1_id = ite2_1.getId();
					ite2_2_id = ite2_2.getId();

					wjte1_id = wjte1.getId();
					wjte2_id = wjte2.getId();
				}
		);
	}

	@DynamicTest
	public void testRevisionsCounts() {
		assertThat( getAuditReader().getRevisions( WhereJoinTableEntity.class, wjte1_id ), contains( 1, 2, 4 ) );
		assertThat( getAuditReader().getRevisions( WhereJoinTableEntity.class, wjte2_id ), contains( 1, 3, 4 ) );

		assertThat( getAuditReader().getRevisions( IntNoAutoIdTestEntity.class, ite1_1_id ), contains( 1 ) );
		assertThat( getAuditReader().getRevisions( IntNoAutoIdTestEntity.class, ite1_2_id ), contains( 1 ) );
		assertThat( getAuditReader().getRevisions( IntNoAutoIdTestEntity.class, ite2_1_id ), contains( 1 ) );
		assertThat( getAuditReader().getRevisions( IntNoAutoIdTestEntity.class, ite2_2_id ), contains( 1 ) );
	}

	@DynamicTest
	public void testHistoryOfWjte1() {
		inTransaction(
				entityManager -> {
					IntNoAutoIdTestEntity ite1_1 = entityManager.find( IntNoAutoIdTestEntity.class, ite1_1_id );
					IntNoAutoIdTestEntity ite2_1 = entityManager.find( IntNoAutoIdTestEntity.class, ite2_1_id );

					WhereJoinTableEntity rev1 = getAuditReader().find( WhereJoinTableEntity.class, wjte1_id, 1 );
					WhereJoinTableEntity rev2 = getAuditReader().find( WhereJoinTableEntity.class, wjte1_id, 2 );
					WhereJoinTableEntity rev3 = getAuditReader().find( WhereJoinTableEntity.class, wjte1_id, 3 );
					WhereJoinTableEntity rev4 = getAuditReader().find( WhereJoinTableEntity.class, wjte1_id, 4 );

					// Checking 1st list
					assertThat( rev1.getReferences1(), CollectionMatchers.isEmpty() );
					assertThat( rev2.getReferences1(), contains( ite1_1 ) );
					assertThat( rev3.getReferences1(), contains( ite1_1 ) );
					assertThat( rev4.getReferences1(), CollectionMatchers.isEmpty() );

					// Checking 2nd list
					assertThat( rev1.getReferences2(), CollectionMatchers.isEmpty() );
					assertThat( rev2.getReferences2(), contains( ite2_1 ) );
					assertThat( rev3.getReferences2(), contains( ite2_1 ) );
					assertThat( rev4.getReferences2(), contains( ite2_1 ) );
				}
		);
	}

	@DynamicTest
	public void testHistoryOfWjte2() {
		inTransaction(
				entityManager -> {
					IntNoAutoIdTestEntity ite1_1 = entityManager.find( IntNoAutoIdTestEntity.class, ite1_1_id );
					IntNoAutoIdTestEntity ite1_2 = entityManager.find( IntNoAutoIdTestEntity.class, ite1_2_id );
					IntNoAutoIdTestEntity ite2_2 = entityManager.find( IntNoAutoIdTestEntity.class, ite2_2_id );

					WhereJoinTableEntity rev1 = getAuditReader().find( WhereJoinTableEntity.class, wjte2_id, 1 );
					WhereJoinTableEntity rev2 = getAuditReader().find( WhereJoinTableEntity.class, wjte2_id, 2 );
					WhereJoinTableEntity rev3 = getAuditReader().find( WhereJoinTableEntity.class, wjte2_id, 3 );
					WhereJoinTableEntity rev4 = getAuditReader().find( WhereJoinTableEntity.class, wjte2_id, 4 );

					// Checking 1st list
					assertThat( rev1.getReferences1(), CollectionMatchers.isEmpty() );
					assertThat( rev2.getReferences1(), CollectionMatchers.isEmpty() );
					assertThat( rev3.getReferences1(), containsInAnyOrder( ite1_1, ite1_2 ) );
					assertThat( rev4.getReferences1(), containsInAnyOrder( ite1_1, ite1_2 ) );

					// Checking 2nd list
					assertThat( rev1.getReferences2(), CollectionMatchers.isEmpty() );
					assertThat( rev2.getReferences2(), CollectionMatchers.isEmpty() );
					assertThat( rev3.getReferences2(), CollectionMatchers.isEmpty() );
					assertThat( rev4.getReferences2(), contains( ite2_2 ) );
				}
		);
	}
}
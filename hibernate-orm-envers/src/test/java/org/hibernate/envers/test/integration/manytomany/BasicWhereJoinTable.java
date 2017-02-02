/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.manytomany;

import java.util.Arrays;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.IntNoAutoIdTestEntity;
import org.hibernate.envers.test.entities.manytomany.WhereJoinTableEntity;
import org.hibernate.envers.test.tools.TestTools;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class BasicWhereJoinTable extends BaseEnversJPAFunctionalTestCase {
	private Integer ite1_1_id;
	private Integer ite1_2_id;
	private Integer ite2_1_id;
	private Integer ite2_2_id;

	private Integer wjte1_id;
	private Integer wjte2_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {WhereJoinTableEntity.class, IntNoAutoIdTestEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		IntNoAutoIdTestEntity ite1_1 = new IntNoAutoIdTestEntity( 1, 10 );
		IntNoAutoIdTestEntity ite1_2 = new IntNoAutoIdTestEntity( 1, 11 );
		IntNoAutoIdTestEntity ite2_1 = new IntNoAutoIdTestEntity( 2, 20 );
		IntNoAutoIdTestEntity ite2_2 = new IntNoAutoIdTestEntity( 2, 21 );

		WhereJoinTableEntity wjte1 = new WhereJoinTableEntity();
		wjte1.setData( "wjte1" );

		WhereJoinTableEntity wjte2 = new WhereJoinTableEntity();
		wjte1.setData( "wjte2" );

		// Revision 1
		em.getTransaction().begin();

		em.persist( ite1_1 );
		em.persist( ite1_2 );
		em.persist( ite2_1 );
		em.persist( ite2_2 );
		em.persist( wjte1 );
		em.persist( wjte2 );

		em.getTransaction().commit();
		em.clear();

		// Revision 2 (wjte1: 1_1, 2_1)

		em.getTransaction().begin();

		wjte1 = em.find( WhereJoinTableEntity.class, wjte1.getId() );

		wjte1.getReferences1().add( ite1_1 );
		wjte1.getReferences2().add( ite2_1 );

		em.getTransaction().commit();
		em.clear();

		// Revision 3 (wjte1: 1_1, 2_1; wjte2: 1_1, 1_2)
		em.getTransaction().begin();

		wjte2 = em.find( WhereJoinTableEntity.class, wjte2.getId() );

		wjte2.getReferences1().add( ite1_1 );
		wjte2.getReferences1().add( ite1_2 );

		em.getTransaction().commit();
		em.clear();

		// Revision 4 (wjte1: 2_1; wjte2: 1_1, 1_2, 2_2)
		em.getTransaction().begin();

		wjte1 = em.find( WhereJoinTableEntity.class, wjte1.getId() );
		wjte2 = em.find( WhereJoinTableEntity.class, wjte2.getId() );

		wjte1.getReferences1().remove( ite1_1 );
		wjte2.getReferences2().add( ite2_2 );

		em.getTransaction().commit();
		em.clear();

		//

		ite1_1_id = ite1_1.getId();
		ite1_2_id = ite1_2.getId();
		ite2_1_id = ite2_1.getId();
		ite2_2_id = ite2_2.getId();

		wjte1_id = wjte1.getId();
		wjte2_id = wjte2.getId();
	}

	@Test
	public void testRevisionsCounts() {
		assertEquals( Arrays.asList( 1, 2, 4 ), getAuditReader().getRevisions( WhereJoinTableEntity.class, wjte1_id ) );
		assertEquals( Arrays.asList( 1, 3, 4 ), getAuditReader().getRevisions( WhereJoinTableEntity.class, wjte2_id ) );

		assertEquals( Arrays.asList( 1 ), getAuditReader().getRevisions( IntNoAutoIdTestEntity.class, ite1_1_id ) );
		assertEquals( Arrays.asList( 1 ), getAuditReader().getRevisions( IntNoAutoIdTestEntity.class, ite1_2_id ) );
		assertEquals( Arrays.asList( 1 ), getAuditReader().getRevisions( IntNoAutoIdTestEntity.class, ite2_1_id ) );
		assertEquals( Arrays.asList( 1 ), getAuditReader().getRevisions( IntNoAutoIdTestEntity.class, ite2_2_id ) );
	}

	@Test
	public void testHistoryOfWjte1() {
		IntNoAutoIdTestEntity ite1_1 = getEntityManager().find( IntNoAutoIdTestEntity.class, ite1_1_id );
		IntNoAutoIdTestEntity ite2_1 = getEntityManager().find( IntNoAutoIdTestEntity.class, ite2_1_id );

		WhereJoinTableEntity rev1 = getAuditReader().find( WhereJoinTableEntity.class, wjte1_id, 1 );
		WhereJoinTableEntity rev2 = getAuditReader().find( WhereJoinTableEntity.class, wjte1_id, 2 );
		WhereJoinTableEntity rev3 = getAuditReader().find( WhereJoinTableEntity.class, wjte1_id, 3 );
		WhereJoinTableEntity rev4 = getAuditReader().find( WhereJoinTableEntity.class, wjte1_id, 4 );

		// Checking 1st list
		assert TestTools.checkCollection( rev1.getReferences1() );
		assert TestTools.checkCollection( rev2.getReferences1(), ite1_1 );
		assert TestTools.checkCollection( rev3.getReferences1(), ite1_1 );
		assert TestTools.checkCollection( rev4.getReferences1() );

		// Checking 2nd list
		assert TestTools.checkCollection( rev1.getReferences2() );
		assert TestTools.checkCollection( rev2.getReferences2(), ite2_1 );
		assert TestTools.checkCollection( rev3.getReferences2(), ite2_1 );
		assert TestTools.checkCollection( rev4.getReferences2(), ite2_1 );
	}

	@Test
	public void testHistoryOfWjte2() {
		IntNoAutoIdTestEntity ite1_1 = getEntityManager().find( IntNoAutoIdTestEntity.class, ite1_1_id );
		IntNoAutoIdTestEntity ite1_2 = getEntityManager().find( IntNoAutoIdTestEntity.class, ite1_2_id );
		IntNoAutoIdTestEntity ite2_2 = getEntityManager().find( IntNoAutoIdTestEntity.class, ite2_2_id );

		WhereJoinTableEntity rev1 = getAuditReader().find( WhereJoinTableEntity.class, wjte2_id, 1 );
		WhereJoinTableEntity rev2 = getAuditReader().find( WhereJoinTableEntity.class, wjte2_id, 2 );
		WhereJoinTableEntity rev3 = getAuditReader().find( WhereJoinTableEntity.class, wjte2_id, 3 );
		WhereJoinTableEntity rev4 = getAuditReader().find( WhereJoinTableEntity.class, wjte2_id, 4 );

		// Checking 1st list
		assert TestTools.checkCollection( rev1.getReferences1() );
		assert TestTools.checkCollection( rev2.getReferences1() );
		assert TestTools.checkCollection( rev3.getReferences1(), ite1_1, ite1_2 );
		assert TestTools.checkCollection( rev4.getReferences1(), ite1_1, ite1_2 );

		// Checking 2nd list
		assert TestTools.checkCollection( rev1.getReferences2() );
		assert TestTools.checkCollection( rev2.getReferences2() );
		assert TestTools.checkCollection( rev3.getReferences2() );
		assert TestTools.checkCollection( rev4.getReferences2(), ite2_2 );
	}
}
/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.manytomany.ternary;

import java.util.Arrays;
import java.util.HashMap;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.IntTestPrivSeqEntity;
import org.hibernate.envers.test.entities.StrTestPrivSeqEntity;
import org.hibernate.envers.test.tools.TestTools;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class TernaryMapFlush extends BaseEnversJPAFunctionalTestCase {
	private Integer str1_id;
	private Integer str2_id;
	private Integer int1_id;
	private Integer int2_id;
	private Integer map1_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {TernaryMapEntity.class, StrTestPrivSeqEntity.class, IntTestPrivSeqEntity.class};
	}

	@Test
	@Priority(10)
	public void createData() {
		EntityManager em = getEntityManager();

		StrTestPrivSeqEntity str1 = new StrTestPrivSeqEntity( "a" );
		StrTestPrivSeqEntity str2 = new StrTestPrivSeqEntity( "b" );
		IntTestPrivSeqEntity int1 = new IntTestPrivSeqEntity( 1 );
		IntTestPrivSeqEntity int2 = new IntTestPrivSeqEntity( 2 );
		TernaryMapEntity map1 = new TernaryMapEntity();

		// Revision 1 (int1 -> str1)
		em.getTransaction().begin();

		em.persist( str1 );
		em.persist( str2 );
		em.persist( int1 );
		em.persist( int2 );

		map1.getMap().put( int1, str1 );

		em.persist( map1 );

		em.getTransaction().commit();

		// Revision 2 (removing int1->str1, flushing, adding int1->str1 again and a new int2->str2 mapping to force a change)

		em.getTransaction().begin();

		map1 = em.find( TernaryMapEntity.class, map1.getId() );
		str1 = em.find( StrTestPrivSeqEntity.class, str1.getId() );
		int1 = em.find( IntTestPrivSeqEntity.class, int1.getId() );

		map1.setMap( new HashMap<IntTestPrivSeqEntity, StrTestPrivSeqEntity>() );

		em.flush();

		map1.getMap().put( int1, str1 );
		map1.getMap().put( int2, str2 );

		em.getTransaction().commit();

		// Revision 3 (removing int1->str1, flushing, overwriting int2->str1)

		em.getTransaction().begin();

		map1 = em.find( TernaryMapEntity.class, map1.getId() );
		str1 = em.find( StrTestPrivSeqEntity.class, str1.getId() );
		int1 = em.find( IntTestPrivSeqEntity.class, int1.getId() );

		map1.getMap().remove( int1 );

		em.flush();

		map1.getMap().put( int2, str1 );

		em.getTransaction().commit();

		//

		map1_id = map1.getId();
		str1_id = str1.getId();
		str2_id = str2.getId();
		int1_id = int1.getId();
		int2_id = int2.getId();
	}

	@Test
	public void testRevisionsCounts() {
		assertEquals( Arrays.asList( 1, 2, 3 ), getAuditReader().getRevisions( TernaryMapEntity.class, map1_id ) );
		assertEquals( Arrays.asList( 1 ), getAuditReader().getRevisions( StrTestPrivSeqEntity.class, str1_id ) );
		assertEquals( Arrays.asList( 1 ), getAuditReader().getRevisions( StrTestPrivSeqEntity.class, str2_id ) );
		assertEquals( Arrays.asList( 1 ), getAuditReader().getRevisions( IntTestPrivSeqEntity.class, int1_id ) );
		assertEquals( Arrays.asList( 1 ), getAuditReader().getRevisions( IntTestPrivSeqEntity.class, int2_id ) );
	}

	@Test
	public void testHistoryOfMap1() {
		StrTestPrivSeqEntity str1 = getEntityManager().find( StrTestPrivSeqEntity.class, str1_id );
		StrTestPrivSeqEntity str2 = getEntityManager().find( StrTestPrivSeqEntity.class, str2_id );
		IntTestPrivSeqEntity int1 = getEntityManager().find( IntTestPrivSeqEntity.class, int1_id );
		IntTestPrivSeqEntity int2 = getEntityManager().find( IntTestPrivSeqEntity.class, int2_id );

		TernaryMapEntity rev1 = getAuditReader().find( TernaryMapEntity.class, map1_id, 1 );
		TernaryMapEntity rev2 = getAuditReader().find( TernaryMapEntity.class, map1_id, 2 );
		TernaryMapEntity rev3 = getAuditReader().find( TernaryMapEntity.class, map1_id, 3 );

		assertEquals( rev1.getMap(), TestTools.makeMap( int1, str1 ) );
		assertEquals( rev2.getMap(), TestTools.makeMap( int1, str1, int2, str2 ) );
		assertEquals( rev3.getMap(), TestTools.makeMap( int2, str1 ) );
	}
}
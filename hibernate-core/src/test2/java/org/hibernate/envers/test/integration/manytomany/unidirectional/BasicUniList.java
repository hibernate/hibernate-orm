/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.manytomany.unidirectional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.StrTestEntity;
import org.hibernate.envers.test.entities.manytomany.unidirectional.ListUniEntity;
import org.hibernate.envers.test.tools.TestTools;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class BasicUniList extends BaseEnversJPAFunctionalTestCase {
	private Integer ed1_id;
	private Integer ed2_id;

	private Integer ing1_id;
	private Integer ing2_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {ListUniEntity.class, StrTestEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		StrTestEntity ed1 = new StrTestEntity( "data_ed_1" );
		StrTestEntity ed2 = new StrTestEntity( "data_ed_2" );

		ListUniEntity ing1 = new ListUniEntity( 3, "data_ing_1" );
		ListUniEntity ing2 = new ListUniEntity( 4, "data_ing_2" );

		// Revision 1
		em.getTransaction().begin();

		em.persist( ed1 );
		em.persist( ed2 );
		em.persist( ing1 );
		em.persist( ing2 );

		em.getTransaction().commit();

		// Revision 2

		em.getTransaction().begin();

		ing1 = em.find( ListUniEntity.class, ing1.getId() );
		ing2 = em.find( ListUniEntity.class, ing2.getId() );
		ed1 = em.find( StrTestEntity.class, ed1.getId() );
		ed2 = em.find( StrTestEntity.class, ed2.getId() );

		ing1.setReferences( new ArrayList<StrTestEntity>() );
		ing1.getReferences().add( ed1 );

		ing2.setReferences( new ArrayList<StrTestEntity>() );
		ing2.getReferences().add( ed1 );
		ing2.getReferences().add( ed2 );

		em.getTransaction().commit();

		// Revision 3
		em.getTransaction().begin();

		ing1 = em.find( ListUniEntity.class, ing1.getId() );
		ed2 = em.find( StrTestEntity.class, ed2.getId() );
		ed1 = em.find( StrTestEntity.class, ed1.getId() );

		ing1.getReferences().add( ed2 );

		em.getTransaction().commit();

		// Revision 4
		em.getTransaction().begin();

		ing1 = em.find( ListUniEntity.class, ing1.getId() );
		ed2 = em.find( StrTestEntity.class, ed2.getId() );
		ed1 = em.find( StrTestEntity.class, ed1.getId() );

		ing1.getReferences().remove( ed1 );

		em.getTransaction().commit();

		// Revision 5
		em.getTransaction().begin();

		ing1 = em.find( ListUniEntity.class, ing1.getId() );

		ing1.setReferences( null );

		em.getTransaction().commit();

		//

		ed1_id = ed1.getId();
		ed2_id = ed2.getId();

		ing1_id = ing1.getId();
		ing2_id = ing2.getId();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1 ).equals( getAuditReader().getRevisions( StrTestEntity.class, ed1_id ) );
		assert Arrays.asList( 1 ).equals( getAuditReader().getRevisions( StrTestEntity.class, ed2_id ) );

		assert Arrays.asList( 1, 2, 3, 4, 5 ).equals( getAuditReader().getRevisions( ListUniEntity.class, ing1_id ) );
		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( ListUniEntity.class, ing2_id ) );
	}

	@Test
	public void testHistoryOfEdIng1() {
		StrTestEntity ed1 = getEntityManager().find( StrTestEntity.class, ed1_id );
		StrTestEntity ed2 = getEntityManager().find( StrTestEntity.class, ed2_id );

		ListUniEntity rev1 = getAuditReader().find( ListUniEntity.class, ing1_id, 1 );
		ListUniEntity rev2 = getAuditReader().find( ListUniEntity.class, ing1_id, 2 );
		ListUniEntity rev3 = getAuditReader().find( ListUniEntity.class, ing1_id, 3 );
		ListUniEntity rev4 = getAuditReader().find( ListUniEntity.class, ing1_id, 4 );
		ListUniEntity rev5 = getAuditReader().find( ListUniEntity.class, ing1_id, 5 );

		assert rev1.getReferences().equals( Collections.EMPTY_LIST );
		assert TestTools.checkCollection( rev2.getReferences(), ed1 );
		assert TestTools.checkCollection( rev3.getReferences(), ed1, ed2 );
		assert TestTools.checkCollection( rev4.getReferences(), ed2 );
		assert rev5.getReferences().equals( Collections.EMPTY_LIST );
	}

	@Test
	public void testHistoryOfEdIng2() {
		StrTestEntity ed1 = getEntityManager().find( StrTestEntity.class, ed1_id );
		StrTestEntity ed2 = getEntityManager().find( StrTestEntity.class, ed2_id );

		ListUniEntity rev1 = getAuditReader().find( ListUniEntity.class, ing2_id, 1 );
		ListUniEntity rev2 = getAuditReader().find( ListUniEntity.class, ing2_id, 2 );
		ListUniEntity rev3 = getAuditReader().find( ListUniEntity.class, ing2_id, 3 );
		ListUniEntity rev4 = getAuditReader().find( ListUniEntity.class, ing2_id, 4 );
		ListUniEntity rev5 = getAuditReader().find( ListUniEntity.class, ing2_id, 5 );

		assert rev1.getReferences().equals( Collections.EMPTY_LIST );
		assert TestTools.checkCollection( rev2.getReferences(), ed1, ed2 );
		assert TestTools.checkCollection( rev3.getReferences(), ed1, ed2 );
		assert TestTools.checkCollection( rev4.getReferences(), ed1, ed2 );
		assert TestTools.checkCollection( rev5.getReferences(), ed1, ed2 );
	}
}
/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.onetomany;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.onetomany.SetRefEdEntity;
import org.hibernate.envers.test.entities.onetomany.SetRefIngEntity;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class BasicSetWithNullsDelete extends BaseEnversJPAFunctionalTestCase {
	private Integer ed1_id;
	private Integer ed2_id;

	private Integer ing1_id;
	private Integer ing2_id;
	private Integer ing3_id;
	private Integer ing4_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {SetRefEdEntity.class, SetRefIngEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		SetRefEdEntity ed1 = new SetRefEdEntity( 1, "data_ed_1" );
		SetRefEdEntity ed2 = new SetRefEdEntity( 2, "data_ed_2" );

		SetRefIngEntity ing1 = new SetRefIngEntity( 3, "data_ing_1", ed1 );
		SetRefIngEntity ing2 = new SetRefIngEntity( 4, "data_ing_2", ed1 );
		SetRefIngEntity ing3 = new SetRefIngEntity( 5, "data_ing_3", ed1 );
		SetRefIngEntity ing4 = new SetRefIngEntity( 6, "data_ing_4", ed1 );

		// Revision 1
		em.getTransaction().begin();

		em.persist( ed1 );
		em.persist( ed2 );

		em.persist( ing1 );
		em.persist( ing2 );
		em.persist( ing3 );
		em.persist( ing4 );

		em.getTransaction().commit();

		// Revision 2
		em.getTransaction().begin();

		ing1 = em.find( SetRefIngEntity.class, ing1.getId() );

		ing1.setReference( null );

		em.getTransaction().commit();

		// Revision 3
		em.getTransaction().begin();

		ing2 = em.find( SetRefIngEntity.class, ing2.getId() );
		em.remove( ing2 );

		em.getTransaction().commit();

		// Revision 4
		em.getTransaction().begin();

		ing3 = em.find( SetRefIngEntity.class, ing3.getId() );
		ed2 = em.find( SetRefEdEntity.class, ed2.getId() );
		ing3.setReference( ed2 );

		em.getTransaction().commit();
		// Revision 5
		em.getTransaction().begin();

		ing4 = em.find( SetRefIngEntity.class, ing4.getId() );
		ed1 = em.find( SetRefEdEntity.class, ed1.getId() );
		em.remove( ed1 );
		ing4.setReference( null );

		em.getTransaction().commit();

		//

		ed1_id = ed1.getId();
		ed2_id = ed2.getId();

		ing1_id = ing1.getId();
		ing2_id = ing2.getId();
		ing3_id = ing3.getId();
		ing4_id = ing4.getId();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2, 3, 4, 5 ).equals( getAuditReader().getRevisions( SetRefEdEntity.class, ed1_id ) );
		assert Arrays.asList( 1, 4 ).equals( getAuditReader().getRevisions( SetRefEdEntity.class, ed2_id ) );

		assert Arrays.asList( 1, 2 ).equals( getAuditReader().getRevisions( SetRefIngEntity.class, ing1_id ) );
		assert Arrays.asList( 1, 3 ).equals( getAuditReader().getRevisions( SetRefIngEntity.class, ing2_id ) );
		assert Arrays.asList( 1, 4 ).equals( getAuditReader().getRevisions( SetRefIngEntity.class, ing3_id ) );
		assert Arrays.asList( 1, 5 ).equals( getAuditReader().getRevisions( SetRefIngEntity.class, ing4_id ) );
	}

	private <T> Set<T> makeSet(T... objects) {
		Set<T> ret = new HashSet<T>();
		//noinspection ManualArrayToCollectionCopy
		for ( T obj : objects ) {
			ret.add( obj );
		}
		return ret;
	}

	@Test
	public void testHistoryOfEdId1() {
		SetRefIngEntity ing1 = getEntityManager().find( SetRefIngEntity.class, ing1_id );
		SetRefIngEntity ing2 = new SetRefIngEntity( 4, "data_ing_2", new SetRefEdEntity( 1, "data_ed_1" ) );
		SetRefIngEntity ing3 = getEntityManager().find( SetRefIngEntity.class, ing3_id );
		SetRefIngEntity ing4 = getEntityManager().find( SetRefIngEntity.class, ing4_id );

		SetRefEdEntity rev1 = getAuditReader().find( SetRefEdEntity.class, ed1_id, 1 );
		SetRefEdEntity rev2 = getAuditReader().find( SetRefEdEntity.class, ed1_id, 2 );
		SetRefEdEntity rev3 = getAuditReader().find( SetRefEdEntity.class, ed1_id, 3 );
		SetRefEdEntity rev4 = getAuditReader().find( SetRefEdEntity.class, ed1_id, 4 );
		SetRefEdEntity rev5 = getAuditReader().find( SetRefEdEntity.class, ed1_id, 5 );

		assert rev1.getReffering().equals( makeSet( ing1, ing2, ing3, ing4 ) );
		assert rev2.getReffering().equals( makeSet( ing2, ing3, ing4 ) );
		assert rev3.getReffering().equals( makeSet( ing3, ing4 ) );
		assert rev4.getReffering().equals( makeSet( ing4 ) );
		assert rev5 == null;
	}

	@Test
	public void testHistoryOfEdId2() {
		SetRefIngEntity ing3 = getEntityManager().find( SetRefIngEntity.class, ing3_id );

		SetRefEdEntity rev1 = getAuditReader().find( SetRefEdEntity.class, ed2_id, 1 );
		SetRefEdEntity rev2 = getAuditReader().find( SetRefEdEntity.class, ed2_id, 2 );
		SetRefEdEntity rev3 = getAuditReader().find( SetRefEdEntity.class, ed2_id, 3 );
		SetRefEdEntity rev4 = getAuditReader().find( SetRefEdEntity.class, ed2_id, 4 );
		SetRefEdEntity rev5 = getAuditReader().find( SetRefEdEntity.class, ed2_id, 5 );

		assert rev1.getReffering().equals( Collections.EMPTY_SET );
		assert rev2.getReffering().equals( Collections.EMPTY_SET );
		assert rev3.getReffering().equals( Collections.EMPTY_SET );
		assert rev4.getReffering().equals( makeSet( ing3 ) );
		assert rev5.getReffering().equals( makeSet( ing3 ) );
	}

	@Test
	public void testHistoryOfEdIng1() {
		SetRefEdEntity ed1 = new SetRefEdEntity( 1, "data_ed_1" );

		SetRefIngEntity rev1 = getAuditReader().find( SetRefIngEntity.class, ing1_id, 1 );
		SetRefIngEntity rev2 = getAuditReader().find( SetRefIngEntity.class, ing1_id, 2 );
		SetRefIngEntity rev3 = getAuditReader().find( SetRefIngEntity.class, ing1_id, 3 );
		SetRefIngEntity rev4 = getAuditReader().find( SetRefIngEntity.class, ing1_id, 4 );
		SetRefIngEntity rev5 = getAuditReader().find( SetRefIngEntity.class, ing1_id, 5 );

		assert rev1.getReference().equals( ed1 );
		assert rev2.getReference() == null;
		assert rev3.getReference() == null;
		assert rev4.getReference() == null;
		assert rev5.getReference() == null;
	}

	@Test
	public void testHistoryOfEdIng2() {
		SetRefEdEntity ed1 = new SetRefEdEntity( 1, "data_ed_1" );

		SetRefIngEntity rev1 = getAuditReader().find( SetRefIngEntity.class, ing2_id, 1 );
		SetRefIngEntity rev2 = getAuditReader().find( SetRefIngEntity.class, ing2_id, 2 );
		SetRefIngEntity rev3 = getAuditReader().find( SetRefIngEntity.class, ing2_id, 3 );
		SetRefIngEntity rev4 = getAuditReader().find( SetRefIngEntity.class, ing2_id, 4 );
		SetRefIngEntity rev5 = getAuditReader().find( SetRefIngEntity.class, ing2_id, 5 );

		assert rev1.getReference().equals( ed1 );
		assert rev2.getReference().equals( ed1 );
		assert rev3 == null;
		assert rev4 == null;
		assert rev5 == null;
	}

	@Test
	public void testHistoryOfEdIng3() {
		SetRefEdEntity ed1 = new SetRefEdEntity( 1, "data_ed_1" );
		SetRefEdEntity ed2 = new SetRefEdEntity( 2, "data_ed_2" );

		SetRefIngEntity rev1 = getAuditReader().find( SetRefIngEntity.class, ing3_id, 1 );
		SetRefIngEntity rev2 = getAuditReader().find( SetRefIngEntity.class, ing3_id, 2 );
		SetRefIngEntity rev3 = getAuditReader().find( SetRefIngEntity.class, ing3_id, 3 );
		SetRefIngEntity rev4 = getAuditReader().find( SetRefIngEntity.class, ing3_id, 4 );
		SetRefIngEntity rev5 = getAuditReader().find( SetRefIngEntity.class, ing3_id, 5 );

		assert rev1.getReference().equals( ed1 );
		assert rev2.getReference().equals( ed1 );
		assert rev3.getReference().equals( ed1 );
		assert rev4.getReference().equals( ed2 );
		assert rev5.getReference().equals( ed2 );
	}

	@Test
	public void testHistoryOfEdIng4() {
		SetRefEdEntity ed1 = new SetRefEdEntity( 1, "data_ed_1" );

		SetRefIngEntity rev1 = getAuditReader().find( SetRefIngEntity.class, ing4_id, 1 );
		SetRefIngEntity rev2 = getAuditReader().find( SetRefIngEntity.class, ing4_id, 2 );
		SetRefIngEntity rev3 = getAuditReader().find( SetRefIngEntity.class, ing4_id, 3 );
		SetRefIngEntity rev4 = getAuditReader().find( SetRefIngEntity.class, ing4_id, 4 );
		SetRefIngEntity rev5 = getAuditReader().find( SetRefIngEntity.class, ing4_id, 5 );

		assert rev1.getReference().equals( ed1 );
		assert rev2.getReference().equals( ed1 );
		assert rev3.getReference().equals( ed1 );
		assert rev4.getReference().equals( ed1 );
		assert rev5.getReference() == null;
	}
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.manytomany;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.manytomany.MapOwnedEntity;
import org.hibernate.envers.test.entities.manytomany.MapOwningEntity;
import org.hibernate.envers.test.tools.TestTools;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class BasicMap extends BaseEnversJPAFunctionalTestCase {
	private Integer ed1_id;
	private Integer ed2_id;

	private Integer ing1_id;
	private Integer ing2_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {MapOwningEntity.class, MapOwnedEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		MapOwnedEntity ed1 = new MapOwnedEntity( 1, "data_ed_1" );
		MapOwnedEntity ed2 = new MapOwnedEntity( 2, "data_ed_2" );

		MapOwningEntity ing1 = new MapOwningEntity( 3, "data_ing_1" );
		MapOwningEntity ing2 = new MapOwningEntity( 4, "data_ing_2" );

		// Revision 1 (ing1: initialy empty, ing2: one mapping)
		em.getTransaction().begin();

		ing2.getReferences().put( "2", ed2 );

		em.persist( ed1 );
		em.persist( ed2 );
		em.persist( ing1 );
		em.persist( ing2 );

		em.getTransaction().commit();

		// Revision 2 (ing1: adding two mappings, ing2: replacing an existing mapping)

		em.getTransaction().begin();

		ing1 = em.find( MapOwningEntity.class, ing1.getId() );
		ing2 = em.find( MapOwningEntity.class, ing2.getId() );
		ed1 = em.find( MapOwnedEntity.class, ed1.getId() );
		ed2 = em.find( MapOwnedEntity.class, ed2.getId() );

		ing1.getReferences().put( "1", ed1 );
		ing1.getReferences().put( "2", ed1 );

		ing2.getReferences().put( "2", ed1 );

		em.getTransaction().commit();

		// No revision (ing1: adding an existing mapping, ing2: removing a non existing mapping)
		em.getTransaction().begin();

		ing1 = em.find( MapOwningEntity.class, ing1.getId() );
		ing2 = em.find( MapOwningEntity.class, ing2.getId() );

		ing1.getReferences().put( "1", ed1 );

		ing2.getReferences().remove( "3" );

		em.getTransaction().commit();

		// Revision 3 (ing1: clearing, ing2: replacing with a new map)
		em.getTransaction().begin();

		ing1 = em.find( MapOwningEntity.class, ing1.getId() );
		ed1 = em.find( MapOwnedEntity.class, ed1.getId() );

		ing1.getReferences().clear();
		ing2.setReferences( new HashMap<String, MapOwnedEntity>() );
		ing2.getReferences().put( "1", ed2 );

		em.getTransaction().commit();
		//

		ed1_id = ed1.getId();
		ed2_id = ed2.getId();

		ing1_id = ing1.getId();
		ing2_id = ing2.getId();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2, 3 ).equals( getAuditReader().getRevisions( MapOwnedEntity.class, ed1_id ) );
		assert Arrays.asList( 1, 2, 3 ).equals( getAuditReader().getRevisions( MapOwnedEntity.class, ed2_id ) );

		assert Arrays.asList( 1, 2, 3 ).equals( getAuditReader().getRevisions( MapOwningEntity.class, ing1_id ) );
		assert Arrays.asList( 1, 2, 3 ).equals( getAuditReader().getRevisions( MapOwningEntity.class, ing2_id ) );
	}

	@Test
	public void testHistoryOfEdId1() {
		MapOwningEntity ing1 = getEntityManager().find( MapOwningEntity.class, ing1_id );
		MapOwningEntity ing2 = getEntityManager().find( MapOwningEntity.class, ing2_id );

		MapOwnedEntity rev1 = getAuditReader().find( MapOwnedEntity.class, ed1_id, 1 );
		MapOwnedEntity rev2 = getAuditReader().find( MapOwnedEntity.class, ed1_id, 2 );
		MapOwnedEntity rev3 = getAuditReader().find( MapOwnedEntity.class, ed1_id, 3 );

		assert rev1.getReferencing().equals( Collections.EMPTY_SET );
		assert rev2.getReferencing().equals( TestTools.makeSet( ing1, ing2 ) );
		assert rev3.getReferencing().equals( Collections.EMPTY_SET );
	}

	@Test
	public void testHistoryOfEdId2() {
		MapOwningEntity ing2 = getEntityManager().find( MapOwningEntity.class, ing2_id );

		MapOwnedEntity rev1 = getAuditReader().find( MapOwnedEntity.class, ed2_id, 1 );
		MapOwnedEntity rev2 = getAuditReader().find( MapOwnedEntity.class, ed2_id, 2 );
		MapOwnedEntity rev3 = getAuditReader().find( MapOwnedEntity.class, ed2_id, 3 );

		assert rev1.getReferencing().equals( TestTools.makeSet( ing2 ) );
		assert rev2.getReferencing().equals( Collections.EMPTY_SET );
		assert rev3.getReferencing().equals( TestTools.makeSet( ing2 ) );
	}

	@Test
	public void testHistoryOfEdIng1() {
		MapOwnedEntity ed1 = getEntityManager().find( MapOwnedEntity.class, ed1_id );

		MapOwningEntity rev1 = getAuditReader().find( MapOwningEntity.class, ing1_id, 1 );
		MapOwningEntity rev2 = getAuditReader().find( MapOwningEntity.class, ing1_id, 2 );
		MapOwningEntity rev3 = getAuditReader().find( MapOwningEntity.class, ing1_id, 3 );

		assert rev1.getReferences().equals( Collections.EMPTY_MAP );
		assert rev2.getReferences().equals( TestTools.makeMap( "1", ed1, "2", ed1 ) );
		assert rev3.getReferences().equals( Collections.EMPTY_MAP );
	}

	@Test
	public void testHistoryOfEdIng2() {
		MapOwnedEntity ed1 = getEntityManager().find( MapOwnedEntity.class, ed1_id );
		MapOwnedEntity ed2 = getEntityManager().find( MapOwnedEntity.class, ed2_id );

		MapOwningEntity rev1 = getAuditReader().find( MapOwningEntity.class, ing2_id, 1 );
		MapOwningEntity rev2 = getAuditReader().find( MapOwningEntity.class, ing2_id, 2 );
		MapOwningEntity rev3 = getAuditReader().find( MapOwningEntity.class, ing2_id, 3 );

		assert rev1.getReferences().equals( TestTools.makeMap( "2", ed2 ) );
		assert rev2.getReferences().equals( TestTools.makeMap( "2", ed1 ) );
		assert rev3.getReferences().equals( TestTools.makeMap( "1", ed2 ) );
	}
}
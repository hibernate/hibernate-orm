/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.collection;

import java.util.Arrays;
import java.util.Collections;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.collection.StringMapEntity;
import org.hibernate.envers.test.tools.TestTools;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class StringMap extends BaseEnversJPAFunctionalTestCase {
	private Integer sme1_id;
	private Integer sme2_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {StringMapEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		StringMapEntity sme1 = new StringMapEntity();
		StringMapEntity sme2 = new StringMapEntity();

		// Revision 1 (sme1: initialy empty, sme2: initialy 1 mapping)
		em.getTransaction().begin();

		sme2.getStrings().put( "1", "a" );

		em.persist( sme1 );
		em.persist( sme2 );

		em.getTransaction().commit();

		// Revision 2 (sme1: adding 2 mappings, sme2: no changes)
		em.getTransaction().begin();

		sme1 = em.find( StringMapEntity.class, sme1.getId() );
		sme2 = em.find( StringMapEntity.class, sme2.getId() );

		sme1.getStrings().put( "1", "a" );
		sme1.getStrings().put( "2", "b" );

		em.getTransaction().commit();

		// Revision 3 (sme1: removing an existing mapping, sme2: replacing a value)
		em.getTransaction().begin();

		sme1 = em.find( StringMapEntity.class, sme1.getId() );
		sme2 = em.find( StringMapEntity.class, sme2.getId() );

		sme1.getStrings().remove( "1" );
		sme2.getStrings().put( "1", "b" );

		em.getTransaction().commit();

		// No revision (sme1: removing a non-existing mapping, sme2: replacing with the same value)
		em.getTransaction().begin();

		sme1 = em.find( StringMapEntity.class, sme1.getId() );
		sme2 = em.find( StringMapEntity.class, sme2.getId() );

		sme1.getStrings().remove( "3" );
		sme2.getStrings().put( "1", "b" );

		em.getTransaction().commit();

		//

		sme1_id = sme1.getId();
		sme2_id = sme2.getId();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2, 3 ).equals( getAuditReader().getRevisions( StringMapEntity.class, sme1_id ) );
		assert Arrays.asList( 1, 3 ).equals( getAuditReader().getRevisions( StringMapEntity.class, sme2_id ) );
	}

	@Test
	public void testHistoryOfSse1() {
		StringMapEntity rev1 = getAuditReader().find( StringMapEntity.class, sme1_id, 1 );
		StringMapEntity rev2 = getAuditReader().find( StringMapEntity.class, sme1_id, 2 );
		StringMapEntity rev3 = getAuditReader().find( StringMapEntity.class, sme1_id, 3 );
		StringMapEntity rev4 = getAuditReader().find( StringMapEntity.class, sme1_id, 4 );

		assert rev1.getStrings().equals( Collections.EMPTY_MAP );
		assert rev2.getStrings().equals( TestTools.makeMap( "1", "a", "2", "b" ) );
		assert rev3.getStrings().equals( TestTools.makeMap( "2", "b" ) );
		assert rev4.getStrings().equals( TestTools.makeMap( "2", "b" ) );
	}

	@Test
	public void testHistoryOfSse2() {
		StringMapEntity rev1 = getAuditReader().find( StringMapEntity.class, sme2_id, 1 );
		StringMapEntity rev2 = getAuditReader().find( StringMapEntity.class, sme2_id, 2 );
		StringMapEntity rev3 = getAuditReader().find( StringMapEntity.class, sme2_id, 3 );
		StringMapEntity rev4 = getAuditReader().find( StringMapEntity.class, sme2_id, 4 );

		assert rev1.getStrings().equals( TestTools.makeMap( "1", "a" ) );
		assert rev2.getStrings().equals( TestTools.makeMap( "1", "a" ) );
		assert rev3.getStrings().equals( TestTools.makeMap( "1", "b" ) );
		assert rev4.getStrings().equals( TestTools.makeMap( "1", "b" ) );
	}
}
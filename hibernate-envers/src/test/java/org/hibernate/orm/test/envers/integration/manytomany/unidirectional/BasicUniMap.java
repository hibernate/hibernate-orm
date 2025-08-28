/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.envers.integration.manytomany.unidirectional;

import java.util.Arrays;
import java.util.HashMap;
import jakarta.persistence.EntityManager;

import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.hibernate.orm.test.envers.entities.StrTestEntity;
import org.hibernate.orm.test.envers.entities.manytomany.unidirectional.MapUniEntity;
import org.hibernate.orm.test.envers.tools.TestTools;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class BasicUniMap extends BaseEnversJPAFunctionalTestCase {
	private Integer str1_id;
	private Integer str2_id;

	private Integer coll1_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {StrTestEntity.class, MapUniEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		StrTestEntity str1 = new StrTestEntity( "str1" );
		StrTestEntity str2 = new StrTestEntity( "str2" );

		MapUniEntity coll1 = new MapUniEntity( 3, "coll1" );

		// Revision 1 (coll1: initialy one mapping)
		em.getTransaction().begin();

		em.persist( str1 );
		em.persist( str2 );

		coll1.setMap( new HashMap<String, StrTestEntity>() );
		coll1.getMap().put( "1", str1 );
		em.persist( coll1 );

		em.getTransaction().commit();

		// Revision 2 (coll1: adding one mapping)
		em.getTransaction().begin();

		str2 = em.find( StrTestEntity.class, str2.getId() );
		coll1 = em.find( MapUniEntity.class, coll1.getId() );

		coll1.getMap().put( "2", str2 );

		em.getTransaction().commit();

		// Revision 3 (coll1: replacing one mapping)
		em.getTransaction().begin();

		str1 = em.find( StrTestEntity.class, str1.getId() );
		coll1 = em.find( MapUniEntity.class, coll1.getId() );

		coll1.getMap().put( "2", str1 );

		em.getTransaction().commit();

		// Revision 4 (coll1: removing one mapping)
		em.getTransaction().begin();

		coll1 = em.find( MapUniEntity.class, coll1.getId() );

		coll1.getMap().remove( "1" );

		em.getTransaction().commit();

		//

		str1_id = str1.getId();
		str2_id = str2.getId();

		coll1_id = coll1.getId();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2, 3, 4 ).equals( getAuditReader().getRevisions( MapUniEntity.class, coll1_id ) );

		assert Arrays.asList( 1 ).equals( getAuditReader().getRevisions( StrTestEntity.class, str1_id ) );
		assert Arrays.asList( 1 ).equals( getAuditReader().getRevisions( StrTestEntity.class, str2_id ) );
	}

	@Test
	public void testHistoryOfColl1() {
		StrTestEntity str1 = getEntityManager().find( StrTestEntity.class, str1_id );
		StrTestEntity str2 = getEntityManager().find( StrTestEntity.class, str2_id );

		MapUniEntity rev1 = getAuditReader().find( MapUniEntity.class, coll1_id, 1 );
		MapUniEntity rev2 = getAuditReader().find( MapUniEntity.class, coll1_id, 2 );
		MapUniEntity rev3 = getAuditReader().find( MapUniEntity.class, coll1_id, 3 );
		MapUniEntity rev4 = getAuditReader().find( MapUniEntity.class, coll1_id, 4 );

		assert rev1.getMap().equals( TestTools.makeMap( "1", str1 ) );
		assert rev2.getMap().equals( TestTools.makeMap( "1", str1, "2", str2 ) );
		assert rev3.getMap().equals( TestTools.makeMap( "1", str1, "2", str1 ) );
		assert rev4.getMap().equals( TestTools.makeMap( "2", str1 ) );

		assert "coll1".equals( rev1.getData() );
		assert "coll1".equals( rev2.getData() );
		assert "coll1".equals( rev3.getData() );
		assert "coll1".equals( rev4.getData() );
	}
}

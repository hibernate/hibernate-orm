/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.onetomany.detached;

import java.util.ArrayList;
import java.util.Arrays;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.StrTestEntity;
import org.hibernate.envers.test.entities.onetomany.detached.ListRefCollEntity;
import org.hibernate.envers.test.tools.TestTools;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class BasicDetachedList extends BaseEnversJPAFunctionalTestCase {
	private Integer str1_id;
	private Integer str2_id;

	private Integer coll1_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {StrTestEntity.class, ListRefCollEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		StrTestEntity str1 = new StrTestEntity( "str1" );
		StrTestEntity str2 = new StrTestEntity( "str2" );

		ListRefCollEntity coll1 = new ListRefCollEntity( 3, "coll1" );

		// Revision 1
		em.getTransaction().begin();

		em.persist( str1 );
		em.persist( str2 );

		coll1.setCollection( new ArrayList<StrTestEntity>() );
		coll1.getCollection().add( str1 );
		em.persist( coll1 );

		em.getTransaction().commit();

		// Revision 2
		em.getTransaction().begin();

		str2 = em.find( StrTestEntity.class, str2.getId() );
		coll1 = em.find( ListRefCollEntity.class, coll1.getId() );

		coll1.getCollection().add( str2 );

		em.getTransaction().commit();

		// Revision 3
		em.getTransaction().begin();

		str1 = em.find( StrTestEntity.class, str1.getId() );
		coll1 = em.find( ListRefCollEntity.class, coll1.getId() );

		coll1.getCollection().remove( str1 );

		em.getTransaction().commit();

		// Revision 4
		em.getTransaction().begin();

		coll1 = em.find( ListRefCollEntity.class, coll1.getId() );

		coll1.getCollection().clear();

		em.getTransaction().commit();

		//

		str1_id = str1.getId();
		str2_id = str2.getId();

		coll1_id = coll1.getId();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2, 3, 4 ).equals( getAuditReader().getRevisions( ListRefCollEntity.class, coll1_id ) );

		assert Arrays.asList( 1 ).equals( getAuditReader().getRevisions( StrTestEntity.class, str1_id ) );
		assert Arrays.asList( 1 ).equals( getAuditReader().getRevisions( StrTestEntity.class, str2_id ) );
	}

	@Test
	public void testHistoryOfColl1() {
		StrTestEntity str1 = getEntityManager().find( StrTestEntity.class, str1_id );
		StrTestEntity str2 = getEntityManager().find( StrTestEntity.class, str2_id );

		ListRefCollEntity rev1 = getAuditReader().find( ListRefCollEntity.class, coll1_id, 1 );
		ListRefCollEntity rev2 = getAuditReader().find( ListRefCollEntity.class, coll1_id, 2 );
		ListRefCollEntity rev3 = getAuditReader().find( ListRefCollEntity.class, coll1_id, 3 );
		ListRefCollEntity rev4 = getAuditReader().find( ListRefCollEntity.class, coll1_id, 4 );

		assert TestTools.checkCollection( rev1.getCollection(), str1 );
		assert TestTools.checkCollection( rev2.getCollection(), str1, str2 );
		assert TestTools.checkCollection( rev3.getCollection(), str2 );
		assert TestTools.checkCollection( rev4.getCollection() );

		assert "coll1".equals( rev1.getData() );
		assert "coll1".equals( rev2.getData() );
		assert "coll1".equals( rev3.getData() );
		assert "coll1".equals( rev4.getData() );
	}
}
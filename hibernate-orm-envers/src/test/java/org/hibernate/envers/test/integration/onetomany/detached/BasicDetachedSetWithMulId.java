/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.onetomany.detached;

import java.util.Arrays;
import java.util.HashSet;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.ids.MulId;
import org.hibernate.envers.test.entities.ids.MulIdTestEntity;
import org.hibernate.envers.test.entities.onetomany.detached.ids.SetRefCollEntityMulId;
import org.hibernate.envers.test.tools.TestTools;

import org.junit.Test;

/**
 * @author Adam Warski (adam at warski dot org)
 */
public class BasicDetachedSetWithMulId extends BaseEnversJPAFunctionalTestCase {
	private MulId str1_id;
	private MulId str2_id;

	private MulId coll1_id;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {MulIdTestEntity.class, SetRefCollEntityMulId.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		str1_id = new MulId( 1, 2 );
		str2_id = new MulId( 3, 4 );

		coll1_id = new MulId( 5, 6 );

		MulIdTestEntity str1 = new MulIdTestEntity( str1_id.getId1(), str1_id.getId2(), "str1" );
		MulIdTestEntity str2 = new MulIdTestEntity( str2_id.getId1(), str2_id.getId2(), "str2" );

		SetRefCollEntityMulId coll1 = new SetRefCollEntityMulId( coll1_id.getId1(), coll1_id.getId2(), "coll1" );

		// Revision 1
		em.getTransaction().begin();

		em.persist( str1 );
		em.persist( str2 );

		coll1.setCollection( new HashSet<MulIdTestEntity>() );
		coll1.getCollection().add( str1 );
		em.persist( coll1 );

		em.getTransaction().commit();

		// Revision 2
		em.getTransaction().begin();

		str2 = em.find( MulIdTestEntity.class, str2_id );
		coll1 = em.find( SetRefCollEntityMulId.class, coll1_id );

		coll1.getCollection().add( str2 );

		em.getTransaction().commit();

		// Revision 3
		em.getTransaction().begin();

		str1 = em.find( MulIdTestEntity.class, str1_id );
		coll1 = em.find( SetRefCollEntityMulId.class, coll1_id );

		coll1.getCollection().remove( str1 );

		em.getTransaction().commit();

		// Revision 4
		em.getTransaction().begin();

		coll1 = em.find( SetRefCollEntityMulId.class, coll1_id );

		coll1.getCollection().clear();

		em.getTransaction().commit();
	}

	@Test
	public void testRevisionsCounts() {
		assert Arrays.asList( 1, 2, 3, 4 ).equals(
				getAuditReader().getRevisions(
						SetRefCollEntityMulId.class,
						coll1_id
				)
		);

		assert Arrays.asList( 1 ).equals( getAuditReader().getRevisions( MulIdTestEntity.class, str1_id ) );
		assert Arrays.asList( 1 ).equals( getAuditReader().getRevisions( MulIdTestEntity.class, str2_id ) );
	}

	@Test
	public void testHistoryOfColl1() {
		MulIdTestEntity str1 = getEntityManager().find( MulIdTestEntity.class, str1_id );
		MulIdTestEntity str2 = getEntityManager().find( MulIdTestEntity.class, str2_id );

		SetRefCollEntityMulId rev1 = getAuditReader().find( SetRefCollEntityMulId.class, coll1_id, 1 );
		SetRefCollEntityMulId rev2 = getAuditReader().find( SetRefCollEntityMulId.class, coll1_id, 2 );
		SetRefCollEntityMulId rev3 = getAuditReader().find( SetRefCollEntityMulId.class, coll1_id, 3 );
		SetRefCollEntityMulId rev4 = getAuditReader().find( SetRefCollEntityMulId.class, coll1_id, 4 );

		assert rev1.getCollection().equals( TestTools.makeSet( str1 ) );
		assert rev2.getCollection().equals( TestTools.makeSet( str1, str2 ) );
		assert rev3.getCollection().equals( TestTools.makeSet( str2 ) );
		assert rev4.getCollection().equals( TestTools.makeSet() );

		assert "coll1".equals( rev1.getData() );
		assert "coll1".equals( rev2.getData() );
		assert "coll1".equals( rev3.getData() );
		assert "coll1".equals( rev4.getData() );
	}
}
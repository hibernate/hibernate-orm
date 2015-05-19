/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.manytomany.unidirectional;

import java.util.Arrays;
import java.util.List;
import javax.persistence.EntityManager;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.envers.test.entities.UnversionedStrTestEntity;
import org.hibernate.envers.test.entities.manytomany.unidirectional.M2MIndexedListTargetNotAuditedEntity;

import org.junit.Test;

import static org.hibernate.envers.test.tools.TestTools.checkCollection;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * A test for auditing a many-to-many indexed list where the target entity is not audited.
 *
 * @author Vladimir Klyushnikov
 * @author Adam Warski
 */
public class M2MIndexedListNotAuditedTarget extends BaseEnversJPAFunctionalTestCase {
	private Integer itnae1_id;
	private Integer itnae2_id;

	private UnversionedStrTestEntity uste1;
	private UnversionedStrTestEntity uste2;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {UnversionedStrTestEntity.class, M2MIndexedListTargetNotAuditedEntity.class};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager em = getEntityManager();

		uste1 = new UnversionedStrTestEntity( "str1" );
		uste2 = new UnversionedStrTestEntity( "str2" );

		// No revision
		em.getTransaction().begin();

		em.persist( uste1 );
		em.persist( uste2 );

		em.getTransaction().commit();

		// Revision 1
		em.getTransaction().begin();

		uste1 = em.find( UnversionedStrTestEntity.class, uste1.getId() );
		uste2 = em.find( UnversionedStrTestEntity.class, uste2.getId() );

		M2MIndexedListTargetNotAuditedEntity itnae1 = new M2MIndexedListTargetNotAuditedEntity( 1, "tnae1" );

		itnae1.getReferences().add( uste1 );
		itnae1.getReferences().add( uste2 );

		em.persist( itnae1 );

		em.getTransaction().commit();

		// Revision 2
		em.getTransaction().begin();

		M2MIndexedListTargetNotAuditedEntity itnae2 = new M2MIndexedListTargetNotAuditedEntity( 2, "tnae2" );

		itnae2.getReferences().add( uste2 );

		em.persist( itnae2 );

		em.getTransaction().commit();

		// Revision 3
		em.getTransaction().begin();

		itnae1.getReferences().set( 0, uste2 );
		itnae1.getReferences().set( 1, uste1 );
		em.getTransaction().commit();

		itnae1_id = itnae1.getId();
		itnae2_id = itnae2.getId();
	}

	@Test
	public void testRevisionsCounts() {
		List<Number> revisions = getAuditReader().getRevisions( M2MIndexedListTargetNotAuditedEntity.class, itnae1_id );
		assertEquals( revisions, Arrays.asList( 1, 3 ) );

		revisions = getAuditReader().getRevisions( M2MIndexedListTargetNotAuditedEntity.class, itnae2_id );
		assertEquals( revisions, Arrays.asList( 2 ) );
	}

	@Test
	public void testHistory1() throws Exception {
		M2MIndexedListTargetNotAuditedEntity rev1 = getAuditReader().find(
				M2MIndexedListTargetNotAuditedEntity.class,
				itnae1_id,
				1
		);
		M2MIndexedListTargetNotAuditedEntity rev2 = getAuditReader().find(
				M2MIndexedListTargetNotAuditedEntity.class,
				itnae1_id,
				2
		);
		M2MIndexedListTargetNotAuditedEntity rev3 = getAuditReader().find(
				M2MIndexedListTargetNotAuditedEntity.class,
				itnae1_id,
				3
		);

		assertTrue( checkCollection( rev1.getReferences(), uste1, uste2 ) );
		assertTrue( checkCollection( rev2.getReferences(), uste1, uste2 ) );
		assertTrue( checkCollection( rev3.getReferences(), uste2, uste1 ) );
	}

	@Test
	public void testHistory2() throws Exception {
		M2MIndexedListTargetNotAuditedEntity rev1 = getAuditReader().find(
				M2MIndexedListTargetNotAuditedEntity.class,
				itnae2_id,
				1
		);
		M2MIndexedListTargetNotAuditedEntity rev2 = getAuditReader().find(
				M2MIndexedListTargetNotAuditedEntity.class,
				itnae2_id,
				2
		);
		M2MIndexedListTargetNotAuditedEntity rev3 = getAuditReader().find(
				M2MIndexedListTargetNotAuditedEntity.class,
				itnae2_id,
				3
		);

		assertNull( rev1 );
		assertTrue( checkCollection( rev2.getReferences(), uste2 ) );
		assertTrue( checkCollection( rev3.getReferences(), uste2 ) );
	}
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.onetoone.bidirectional;

import javax.persistence.EntityManager;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-8305")
public class BidirectionalOneToOneOptionalTest extends BaseEnversJPAFunctionalTestCase {
	private Integer refingWithNoRefedId;
	private Integer refingId;
	private Integer refedId;

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				BiRefingOptionalEntity.class,
				BiRefedOptionalEntity.class
		};
	}

	@Test
	@Priority(10)
	public void initData() {
		EntityManager entityManager = getEntityManager();
		try {
			// Revision 1
			entityManager.getTransaction().begin();

			// store refing with null refed entity
			BiRefingOptionalEntity refingWithNoRefed = new BiRefingOptionalEntity();
			refingWithNoRefed.setReference( null );
			entityManager.persist( refingWithNoRefed );

			// store refing with non-null refed entity
			BiRefingOptionalEntity refing = new BiRefingOptionalEntity();
			BiRefedOptionalEntity refed = new BiRefedOptionalEntity();
			refed.setReferencing( refing );
			refing.setReference( refed );
			entityManager.persist( refing );
			entityManager.persist( refed );

			entityManager.getTransaction().commit();

			this.refingId = refing.getId();
			this.refedId = refed.getId();
			this.refingWithNoRefedId = refingWithNoRefed.getId();
		}
		finally {
			entityManager.close();
		}
	}

	@Test
	public void testRevisionCounts() {
		assertEquals( 1, getAuditReader().getRevisions( BiRefingOptionalEntity.class, refingId ).size() );
		assertEquals( 1, getAuditReader().getRevisions( BiRefingOptionalEntity.class, refingWithNoRefedId ).size() );
		assertEquals( 1, getAuditReader().getRevisions( BiRefedOptionalEntity.class, refedId ).size() );
	}

	@Test
	public void testRevisionHistoryNullReference() {
		BiRefingOptionalEntity rev1 = getAuditReader().find( BiRefingOptionalEntity.class, refingWithNoRefedId, 1 );
		assertNull( rev1.getReference() );
	}

	@Test
	public void testRevisionHistoryWithNonNullReference() {
		assertNotNull( getAuditReader().find( BiRefingOptionalEntity.class, refingId, 1).getReference() );
		assertNotNull( getAuditReader().find( BiRefedOptionalEntity.class, refedId, 1 ).getReferencing() );
	}
}

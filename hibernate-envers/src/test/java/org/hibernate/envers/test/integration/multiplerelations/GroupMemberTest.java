/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.envers.test.integration.multiplerelations;

import java.math.BigInteger;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.hibernate.envers.test.BaseEnversJPAFunctionalTestCase;
import org.hibernate.envers.test.Priority;
import org.hibernate.testing.TestForIssue;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * @author Chris Cranford
 */
@TestForIssue(jiraKey = "HHH-7681")
public class GroupMemberTest extends BaseEnversJPAFunctionalTestCase {

	private static final String QUERY_STRING =
			"SELECT uniqueGroup_id FROM groupmember_aud ORDER by rev DESC";

	private Long uniqueGroupId;
	private Long groupMemberId;

    @Override
    protected Class<?>[] getAnnotatedClasses() {
    	return new Class[] {
    		GroupMember.class,
    		MultiGroup.class,
    		UniqueGroup.class
    	};
    }

	@Test
	@Priority(10)
	public void initData() {
		EntityManager entityManager = getEntityManager();
		try {
			// Revision 1
			UniqueGroup uniqueGroup = new UniqueGroup();
			GroupMember groupMember = new GroupMember();
			uniqueGroup.addMember( groupMember );

			entityManager.getTransaction().begin();
			entityManager.persist( uniqueGroup );
			entityManager.persist( groupMember );
			entityManager.getTransaction().commit();
			this.uniqueGroupId = uniqueGroup.getId();
			this.groupMemberId = groupMember.getId();

			// Revision 2
			MultiGroup multiGroup = new MultiGroup();
			multiGroup.addMember( groupMember );
			groupMember.addMultiGroup( multiGroup );

			entityManager.getTransaction().begin();
			entityManager.persist( multiGroup );
			entityManager.getTransaction().commit();
		}
		finally {
			entityManager.close();
		}
	}

	@Test
	public void testUniqueGroupFound() {
		EntityManager entityManager = getEntityManager();
		try {
			entityManager.getTransaction().begin();
			GroupMember groupMember = entityManager.find( GroupMember.class, groupMemberId );
			assertNotNull( groupMember );
			assertNotNull( groupMember.getUniqueGroup() );
		}
		finally {
			entityManager.close();
		}
	}

	@Test
	public void testUniqueGroupFromAuditHistory() {
		assertEquals( uniqueGroupId, getCurrentAuditUniqueGroupId() );
	}

	private Long getCurrentAuditUniqueGroupId() {
		EntityManager entityManager = getEntityManager();
		try {
			Query query = entityManager.createNativeQuery( QUERY_STRING );
			List<?> queryResults = query.getResultList();
			assertFalse( queryResults.isEmpty() );
			Object value = queryResults.get( 0 );
			return ( value != null ? ( (BigInteger) value ).longValue() : null );
		}
		finally {
			entityManager.close();
		}
	}
}

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
import org.hibernate.testing.TestForIssue;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author Naros (crancran at gmail dot com)
 */
@TestForIssue(jiraKey = "HHH-7681")
public class GroupMemberTest extends BaseEnversJPAFunctionalTestCase {

    @Override
    protected Class<?>[] getAnnotatedClasses() {
    	return new Class[] {
    		GroupMember.class,
    		MultiGroup.class,
    		UniqueGroup.class
    	};
    }
    
    @Test
    public void uniqueGroupIdConsistent() {
    	
    	EntityManager em = getEntityManager();
    	
    	UniqueGroup uniqueGroup = new UniqueGroup();
    	GroupMember groupMember = new GroupMember();
    	uniqueGroup.addMember( groupMember );
    	
    	em.getTransaction().begin();
    	em.persist( uniqueGroup );
    	em.persist( groupMember );
    	em.getTransaction().commit();
    	
    	Assert.assertEquals( currentAuditUniqueGroupId(), BigInteger.valueOf( uniqueGroup.getId() ) );
    	
    	MultiGroup multiGroup = new MultiGroup();
    	multiGroup.addMember(groupMember);
    	groupMember.addMultiGroup(multiGroup);
    	
    	em.getTransaction().begin();
    	em.persist(multiGroup);
    	em.getTransaction().commit();
    	
    	Assert.assertEquals(currentAuditUniqueGroupId(), BigInteger.valueOf(uniqueGroup.getId()));
    	
    } 
    
    private BigInteger currentAuditUniqueGroupId() {
    	
    	final String auditQuery = "SELECT uniqueGroup_id FROM groupmember_aud ORDER by rev DESC";
    	
    	EntityManager em = getEntityManager();    	
    	Query query = em.createNativeQuery(auditQuery);
    	List<?> values = query.getResultList();    	
    	Assert.assertFalse(values.isEmpty());
    	    	
    	BigInteger currentUniqueGroupId = (BigInteger) values.get(0);    	
    	return currentUniqueGroupId;
    }

}

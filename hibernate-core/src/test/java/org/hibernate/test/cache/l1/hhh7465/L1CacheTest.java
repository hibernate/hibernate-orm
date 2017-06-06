package org.hibernate.test.cache.l1.hhh7465;

import java.util.Arrays;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Assert;
import org.junit.Test;

public class L1CacheTest extends BaseCoreFunctionalTestCase {
	
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[]{Group.class, User.class};
	}
	
	@Test
    public void testTest() throws Exception {
        User user = new User();
        user.setName("John Doe");
        List<Group> groups = Arrays.asList(new Group(), new Group(), new Group());
        user = saveUser(user, groups);
        User freshUser = getById(user.getId());
        
        // The freshUser that is queried via a new session is as expected
        Assert.assertEquals(groups.size(), freshUser.getGroups().size());
        // The user returned by saveUser is not fully initialized because
        // it is directly taken from L1 cache but the join fetch is not considered
        Assert.assertEquals(groups.size(), user.getGroups().size());
    }
	
	private User getById(long id) {
		Session session = null;
		
		try {
			session = openSession();
	        return getById(session, id);
		} finally {
			session.close();
		}
    }
	
	private User getById(Session session, long id) {
		// When the user object is already in the L1 cache, the join fetch is ignored
        return (User) session.createQuery("SELECT user From User user JOIN FETCH user.groups WHERE user.id = :id").setParameter("id", id).uniqueResult();
    }

    private User saveUser(User user,  List<Group> groups) {
		Session session = null;
		
		try {
			session = openSession();
			session.beginTransaction();
	        user = (User) session.merge(user);
	        for (Group group : groups) {
	            group.setUser(user);
	            session.merge(group);
	        }
	        session.flush();
			session.getTransaction().commit();
			// we use the same session so we have user in L1 cache
	        return getById(session, user.getId());
		} finally {
			session.close();
		}
    }
}

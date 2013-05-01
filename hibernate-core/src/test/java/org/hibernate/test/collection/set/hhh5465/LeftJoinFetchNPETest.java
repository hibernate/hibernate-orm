/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.test.collection.set.hhh5465;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.hibernate.Session;
import org.junit.Assert;
import org.junit.Test;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;

/**
 * @author Elizabeth Chatman
 * @author Steve Ebersole
 */
public class LeftJoinFetchNPETest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {Contact.class, EmailAddress.class, User.class};
	}

	@Test
    public void testTest() throws Exception {
        User user = new User();
        user.setName("john");
        Contact contact = new Contact();
        contact.setName("John Doe");
        Set<EmailAddress> emailAddresses = new HashSet<EmailAddress>();
        emailAddresses.add(new EmailAddress("test1@test.com"));
        emailAddresses.add(new EmailAddress("test2@test.com"));
        emailAddresses.add(new EmailAddress("test3@test.com"));
        contact.setEmailAddresses(emailAddresses);
        user.setContact(contact);
        user = saveUser(user);
        User freshUser = getById(user.getId());
        Assert.assertEquals(emailAddresses, freshUser.getContact().getEmailAddresses());
    }
	
	public User getById(long id) {
		Session session = null;
		
		try {
			session = openSession();
	        return (User) session.createQuery("SELECT user "
	        		+ "FROM User user "
	        		+ "LEFT OUTER JOIN FETCH user.contact contact "
	        		+ "LEFT OUTER JOIN FETCH contact.emailAddresses2 "
	        		+ "LEFT OUTER JOIN FETCH contact.emailAddresses "
	        		+ "WHERE user.id = :id")
	        		.setParameter("id", id)
	        		.uniqueResult();
		} finally {
			session.close();
		}
    }

    public User saveUser(User user) {
		Session session = null;
		
		try {
			session = openSession();
			session.beginTransaction();
			user.setContact((Contact) session.merge(user.getContact()));
			user = (User) session.merge(user);
			session.getTransaction().commit();
			return user;
		} finally {
			session.close();
		}
    }
}

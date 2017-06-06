package org.hibernate.test.collection.set.hhh8206;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.Session;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Assert;
import org.junit.Test;

public class JoinFetchElementCollectionTest extends BaseCoreFunctionalTestCase {
	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] {Contact.class, EmailAddress.class, User.class};
	}
	
	@Test
	public void test(){
        Set<EmailAddress> emailAddresses = new HashSet<EmailAddress>();
        emailAddresses.add(new EmailAddress("test1@test.com"));
        emailAddresses.add(new EmailAddress("test2@test.com"));
        emailAddresses.add(new EmailAddress("test3@test.com"));
        
		{
			// Session 1: Insert a user with email addresses but no emailAddresses2
			Session session = openSession();
			session.beginTransaction();
			
			User user = new User();
	        user.setName("john");
	        Contact contact = new Contact();
	        contact.setName("John Doe");
	        contact.setEmailAddresses(emailAddresses);
	        contact = (Contact) session.merge(contact);
	        user.setContact(contact);
	        user = (User) session.merge(user);

			session.getTransaction().commit();
			session.close();
		}
		{
			// Session 2: Retrieve the user object and check if the sets have the expected values
			Session session = openSession();
			session.beginTransaction();
			User user = (User) session.createQuery("SELECT user "
					+ "FROM User user "
					+ "LEFT OUTER JOIN FETCH user.contact "
					+ "LEFT OUTER JOIN FETCH user.contact.emailAddresses2 "
					+ "LEFT OUTER JOIN FETCH user.contact.emailAddresses")
					.uniqueResult();
			session.getTransaction().commit();
			session.close();

			Assert.assertEquals(emailAddresses, user.getContact().getEmailAddresses());
			Assert.assertTrue(user.getContact().getEmailAddresses2().isEmpty());
		}
		
	}

}

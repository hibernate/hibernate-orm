/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.extralazy;

import org.hibernate.Session;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author Vlad Mihalcea
 */
public class ExtraLazyCollectionConsistencyTest extends BaseCoreFunctionalTestCase {

	private User user;

	@Override
	public String[] getMappings() {
		return new String[] { "extralazy/UserGroup.hbm.xml","extralazy/Parent.hbm.xml","extralazy/Child.hbm.xml" };
	}

	@Override
	protected void prepareTest()  {
		Session session = openSession();
		session.beginTransaction();
		{
			user = new User("victor", "hugo");
			session.persist(user);
		}
		session.getTransaction().commit();
		session.close();
	}

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9933")
	public void testSetSize() {
		Session session = openSession();
		session.beginTransaction();
		{
			User _user = session.get(User.class, user.getName());
			Document document = new Document("Les Miserables", "sad", _user);
			assertEquals(1, _user.getDocuments().size());
		}
		session.getTransaction().commit();
		session.close();
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9933")
	public void testSetIterator() {
		Session session = openSession();
		session.beginTransaction();
		{
			User _user = session.get(User.class, user.getName());
			Document document = new Document("Les Miserables", "sad", _user);
			assertTrue(_user.getDocuments().iterator().hasNext());
		}
		session.getTransaction().commit();
		session.close();
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9933")
	public void testSetIsEmpty() {
		Session session = openSession();
		session.beginTransaction();
		{
			User _user = session.get(User.class, user.getName());
			Document document = new Document("Les Miserables", "sad", _user);
			assertFalse(_user.getDocuments().isEmpty());
		}
		session.getTransaction().commit();
		session.close();
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9933")
	public void testSetContains() {
		Session session = openSession();
		session.beginTransaction();
		{
			User _user = session.get(User.class, user.getName());
			Document document = new Document("Les Miserables", "sad", _user);
			assertTrue(_user.getDocuments().contains(document));
		}
		session.getTransaction().commit();
		session.close();
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9933")
	public void testSetAdd() {
		Session session = openSession();
		session.beginTransaction();
		{
			User _user = session.get(User.class, user.getName());
			Document document = new Document();
			document.setTitle("Les Miserables");
			document.setContent("sad");
			document.setOwner(_user);
			assertTrue("not added", _user.getDocuments().add(document));
			assertFalse("added", _user.getDocuments().add(document));
		}
		session.getTransaction().commit();
		session.close();
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9933")
	public void testSetRemove() {
		Session session = openSession();
		session.beginTransaction();
		{
			User _user = session.get(User.class, user.getName());

			Document document = new Document("Les Miserables", "sad", _user);
			assertTrue("not removed", _user.getDocuments().remove(document));
		}
		session.getTransaction().commit();
		session.close();
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9933")
	public void testSetToArray() {
		Session session = openSession();
		session.beginTransaction();
		{
			User _user = session.get(User.class, user.getName());

			Document document = new Document("Les Miserables", "sad", _user);
			assertEquals(1, _user.getDocuments().toArray().length);
		}
		session.getTransaction().commit();
		session.close();
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9933")
	public void testSetToArrayTyped() {
		Session session = openSession();
		session.beginTransaction();
		{
			User _user = session.get(User.class, user.getName());

			Document document = new Document("Les Miserables", "sad", _user);
			assertEquals(1, _user.getDocuments().toArray(new Document[0]).length);
		}
		session.getTransaction().commit();
		session.close();
	}
}


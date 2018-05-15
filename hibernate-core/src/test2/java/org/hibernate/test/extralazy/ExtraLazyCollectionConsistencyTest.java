/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.extralazy;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
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
		doInHibernate( this::sessionFactory, session -> {
			user = new User("victor", "hugo");
			session.persist(user);
		});
	}

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9933")
	public void testSetSize() {
		doInHibernate( this::sessionFactory, session -> {
			User _user = session.get(User.class, user.getName());
			Document document = new Document("Les Miserables", "sad", _user);
			assertEquals(1, _user.getDocuments().size());
		});
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9933")
	public void testSetIterator() {
		doInHibernate( this::sessionFactory, session -> {
			User _user = session.get(User.class, user.getName());
			Document document = new Document("Les Miserables", "sad", _user);
			assertTrue(_user.getDocuments().iterator().hasNext());
		});
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9933")
	public void testSetIsEmpty() {
		doInHibernate( this::sessionFactory, session -> {
			User _user = session.get(User.class, user.getName());
			Document document = new Document("Les Miserables", "sad", _user);
			assertFalse(_user.getDocuments().isEmpty());
		});
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9933")
	public void testSetContains() {
		doInHibernate( this::sessionFactory, session -> {
			User _user = session.get(User.class, user.getName());
			Document document = new Document("Les Miserables", "sad", _user);
			assertTrue(_user.getDocuments().contains(document));
		});
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9933")
	public void testSetAdd() {
		doInHibernate( this::sessionFactory, session -> {
			User _user = session.get(User.class, user.getName());
			Document document = new Document();
			document.setTitle("Les Miserables");
			document.setContent("sad");
			document.setOwner(_user);
			assertTrue("not added", _user.getDocuments().add(document));
			assertFalse("added", _user.getDocuments().add(document));
		});
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9933")
	public void testSetRemove() {
		doInHibernate( this::sessionFactory, session -> {
			User _user = session.get(User.class, user.getName());

			Document document = new Document("Les Miserables", "sad", _user);
			assertTrue("not removed", _user.getDocuments().remove(document));
		});
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9933")
	public void testSetToArray() {
		doInHibernate( this::sessionFactory, session -> {
			User _user = session.get(User.class, user.getName());

			Document document = new Document("Les Miserables", "sad", _user);
			assertEquals(1, _user.getDocuments().toArray().length);
		});
	}

	@Test
	@TestForIssue(jiraKey = "HHH-9933")
	public void testSetToArrayTyped() {
		doInHibernate( this::sessionFactory, session -> {
			User _user = session.get(User.class, user.getName());

			Document document = new Document("Les Miserables", "sad", _user);
			assertEquals(1, _user.getDocuments().toArray(new Document[0]).length);
		});
	}
}


/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.lazyload;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * @author Oleksander Dukhno
 */
public class LazyLoadingTest
		extends BaseCoreFunctionalTestCase {

	private static final int CHILDREN_SIZE = 3;
	private Long parentID;
	private Long lastChildID;

	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.ENABLE_LAZY_LOAD_NO_TRANS, "true" );
		cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "false" );
	}


	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Parent.class,
				Child.class
		};
	}

	protected void prepareTest()
			throws Exception {
		Session s = openSession();
		s.beginTransaction();

		Parent p = new Parent();
		for ( int i = 0; i < CHILDREN_SIZE; i++ ) {
			final Child child = p.makeChild();
			s.persist( child );
			lastChildID = child.getId();
		}
		s.persist( p );
		parentID = p.getId();

		s.getTransaction().commit();
		s.clear();
		s.close();
	}

	@Test
	@TestForIssue(jiraKey = "HHH-7971")
	public void testLazyCollectionLoadingAfterEndTransaction() {
		Session s = openSession();
		s.beginTransaction();
		Parent loadedParent = (Parent) s.load( Parent.class, parentID );
		s.getTransaction().commit();
		s.close();

		assertFalse( Hibernate.isInitialized( loadedParent.getChildren() ) );

		int i = 0;
		for ( Child child : loadedParent.getChildren() ) {
			i++;
			assertNotNull( child );
		}

		assertEquals( CHILDREN_SIZE, i );

		s = openSession();
		s.beginTransaction();
		Child loadedChild = (Child) s.load( Child.class, lastChildID );
		s.getTransaction().commit();
		s.close();

		Parent p = loadedChild.getParent();
		int j = 0;
		for ( Child child : p.getChildren() ) {
			j++;
			assertNotNull( child );
		}

		assertEquals( CHILDREN_SIZE, j );
	}

}

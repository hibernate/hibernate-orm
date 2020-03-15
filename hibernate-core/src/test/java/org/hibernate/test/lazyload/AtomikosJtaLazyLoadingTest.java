/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.lazyload;

import org.hibernate.Hibernate;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.jta.TestingJtaBootstrap;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * @author Vlad Mihalcea
 */
public class AtomikosJtaLazyLoadingTest
		extends BaseCoreFunctionalTestCase {

	private static final int CHILDREN_SIZE = 3;
	private Long parentID;
	private Long lastChildID;

	protected void configure(Configuration cfg) {
		super.configure( cfg );
		cfg.setProperty( Environment.ENABLE_LAZY_LOAD_NO_TRANS, "true" );

		TestingJtaBootstrap.prepare( cfg.getProperties() );
		cfg.setProperty( AvailableSettings.JTA_PLATFORM, "Atomikos" );
	}

	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Parent.class,
				Child.class
		};
	}

	protected void prepareTest()
			throws Exception {
		doInHibernate( this::sessionFactory, session -> {
			Parent p = new Parent();
			for ( int i = 0; i < CHILDREN_SIZE; i++ ) {
				final Child child = p.makeChild();
				session.persist( child );
				lastChildID = child.getId();
			}
			session.persist( p );
			parentID = p.getId();
		} );
	}

	@Test
	@TestForIssue(jiraKey = "HHH-7971")
	public void testLazyCollectionLoadingAfterEndTransaction() {
		Parent loadedParent = doInHibernate( this::sessionFactory, session -> {
			return session.load( Parent.class, parentID );
		} );

		assertFalse( Hibernate.isInitialized( loadedParent.getChildren() ) );

		int i = 0;
		for ( Child child : loadedParent.getChildren() ) {
			i++;
			assertNotNull( child );
		}

		assertEquals( CHILDREN_SIZE, i );

		Child loadedChild = doInHibernate( this::sessionFactory, session -> {
			return session.load( Child.class, lastChildID );
		} );

		Parent p = loadedChild.getParent();
		int j = 0;
		for ( Child child : p.getChildren() ) {
			j++;
			assertNotNull( child );
		}

		assertEquals( CHILDREN_SIZE, j );
	}

}

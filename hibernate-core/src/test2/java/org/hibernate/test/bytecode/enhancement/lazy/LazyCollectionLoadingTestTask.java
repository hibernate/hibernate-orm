/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.lazy;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.proxy.HibernateProxy;

import org.hibernate.test.bytecode.enhancement.AbstractEnhancerTestTask;
import org.hibernate.testing.bytecode.enhancement.EnhancerTestUtils;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Simple test for lazy collection handling in the new bytecode support.
 * Prior to HHH-10055 lazy collections were simply not handled.  The tests
 * initially added for HHH-10055 cover the more complicated case of handling
 * lazy collection initialization outside of a transaction; that is a bigger
 * fix, and I first want to get collection handling to work here in general.
 *
 * @author Steve Ebersole
 */
public class LazyCollectionLoadingTestTask extends AbstractEnhancerTestTask {
	private static final int CHILDREN_SIZE = 10;
	private Long parentID;
	private Long lastChildID;

	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {Parent.class, Child.class};
	}

	public void prepare() {
		Configuration cfg = new Configuration();
		cfg.setProperty( Environment.ENABLE_LAZY_LOAD_NO_TRANS, "false" );
		cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "false" );
		super.prepare( cfg );

		Session s = getFactory().openSession();
		s.beginTransaction();

		Parent parent = new Parent();
		parent.setChildren( new ArrayList<Child>() );
		for ( int i = 0; i < CHILDREN_SIZE; i++ ) {
			final Child child = new Child();
			child.setParent( parent );
			s.persist( child );
			lastChildID = child.getId();
		}
		s.persist( parent );
		parentID = parent.getId();

		s.getTransaction().commit();
		s.close();
	}

	public void execute() {
		Session s = getFactory().openSession();
		s.beginTransaction();

		Parent parent = s.load( Parent.class, parentID );
		assertThat( parent, notNullValue() );
		assertThat( parent, not( instanceOf( HibernateProxy.class ) ) );
		assertThat( parent, not( instanceOf( HibernateProxy.class ) ) );
		assertFalse( Hibernate.isPropertyInitialized( parent, "children" ) );
		EnhancerTestUtils.checkDirtyTracking( parent );

		List children1 = parent.getChildren();
		List children2 = parent.getChildren();

		assertTrue( Hibernate.isPropertyInitialized( parent, "children" ) );
		EnhancerTestUtils.checkDirtyTracking( parent );

		assertThat( children1, sameInstance( children2 ) );
		assertThat( children1.size(), equalTo( CHILDREN_SIZE ) );

		s.getTransaction().commit();
		s.close();
	}

	protected void cleanup() {
	}

}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.lazy;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;

import org.hibernate.test.bytecode.enhancement.AbstractEnhancerTestTask;
import org.hibernate.testing.bytecode.enhancement.EnhancerTestUtils;
import org.junit.Assert;

/**
 * @author Luis Barreiro
 */
public class LazyLoadingIntegrationTestTask extends AbstractEnhancerTestTask {

	private static final int CHILDREN_SIZE = 10;
	private Long parentID;
	private Long lastChildID;

	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {Parent.class, Child.class};
	}

	public void prepare() {
		Configuration cfg = new Configuration();
		cfg.setProperty( Environment.ENABLE_LAZY_LOAD_NO_TRANS, "true" );
		cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "false" );
		super.prepare( cfg );

		Session s = getFactory().openSession();
		s.beginTransaction();

		Parent parent = new Parent();
		parent.setChildren( new ArrayList<Child>( CHILDREN_SIZE ) );
		for ( int i = 0; i < CHILDREN_SIZE; i++ ) {
			final Child child = new Child();
			// Association management should kick in here
			child.setParent( parent );
			s.persist( child );
			lastChildID = child.getId();
		}
		s.persist( parent );
		parentID = parent.getId();

		s.getTransaction().commit();
		s.clear();
		s.close();
	}

	public void execute() {
		Session s = getFactory().openSession();
		s.beginTransaction();

		Child loadedChild = s.load( Child.class, lastChildID );
		EnhancerTestUtils.checkDirtyTracking( loadedChild );

		loadedChild.setName( "Barrabas" );
		EnhancerTestUtils.checkDirtyTracking( loadedChild, "name" );

		Parent loadedParent = loadedChild.getParent();
		EnhancerTestUtils.checkDirtyTracking( loadedChild, "name" );
		EnhancerTestUtils.checkDirtyTracking( loadedParent );

		List<Child> loadedChildren = new ArrayList<Child>( loadedParent.getChildren() );
		loadedChildren.remove( 0 );
		loadedChildren.remove( loadedChild );
		loadedParent.setChildren( loadedChildren );

		EnhancerTestUtils.checkDirtyTracking( loadedParent, "children" );
		Assert.assertNull( loadedChild.parent );

		s.getTransaction().commit();
		s.close();
	}

	protected void cleanup() {
	}

}

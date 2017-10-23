/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.lazy;

import java.util.ArrayList;
import java.util.Collection;

import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.proxy.HibernateProxy;

import org.hibernate.test.bytecode.enhancement.AbstractEnhancerTestTask;
import org.hibernate.testing.bytecode.enhancement.EnhancerTestUtils;
import org.junit.Assert;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * @author Luis Barreiro
 */
public class LazyLoadingTestTask extends AbstractEnhancerTestTask {

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
		parent.setChildren(new ArrayList<Child>());
		for ( int i = 0; i < CHILDREN_SIZE; i++ ) {
			final Child child = new Child( "Child #" + i );
			child.setParent( parent );
			parent.getChildren().add( child );
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

		Object nameByReflection = EnhancerTestUtils.getFieldByReflection( loadedChild, "name" );
		Assert.assertNotNull( "Non-lazy field 'name' was not loaded", nameByReflection );

		Object parentByReflection = EnhancerTestUtils.getFieldByReflection( loadedChild, "parent" );
		Assert.assertNull( "Lazy field 'parent' is initialized", parentByReflection );
		Assert.assertFalse( loadedChild instanceof HibernateProxy );

		Parent loadedParent = loadedChild.getParent();
		assertThat( loadedChild.getName(), notNullValue() );
		assertThat( loadedParent, notNullValue() );
		assertThat( loadedChild.getParent(), notNullValue() );

		EnhancerTestUtils.checkDirtyTracking( loadedChild );

		parentByReflection = EnhancerTestUtils.getFieldByReflection( loadedChild, "parent" );
		Object childrenByReflection = EnhancerTestUtils.getFieldByReflection( loadedParent, "children" );
		Assert.assertNotNull( "Lazy field 'parent' is not loaded", parentByReflection );
		Assert.assertNull( "Lazy field 'children' is initialized", childrenByReflection );
		Assert.assertFalse( loadedParent instanceof HibernateProxy );
		Assert.assertTrue( parentID.equals( loadedParent.id ) );

		Collection<Child> loadedChildren = loadedParent.getChildren();

		EnhancerTestUtils.checkDirtyTracking( loadedChild );
		EnhancerTestUtils.checkDirtyTracking( loadedParent );

		childrenByReflection = EnhancerTestUtils.getFieldByReflection( loadedParent, "children" );
		Assert.assertNotNull( "Lazy field 'children' is not loaded", childrenByReflection );
		Assert.assertFalse( loadedChildren instanceof HibernateProxy );
		Assert.assertEquals( CHILDREN_SIZE, loadedChildren.size() );
		Assert.assertTrue( loadedChildren.contains( loadedChild ) );

		s.getTransaction().commit();
		s.close();
	}

	protected void cleanup() {
	}

}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.lazy.group;

import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.proxy.HibernateProxy;

import org.hibernate.testing.bytecode.enhancement.EnhancerTestUtils;
import org.hibernate.test.bytecode.enhancement.AbstractEnhancerTestTask;
import org.junit.Assert;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * @author Steve Ebersole
 */
public class LazyGroupAccessTestTask extends AbstractEnhancerTestTask {

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class[] { Child.class, Parent.class };
	}

	@Override
	public void prepare() {
		Configuration cfg = new Configuration();
		cfg.setProperty( Environment.ENABLE_LAZY_LOAD_NO_TRANS, "true" );
		cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "false" );
		super.prepare( cfg );

		Session s = getFactory().openSession();
		s.beginTransaction();

		Child c1 = new Child( "steve", "hibernater" );
		Child c2 = new Child( "sally", "Joe Mama" );

		Parent p1 = new Parent( "Hibernate" );
		Parent p2 = new Parent( "Swimming" );

		c1.setParent( p1 );
		p1.getChildren().add( c1 );

		c1.setAlternateParent( p2 );
		p2.getAlternateChildren().add( c1 );

		c2.setAlternateParent( p1 );
		p1.getAlternateChildren().add( c2 );

		c2.setParent( p2 );
		p2.getChildren().add( c2 );

		s.save( p1 );
		s.save( p2 );

		s.getTransaction().commit();
		s.close();
	}

	@Override
	public void execute() {
		Session s = getFactory().openSession();
		s.beginTransaction();

		Child c1 = (Child) s.createQuery( "from Child c where c.name = :name" ).setString( "name", "steve" ).uniqueResult();

		// verify the expected initial loaded state
		assertLoaded( c1, "name" );
		assertNotLoaded( c1, "nickName" );
		assertNotLoaded( c1, "parent" );
		assertNotLoaded( c1, "alternateParent" );

		// Now lets access nickName which ought to initialize nickName and parent, but not alternateParent
		c1.getNickName();
		assertLoaded( c1, "nickName" );
		assertLoaded( c1, "parent" );
		assertNotLoaded( c1, "alternateParent" );
		assertEquals( "Hibernate", c1.getParent().getNombre() );
		assertFalse( c1.getParent() instanceof HibernateProxy );

		Child c2 = (Child) s.createQuery( "from Child c where c.name = :name" ).setString( "name", "sally" ).uniqueResult();

		// verify the expected initial loaded state
		assertLoaded( c2, "name" );
		assertNotLoaded( c2, "nickName" );
		assertNotLoaded( c2, "parent" );
		assertNotLoaded( c2, "alternateParent" );

		// Now lets access alternateParent which ought to initialize alternateParent and nothing else
		c2.getAlternateParent();
		assertNotLoaded( c2, "nickName" );
		assertNotLoaded( c2, "parent" );
		assertLoaded( c2, "alternateParent" );
		assertEquals( "Hibernate", c2.getAlternateParent().getNombre() );
		assertFalse( c2.getAlternateParent() instanceof HibernateProxy );

		s.getTransaction().commit();
		s.close();
	}

	private void assertLoaded(Object owner, String name) {
		// NOTE we assume null == not-loaded
		Object fieldByReflection = EnhancerTestUtils.getFieldByReflection( owner, name );
		assertNotNull( "Expecting field '" + name + "' to be loaded, but it was not", fieldByReflection );
	}

	private void assertNotLoaded(Object owner, String name) {
		// NOTE we assume null == not-loaded
		Object fieldByReflection = EnhancerTestUtils.getFieldByReflection( owner, name );
		assertNull( "Expecting field '" + name + "' to be not loaded, but it was", fieldByReflection );
	}

	@Override
	protected void cleanup() {
		Session s = getFactory().openSession();
		s.beginTransaction();

		s.createQuery( "delete Child" ).executeUpdate();
		s.createQuery( "delete Parent" ).executeUpdate();

		s.getTransaction().commit();
		s.close();
	}
}

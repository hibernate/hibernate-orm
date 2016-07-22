/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.otherentityentrycontext;

import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.ManagedEntity;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.test.bytecode.enhancement.AbstractEnhancerTestTask;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * This task tests ManagedEntity objects that are already associated with a different PersistenceContext.
 *
 * @author Gail Badner
 */
public class OtherEntityEntryContextTestTask extends AbstractEnhancerTestTask {

	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {Parent.class};
	}

	public void prepare() {
		Configuration cfg = new Configuration();
		cfg.setProperty( Environment.ENABLE_LAZY_LOAD_NO_TRANS, "true" );
		cfg.setProperty( Environment.USE_SECOND_LEVEL_CACHE, "false" );
		super.prepare( cfg );

		// Create a Parent
		Session s = getFactory().openSession();
		s.beginTransaction();
		s.persist( new Parent( 1, "first" ) );
		s.getTransaction().commit();
		s.close();
	}

	public void execute() {
		Session s1 = getFactory().openSession();
		s1.beginTransaction();
		Parent p = (Parent) s1.get( Parent.class, 1 );
		assertTrue( ManagedEntity.class.isInstance( p ) );
		p.setName( "second" );

		assertTrue( s1.contains( p ) );

		// open another session and evict p from the new session
		Session s2 = getFactory().openSession();
		s2.beginTransaction();

		// s2 should contains no entities
		assertFalse( s2.contains( p ) );

		// evict should do nothing, since p is not associated with s2
		s2.evict( p );

		assertFalse( s2.contains( p ) );

		assertNull( ( (SharedSessionContractImplementor) s2 ).getPersistenceContext().getEntry( p ) );

		try {
			s2.update( p );
			fail( "should have failed because p is already associated with a PersistenceContext that is still open." );
		}
		catch (HibernateException expected) {
			// expected
			s2.getTransaction().rollback();
			s2.close();
		}

		s1.getTransaction().commit();
		s1.close();

		p.setName( "third" );

		s1 = getFactory().openSession();
		s1.getTransaction().begin();
		s1.update( p );
		assertTrue( s1.contains( p ) );
		s1.evict( p );
		assertFalse( s1.contains( p ) );
		p = s1.get( Parent.class, p.getId() );
		assertEquals( "second", p.getName() );
		s1.getTransaction().commit();
		s1.close();
	}

	protected void cleanup() {
	}
}

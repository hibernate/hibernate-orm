/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.eviction;

import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.engine.spi.ManagedEntity;
import org.hibernate.test.bytecode.enhancement.AbstractEnhancerTestTask;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

/**
 * @author Gail Badner
 */
public class EvictionTestTask extends AbstractEnhancerTestTask {


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

		Parent p = new Parent();
		p.setName( "PARENT" );
		s.persist( p );

		s.getTransaction().commit();
		s.close();
	}

	public void execute() {
		// Delete the Parent
		Session s = getFactory().openSession();
		s.beginTransaction();
		Parent loadedParent = (Parent) s.createQuery( "SELECT p FROM Parent p WHERE name=:name" )
				.setParameter( "name", "PARENT" )
				.uniqueResult();
		assertTyping( ManagedEntity.class, loadedParent );
		ManagedEntity managedParent = (ManagedEntity) loadedParent;
		// before eviction
		assertNotNull( managedParent.$$_hibernate_getEntityInstance() );
		assertNotNull( managedParent.$$_hibernate_getEntityEntry() );
		assertNull( managedParent.$$_hibernate_getPreviousManagedEntity() );
		assertNull( managedParent.$$_hibernate_getNextManagedEntity() );

		assertTrue( s.contains( managedParent ) );
		s.evict( managedParent );

		// after eviction
		assertFalse( s.contains( managedParent ) );
		assertNotNull( managedParent.$$_hibernate_getEntityInstance() );
		assertNull( managedParent.$$_hibernate_getEntityEntry() );
		assertNull( managedParent.$$_hibernate_getPreviousManagedEntity() );
		assertNull( managedParent.$$_hibernate_getNextManagedEntity() );

		// evict again
		s.evict( managedParent );

		assertFalse( s.contains( managedParent ) );
		assertNotNull( managedParent.$$_hibernate_getEntityInstance() );
		assertNull( managedParent.$$_hibernate_getEntityEntry() );
		assertNull( managedParent.$$_hibernate_getPreviousManagedEntity() );
		assertNull( managedParent.$$_hibernate_getNextManagedEntity() );

		s.delete( managedParent );

		s.getTransaction().commit();
		s.close();
	}

	protected void cleanup() {
	}


}

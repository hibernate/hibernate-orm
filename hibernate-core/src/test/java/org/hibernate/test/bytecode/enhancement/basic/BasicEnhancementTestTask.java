/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.bytecode.enhancement.basic;

import org.hibernate.engine.spi.ManagedEntity;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;

import org.hibernate.test.bytecode.enhancement.AbstractEnhancerTestTask;
import org.hibernate.test.bytecode.enhancement.EnhancerTestUtils;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

/**
 * @author Luis Barreiro
 */
public class BasicEnhancementTestTask extends AbstractEnhancerTestTask {

	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {SimpleEntity.class};
	}

	public void prepare() {
	}

	public void execute() {
		SimpleEntity entity = new SimpleEntity();

		// Call the new ManagedEntity methods
		assertTyping( ManagedEntity.class, entity );
		ManagedEntity managedEntity = (ManagedEntity) entity;
		assertSame( entity, managedEntity.$$_hibernate_getEntityInstance() );

		assertNull( managedEntity.$$_hibernate_getEntityEntry() );
		managedEntity.$$_hibernate_setEntityEntry( EnhancerTestUtils.makeEntityEntry() );
		assertNotNull( managedEntity.$$_hibernate_getEntityEntry() );
		managedEntity.$$_hibernate_setEntityEntry( null );
		assertNull( managedEntity.$$_hibernate_getEntityEntry() );

		managedEntity.$$_hibernate_setNextManagedEntity( managedEntity );
		managedEntity.$$_hibernate_setPreviousManagedEntity( managedEntity );
		assertSame( managedEntity, managedEntity.$$_hibernate_getNextManagedEntity() );
		assertSame( managedEntity, managedEntity.$$_hibernate_getPreviousManagedEntity() );

		// Add an attribute interceptor...
		assertTyping( PersistentAttributeInterceptable.class, entity );
		PersistentAttributeInterceptable interceptableEntity = (PersistentAttributeInterceptable) entity;

		assertNull( interceptableEntity.$$_hibernate_getInterceptor() );
		interceptableEntity.$$_hibernate_setInterceptor( new ObjectAttributeMarkerInterceptor() );
		assertNotNull( interceptableEntity.$$_hibernate_getInterceptor() );

		assertNull( entity.anUnspecifiedObject );
		entity.setAnObject( new Object() );
		assertSame( entity.anUnspecifiedObject, ObjectAttributeMarkerInterceptor.WRITE_MARKER );
		assertSame( entity.getAnObject(), ObjectAttributeMarkerInterceptor.READ_MARKER );
		entity.setAnObject( null );
		assertSame( entity.anUnspecifiedObject, ObjectAttributeMarkerInterceptor.WRITE_MARKER );

	}

	protected void cleanup() {
	}
}

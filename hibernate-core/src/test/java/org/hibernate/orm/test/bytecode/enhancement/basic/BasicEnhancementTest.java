/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhancement.basic;

import org.hibernate.Version;
import org.hibernate.bytecode.enhance.spi.EnhancementInfo;
import org.hibernate.engine.spi.ManagedEntity;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;

import org.hibernate.testing.bytecode.enhancement.EnhancerTestUtils;
import org.hibernate.testing.bytecode.enhancement.extension.BytecodeEnhanced;
import org.hibernate.testing.orm.junit.Jira;
import org.junit.jupiter.api.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hibernate.testing.junit4.ExtraAssertions.assertTyping;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Luis Barreiro
 */
@BytecodeEnhanced
public class BasicEnhancementTest {

	@Test
	public void basicManagedTest() {
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
	}

	@Test
	@Jira("HHH-13439")
	public void enhancementInfoTest() {
		EnhancementInfo info = SimpleEntity.class.getAnnotation( EnhancementInfo.class );
		assertNotNull( info, "EnhancementInfo was not applied" );

		assertEquals( Version.getVersionString(), info.version() );
	}

	@Test
	public void basicInterceptableTest() {
		SimpleEntity entity = new SimpleEntity();

		assertTyping( PersistentAttributeInterceptable.class, entity );
		PersistentAttributeInterceptable interceptableEntity = (PersistentAttributeInterceptable) entity;

		assertNull( interceptableEntity.$$_hibernate_getInterceptor() );
		interceptableEntity.$$_hibernate_setInterceptor( new ObjectAttributeMarkerInterceptor() );
		assertNotNull( interceptableEntity.$$_hibernate_getInterceptor() );

		assertNull( EnhancerTestUtils.getFieldByReflection( entity, "anUnspecifiedObject" ) );
		entity.setAnObject( new Object() );

		assertSame( ObjectAttributeMarkerInterceptor.WRITE_MARKER, EnhancerTestUtils.getFieldByReflection( entity, "anUnspecifiedObject" ) );
		assertSame( ObjectAttributeMarkerInterceptor.READ_MARKER, entity.getAnObject() );

		entity.setAnObject( null );
		assertSame( ObjectAttributeMarkerInterceptor.WRITE_MARKER, EnhancerTestUtils.getFieldByReflection( entity, "anUnspecifiedObject" ) );
	}

	@Test
	public void basicExtendedEnhancementTest() {
		// test uses ObjectAttributeMarkerInterceptor to ensure that field access is routed through enhanced methods

		SimpleEntity entity = new SimpleEntity();
		( (PersistentAttributeInterceptable) entity ).$$_hibernate_setInterceptor( new ObjectAttributeMarkerInterceptor() );

		Object decoy = new Object();
		entity.anUnspecifiedObject = decoy;

		Object gotByReflection = EnhancerTestUtils.getFieldByReflection( entity, "anUnspecifiedObject" );
		assertNotSame( decoy, gotByReflection );
		assertSame( ObjectAttributeMarkerInterceptor.WRITE_MARKER, gotByReflection );

		Object entityObject = entity.anUnspecifiedObject;

		assertNotSame( decoy, entityObject );
		assertSame( ObjectAttributeMarkerInterceptor.READ_MARKER, entityObject );

		// do some more calls on the various types, without the interceptor
		( (PersistentAttributeInterceptable) entity ).$$_hibernate_setInterceptor( null );

		entity.id = 1234567890L;
		assertEquals( 1234567890L, (long) entity.getId() );

		entity.name = "Entity Name";
		assertSame( "Entity Name", entity.name );

		entity.active = true;
		assertTrue( entity.getActive() );

		entity.someStrings = Arrays.asList( "A", "B", "C", "D" );
		assertArrayEquals( new String[]{"A", "B", "C", "D"}, entity.someStrings.toArray() );
	}

	// --- //

	@Entity
	private static class SimpleEntity {

		Object anUnspecifiedObject;

		@Id
		Long id;

		String name;

		Boolean active;

		List<String> someStrings;

		Long getId() {
			return id;
		}

		void setId(Long id) {
			this.id = id;
		}

		String getName() {
			return name;
		}

		void setName(String name) {
			this.name = name;
		}

		public Boolean getActive() {
			return active;
		}

		public void setActive(Boolean active) {
			this.active = active;
		}

		Object getAnObject() {
			return anUnspecifiedObject;
		}

		void setAnObject(Object providedObject) {
			this.anUnspecifiedObject = providedObject;
		}

		List<String> getSomeStrings() {
			return Collections.unmodifiableList( someStrings );
		}

		void setSomeStrings(List<String> someStrings) {
			this.someStrings = someStrings;
		}
	}
}

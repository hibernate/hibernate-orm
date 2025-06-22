/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.bytecode.enhancement;

import java.lang.reflect.Field;

import org.hibernate.LockMode;
import org.hibernate.engine.internal.MutableEntityEntryFactory;
import org.hibernate.engine.spi.EntityEntry;
import org.hibernate.engine.spi.SelfDirtinessTracker;
import org.hibernate.engine.spi.Status;
import org.hibernate.internal.util.ReflectHelper;

import org.hibernate.testing.junit4.BaseUnitTestCase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;

/**
 * utility class to use in bytecode enhancement tests
 *
 * @author Steve Ebersole
 * @author Luis Barreiro
 */
public abstract class EnhancerTestUtils extends BaseUnitTestCase {

	public static Object getFieldByReflection(Object entity, String fieldName) {
		try {
			Field field = findField( entity.getClass(), fieldName );
			ReflectHelper.ensureAccessibility( field );
			return field.get( entity );
		}
		catch (NoSuchFieldException | IllegalAccessException e) {
			fail( "Fail to get field '" + fieldName + "' in entity " + entity + ": " + e.getMessage() );
		}
		return null;
	}

	/**
	 * clears the dirty set for an entity
	 */
	public static void clearDirtyTracking(Object entityInstance) {
		( (SelfDirtinessTracker) entityInstance ).$$_hibernate_clearDirtyAttributes();
	}

	/**
	 * compares the dirty fields of an entity with a set of expected values
	 */
	public static void checkDirtyTracking(Object entityInstance, String... dirtyFields) {
		final SelfDirtinessTracker selfDirtinessTracker = (SelfDirtinessTracker) entityInstance;
		assertThat( selfDirtinessTracker.$$_hibernate_getDirtyAttributes() )
				.containsExactlyInAnyOrder( dirtyFields );
		assertThat( selfDirtinessTracker.$$_hibernate_hasDirtyAttributes() )
				.isEqualTo( dirtyFields.length > 0 );
	}

	public static EntityEntry makeEntityEntry() {
		return MutableEntityEntryFactory.INSTANCE.createEntityEntry(
				Status.MANAGED,
				null,
				null,
				1,
				null,
				LockMode.NONE,
				false,
				null,
				false,
				null
		);
	}

	private static Field findField(Class<?> clazz, String fieldName) throws NoSuchFieldException {
		NoSuchFieldException exception = null;
		while ( clazz != null ) {
			try {
				return clazz.getDeclaredField( fieldName );
			}
			catch (NoSuchFieldException e) {
				if ( exception == null ) {
					exception = e;
				}
				clazz = clazz.getSuperclass();
			}
		}
		throw exception;
	}

}

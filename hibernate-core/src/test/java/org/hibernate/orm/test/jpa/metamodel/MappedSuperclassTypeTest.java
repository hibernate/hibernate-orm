/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.jpa.metamodel;

import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.IdentifiableType;
import jakarta.persistence.metamodel.ManagedType;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.Jpa;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@Jpa(annotatedClasses = {
		SomeMappedSuperclassSubclass.class
})
public class MappedSuperclassTypeTest {

	@Test
	@JiraKey( value = "HHH-6896" )
	public void ensureMappedSuperclassTypeReturnedAsManagedType(EntityManagerFactoryScope scope) {
		ManagedType<SomeMappedSuperclass> type = scope.getEntityManagerFactory().getMetamodel().managedType( SomeMappedSuperclass.class );
		// the issue was in regards to throwing an exception, but also check for nullness
		assertNotNull( type );
	}

	@Test
	@JiraKey( value = "HHH-8533" )
	@SuppressWarnings("unchecked")
	public void testAttributeAccess(EntityManagerFactoryScope scope) {
		final EntityType<SomeMappedSuperclassSubclass> entityType =  scope.getEntityManagerFactory().getMetamodel().entity( SomeMappedSuperclassSubclass.class );
		final IdentifiableType<SomeMappedSuperclass> mappedSuperclassType = (IdentifiableType<SomeMappedSuperclass>) entityType.getSupertype();

		assertNotNull( entityType.getId( Long.class ) );
		try {
			entityType.getDeclaredId( Long.class );
			fail();
		}
		catch (IllegalArgumentException expected) {
		}

		assertNotNull( mappedSuperclassType.getId( Long.class ) );
		assertNotNull( mappedSuperclassType.getDeclaredId( Long.class ) );
	}
}

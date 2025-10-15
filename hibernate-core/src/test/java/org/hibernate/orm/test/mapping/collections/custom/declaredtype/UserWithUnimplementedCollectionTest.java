/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.collections.custom.declaredtype;

import org.hibernate.AnnotationException;
import org.hibernate.boot.MetadataSources;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import jakarta.persistence.OneToMany;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test that we get an exception when an attribute whose type is not a Collection
 * is annotated with any of <ul>
 *     <li>{@linkplain jakarta.persistence.ElementCollection}</li>
 *     <li>{@linkplain jakarta.persistence.OneToMany}</li>
 *     <li>{@linkplain jakarta.persistence.ManyToMany}</li>
 *     <li>{@linkplain org.hibernate.annotations.ManyToAny}</li>
 * </ul>
 * The test specifically uses {@linkplain OneToMany}, but the handling is the same
 *
 * @author Max Rydahl Andersen
 * @author David Weinberg
 */
@ServiceRegistry
public class UserWithUnimplementedCollectionTest {
	@Test
	void testCollectionNotCollectionFailure(ServiceRegistryScope serviceRegistryScope) {
		final MetadataSources metadataSources = new MetadataSources( serviceRegistryScope.getRegistry() );
		metadataSources.addAnnotatedClasses( UserWithUnimplementedCollection.class, Email.class );
		try {
			metadataSources.buildMetadata();
			fail( "Expecting an AnnotationException" );
		}
		catch (AnnotationException e) {
			assertThat( e ).hasMessageEndingWith( "is not a collection and may not be a '@OneToMany', '@ManyToMany', or '@ElementCollection'" );
			assertThat( e ).hasMessageContaining( ".emailAddresses" );
		}
	}
}

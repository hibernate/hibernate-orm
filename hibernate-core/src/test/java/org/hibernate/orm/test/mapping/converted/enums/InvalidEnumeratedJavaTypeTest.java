/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.enums;

import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;

import org.hibernate.AnnotationException;
import org.hibernate.boot.MetadataSources;

import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.ServiceRegistryScope;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@ServiceRegistry
public class InvalidEnumeratedJavaTypeTest {
	@Test
	public void testInvalidMapping(ServiceRegistryScope scope) {
		MetadataSources metadataSources = new MetadataSources( scope.getRegistry() )
			.addAnnotatedClass( TheEntity.class );
		try {
			metadataSources.buildMetadata();
			fail( "Was expecting failure" );
		}
		catch (AnnotationException ignore) {
		}
	}

	@Entity
	public static class TheEntity {
		@Id private Long id;
		@Enumerated private Boolean yesNo;
	}
}

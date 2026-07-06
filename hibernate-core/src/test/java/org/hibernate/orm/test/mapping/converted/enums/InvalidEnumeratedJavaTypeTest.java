/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.mapping.converted.enums;

import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;

import org.hibernate.AnnotationException;

import org.hibernate.orm.test.boot.MetadataBuildingTestHelper;
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
		try {
			MetadataBuildingTestHelper.buildMetadata( scope.getRegistry(), TheEntity.class );
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

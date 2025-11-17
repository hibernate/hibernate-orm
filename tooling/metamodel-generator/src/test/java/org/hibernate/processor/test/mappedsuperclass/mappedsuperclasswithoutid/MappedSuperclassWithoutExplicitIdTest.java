/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.mappedsuperclass.mappedsuperclasswithoutid;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.assertPresenceOfFieldInMetamodelFor;

/**
 * @author Hardy Ferentschik
 */
@CompilationTest
class MappedSuperclassWithoutExplicitIdTest {
	@Test
	@WithClasses({ ConcreteProduct.class, Product.class, Shop.class })
	void testRightAccessTypeForMappedSuperclass() {
		assertMetamodelClassGeneratedFor( ConcreteProduct.class );
		assertMetamodelClassGeneratedFor( Product.class );
		assertMetamodelClassGeneratedFor( Shop.class );
		assertPresenceOfFieldInMetamodelFor( Product.class, "shop", "The many to one attribute shop is missing" );
	}
}

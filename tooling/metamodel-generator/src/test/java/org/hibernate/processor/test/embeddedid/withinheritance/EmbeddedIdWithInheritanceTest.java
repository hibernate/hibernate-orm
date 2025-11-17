/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.embeddedid.withinheritance;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.hibernate.processor.test.util.WithMappingFiles;
import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.assertPresenceOfFieldInMetamodelFor;

/**
 * @author Hardy Ferentschik
 */
@CompilationTest
class EmbeddedIdWithInheritanceTest {
	@Test
	@WithClasses({ Ref.class, AbstractRef.class, TestEntity.class })
	@WithMappingFiles("orm.xml")
	void testEntityContainsEmbeddedIdProperty() {
		assertMetamodelClassGeneratedFor( TestEntity.class );
		assertPresenceOfFieldInMetamodelFor(
				TestEntity.class, "ref", "Property ref should be in metamodel"
		);
	}
}

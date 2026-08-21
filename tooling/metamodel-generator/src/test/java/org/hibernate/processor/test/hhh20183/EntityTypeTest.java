/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.hhh20183;


import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;


import static org.hibernate.processor.test.util.TestUtil.assertAttributeTypeInMetaModelFor;
import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;

/**
 * @author Emmanuel Bernard
 */
@CompilationTest
class EntityTypeTest {

	@Test
	@WithClasses({EntityType.class, FormType.class})
	void testOneToMany() {
		System.out.println( getMetaModelSourceAsString( FormType.class ) );
		assertMetamodelClassGeneratedFor( FormType.class );
		assertAttributeTypeInMetaModelFor( FormType.class,
				"entityType", EntityType.class,
				"EntityType must be qualified");
	}
}

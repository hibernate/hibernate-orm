/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.inheritance.basic;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.Test;

import static org.hibernate.processor.test.util.TestUtil.assertAttributeTypeInMetaModelFor;
import static org.hibernate.processor.test.util.TestUtil.assertPresenceOfFieldInMetamodelFor;
import static org.hibernate.processor.test.util.TestUtil.assertSuperclassRelationshipInMetamodel;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public class InheritanceTest extends CompilationTest {
	@Test
	@WithClasses({
			AbstractEntity.class,
			Area.class,
			Building.class,
			Customer.class,
			House.class,
			Person.class,
			User.class
	})
	public void testInheritance() throws Exception {

		// entity inheritance
		assertSuperclassRelationshipInMetamodel( Customer.class, User.class );


		// mapped super class
		assertSuperclassRelationshipInMetamodel( House.class, Building.class );
		assertSuperclassRelationshipInMetamodel( Building.class, Area.class );

		// METAGEN-29
		assertSuperclassRelationshipInMetamodel( Person.class, AbstractEntity.class );
		assertPresenceOfFieldInMetamodelFor( AbstractEntity.class, "id", "Property 'id' should exist" );
		assertPresenceOfFieldInMetamodelFor( AbstractEntity.class, "foo", "Property should exist - METAGEN-29" );
		assertAttributeTypeInMetaModelFor(
				AbstractEntity.class,
				"foo",
				Object.class,
				"Object is the upper bound of foo "
		);

		assertPresenceOfFieldInMetamodelFor( Person.class, "name", "Property 'name' should exist" );
	}
}

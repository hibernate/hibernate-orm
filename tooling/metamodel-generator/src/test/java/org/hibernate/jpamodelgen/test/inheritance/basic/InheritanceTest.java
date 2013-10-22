/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hibernate.jpamodelgen.test.inheritance.basic;

import org.testng.annotations.Test;

import org.hibernate.jpamodelgen.test.util.CompilationTest;

import static org.hibernate.jpamodelgen.test.util.TestUtil.assertAttributeTypeInMetaModelFor;
import static org.hibernate.jpamodelgen.test.util.TestUtil.assertPresenceOfFieldInMetamodelFor;
import static org.hibernate.jpamodelgen.test.util.TestUtil.assertSuperClassRelationShipInMetamodel;

/**
 * @author Emmanuel Bernard
 * @author Hardy Ferentschik
 */
public class InheritanceTest extends CompilationTest {
	@Test
	public void testInheritance() throws Exception {

		// entity inheritance
		assertSuperClassRelationShipInMetamodel( Customer.class, User.class );


		// mapped super class
		assertSuperClassRelationShipInMetamodel( House.class, Building.class );
		assertSuperClassRelationShipInMetamodel( Building.class, Area.class );

		// METAGEN-29
		assertSuperClassRelationShipInMetamodel( Person.class, AbstractEntity.class );
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

	@Override
	protected String getPackageNameOfCurrentTest() {
		return InheritanceTest.class.getPackage().getName();
	}
}


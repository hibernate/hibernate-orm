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


package org.hibernate.jpamodelgen.test.targetannotation;

import org.testng.annotations.Test;

import org.hibernate.jpamodelgen.test.util.CompilationTest;
import org.hibernate.jpamodelgen.test.util.TestForIssue;

import static org.hibernate.jpamodelgen.test.util.TestUtil.assertAttributeTypeInMetaModelFor;
import static org.hibernate.jpamodelgen.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.jpamodelgen.test.util.TestUtil.assertPresenceOfFieldInMetamodelFor;

/**
 * @author Hardy Ferentschik
 */
public class TargetAnnotationTest extends CompilationTest {

	@Test
	@TestForIssue(jiraKey = "METAGEN-30")
	public void testEmbeddableWithTargetAnnotation() {
		assertMetamodelClassGeneratedFor( House.class );
		assertPresenceOfFieldInMetamodelFor( House.class, "address", "the metamodel should have a member 'address'" );
		assertAttributeTypeInMetaModelFor(
				House.class,
				"address",
				AddressImpl.class,
				"The target annotation set the type to AddressImpl"
		);
	}

	@Override
	protected String getPackageNameOfCurrentTest() {
		return TargetAnnotationTest.class.getPackage().getName();
	}
}

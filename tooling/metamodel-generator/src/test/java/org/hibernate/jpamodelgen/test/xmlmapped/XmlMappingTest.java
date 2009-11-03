// $Id$
/*
* JBoss, Home of Professional Open Source
* Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.hibernate.jpamodelgen.test.xmlmapped;

import org.testng.annotations.Test;

import org.hibernate.jpamodelgen.test.util.CompilationTest;
import static org.hibernate.jpamodelgen.test.util.TestUtil.assertClassGenerated;
import static org.hibernate.jpamodelgen.test.util.TestUtil.assertPresenceOfField;
import static org.hibernate.jpamodelgen.test.util.TestUtil.assertSuperClass;

/**
 * @author Hardy Ferentschik
 */
public class XmlMappingTest extends CompilationTest {
	@Test
	public void testXmlConfiguredEmbeddedClassGenerated() throws Exception {
		assertClassGenerated( Address.class.getName() + "_" );
	}

	@Test
	public void testXmlConfiguredMappedSuperclassGenerated() throws Exception {
		assertClassGenerated( Building.class.getName() + "_" );
		assertPresenceOfField(
				Building.class.getName() + "_", "address", "address field should exist"
		);
	}

	@Test
	public void testClassHierarchy() throws Exception {
		assertClassGenerated( Mammal.class.getName() + "_" );
		assertClassGenerated( LivingBeing.class.getName() + "_" );
		assertSuperClass( Mammal.class.getName() + "_", LivingBeing.class.getName() + "_" );
	}

	@Test(expectedExceptions = ClassNotFoundException.class)
	public void testNonExistentMappedClassesGetIgnored() throws Exception {
		Class.forName( "org.hibernate.jpamodelgen.test.model.Dummy_" );
	}

	@Override
	protected String getTestPackage() {
		return Address.class.getPackage().getName();
	}
}
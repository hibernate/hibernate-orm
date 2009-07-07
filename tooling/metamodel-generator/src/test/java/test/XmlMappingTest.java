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
package test;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import org.testng.annotations.Test;

/**
 * @author Hardy Ferentschik
 */
public class XmlMappingTest {
	@Test
	public void testXmlConfiguredEmbeddedClassGenerated() throws Exception {
		assertNotNull( Class.forName( "model.xmlmapped.Address_" ) );
	}

	@Test
	public void testXmlConfiguredMappedSuperclassGenerated() throws Exception {
		Class<?> building = Class.forName( "model.xmlmapped.Building_" );
		assertNotNull( building );
		assertNotNull( building.getField( "address" ) );
	}

	@Test
	public void testClassHierarchy() throws Exception {
		Class<?> mammal = Class.forName( "model.xmlmapped.Mammal_" );
		assertNotNull( mammal );

		Class<?> being = Class.forName( "model.xmlmapped.LivingBeing_" );
		assertNotNull( being );

		assertTrue( mammal.getSuperclass().equals( being ) );
	}

	@Test(expectedExceptions = ClassNotFoundException.class)
	public void testNonExistentMappedClassesGetIgnored() throws Exception {
		Class.forName( "model.Dummy_" );
	}
}
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
package org.hibernate.jpamodelgen.test.xmlmapped;

import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.Test;

import org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor;
import org.hibernate.jpamodelgen.test.util.CompilationTest;
import org.hibernate.jpamodelgen.test.util.TestForIssue;
import org.hibernate.jpamodelgen.test.util.TestUtil;

import static org.hibernate.jpamodelgen.test.util.TestUtil.assertAttributeTypeInMetaModelFor;
import static org.hibernate.jpamodelgen.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.jpamodelgen.test.util.TestUtil.assertPresenceOfFieldInMetamodelFor;
import static org.hibernate.jpamodelgen.test.util.TestUtil.assertSuperClassRelationShipInMetamodel;

/**
 * @author Hardy Ferentschik
 */
public class XmlMappingTest extends CompilationTest {
	@Test
	public void testXmlConfiguredEmbeddedClassGenerated() {
		assertMetamodelClassGeneratedFor( Address.class );
	}

	@Test
	public void testXmlConfiguredMappedSuperclassGenerated() {
		assertMetamodelClassGeneratedFor( Building.class );
		assertPresenceOfFieldInMetamodelFor( Building.class, "address", "address field should exist" );
	}

	@Test
	@TestForIssue(jiraKey = "METAGEN-17")
	public void testTargetEntityOnOneToOne() {
		assertMetamodelClassGeneratedFor( Boy.class );
		assertPresenceOfFieldInMetamodelFor( Boy.class, "favoriteSuperhero", "favoriteSuperhero field should exist" );
		assertAttributeTypeInMetaModelFor(
				Boy.class, "favoriteSuperhero", FakeHero.class, "target entity overridden in xml"
		);
	}

	@Test
	@TestForIssue(jiraKey = "METAGEN-17")
	public void testTargetEntityOnOneToMany() {
		assertMetamodelClassGeneratedFor( Boy.class );
		assertPresenceOfFieldInMetamodelFor( Boy.class, "knowsHeroes", "knowsHeroes field should exist" );
		assertAttributeTypeInMetaModelFor(
				Boy.class, "knowsHeroes", FakeHero.class, "target entity overridden in xml"
		);
	}

	@Test
	@TestForIssue(jiraKey = "METAGEN-17")
	public void testTargetEntityOnManyToMany() {
		assertMetamodelClassGeneratedFor( Boy.class );
		assertPresenceOfFieldInMetamodelFor( Boy.class, "savedBy", "savedBy field should exist" );
		assertAttributeTypeInMetaModelFor(
				Boy.class, "savedBy", FakeHero.class, "target entity overridden in xml"
		);
	}

	@Test
	public void testXmlConfiguredElementCollection() {
		assertMetamodelClassGeneratedFor( Boy.class );
		assertPresenceOfFieldInMetamodelFor( Boy.class, "nickNames", "nickNames field should exist" );
		assertAttributeTypeInMetaModelFor( Boy.class, "nickNames", String.class, "target class overridden in xml" );
	}

	@Test
	public void testClassHierarchy() {
		assertMetamodelClassGeneratedFor( Mammal.class );
		assertMetamodelClassGeneratedFor( LivingBeing.class );
		assertSuperClassRelationShipInMetamodel( Mammal.class, LivingBeing.class );
	}

	@Test(expectedExceptions = ClassNotFoundException.class)
	public void testNonExistentMappedClassesGetIgnored() throws Exception {
		Class.forName( "org.hibernate.jpamodelgen.test.model.Dummy_" );
	}

	@Override
	protected String getPackageNameOfCurrentTest() {
		return XmlMappingTest.class.getPackage().getName();
	}

	@Override
	protected Map<String, String> getProcessorOptions() {
		Map<String, String> properties = new HashMap<String, String>();
		properties.put(
				JPAMetaModelEntityProcessor.PERSISTENCE_XML_OPTION,
				TestUtil.fcnToPath( XmlMappingTest.class.getPackage().getName() ) + "/persistence.xml"
		);
		return properties;
	}
}

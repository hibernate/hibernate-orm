/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.xmlmapped;

import org.hibernate.processor.HibernateProcessor;
import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestForIssue;
import org.hibernate.processor.test.util.WithClasses;
import org.hibernate.processor.test.util.WithProcessorOption;
import org.junit.Test;

import static org.hibernate.processor.test.util.TestUtil.assertAttributeTypeInMetaModelFor;
import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.assertPresenceOfFieldInMetamodelFor;
import static org.hibernate.processor.test.util.TestUtil.assertSuperclassRelationshipInMetamodel;

/**
 * @author Hardy Ferentschik
 */
// TODO - differentiate needed classes per test better. Right now all test classes are processed for each test (HF)
@WithClasses({
		Address.class,
		Boy.class,
		Building.class,
		FakeHero.class,
		LivingBeing.class,
		Mammal.class,
		Superhero.class
})
@WithProcessorOption(key = HibernateProcessor.PERSISTENCE_XML_OPTION,
		value = "org/hibernate/processor/test/xmlmapped/persistence.xml")
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
		assertSuperclassRelationshipInMetamodel( Mammal.class, LivingBeing.class );
	}

	@Test(expected = ClassNotFoundException.class)
	public void testNonExistentMappedClassesGetIgnored() throws Exception {
		Class.forName( "org.hibernate.processor.test.model.Dummy_" );
	}
}

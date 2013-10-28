/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2010, Red Hat, Inc. and/or its affiliates or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat, Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.jpamodelgen.test.xmlmapped;

import org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor;
import org.hibernate.jpamodelgen.test.util.CompilationTest;
import org.hibernate.jpamodelgen.test.util.TestForIssue;
import org.hibernate.jpamodelgen.test.util.WithClasses;
import org.hibernate.jpamodelgen.test.util.WithProcessorOption;
import org.junit.Test;

import static org.hibernate.jpamodelgen.test.util.TestUtil.assertAttributeTypeInMetaModelFor;
import static org.hibernate.jpamodelgen.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.jpamodelgen.test.util.TestUtil.assertPresenceOfFieldInMetamodelFor;
import static org.hibernate.jpamodelgen.test.util.TestUtil.assertSuperClassRelationShipInMetamodel;

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
@WithProcessorOption(key = JPAMetaModelEntityProcessor.PERSISTENCE_XML_OPTION,
		value = "org/hibernate/jpamodelgen/test/xmlmapped/persistence.xml")
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

	@Test(expected = ClassNotFoundException.class)
	public void testNonExistentMappedClassesGetIgnored() throws Exception {
		Class.forName( "org.hibernate.jpamodelgen.test.model.Dummy_" );
	}
}

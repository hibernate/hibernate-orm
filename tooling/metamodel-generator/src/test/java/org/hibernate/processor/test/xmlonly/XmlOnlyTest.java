/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.xmlonly;

import org.hibernate.processor.HibernateProcessor;
import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.hibernate.processor.test.util.WithProcessorOption;
import org.junit.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.assertPresenceOfFieldInMetamodelFor;

/**
 * @author Hardy Ferentschik
 */
@WithClasses({ Car.class, Course.class, Option.class, Period.class, Teacher.class, Tire.class, XmlOnly.class })
@WithProcessorOption(key = HibernateProcessor.PERSISTENCE_XML_OPTION,
		value = "org/hibernate/processor/test/xmlonly/persistence.xml")
public class XmlOnlyTest extends CompilationTest {
	@Test
	public void testMetaModelGeneratedForXmlConfiguredEntity() {
		assertMetamodelClassGeneratedFor( XmlOnly.class );
	}

	@Test
	public void testMetaModelGeneratedForManyToManyFieldAccessWithoutTargetEntity() {
		assertPresenceOfFieldInMetamodelFor( Course.class, "qualifiedTeachers", "Type should be inferred from field" );
		assertPresenceOfFieldInMetamodelFor( Teacher.class, "qualifiedFor", "Type should be inferred from field" );
	}

	@Test
	public void testMetaModelGeneratedForOneToManyPropertyAccessWithoutTargetEntity() {
		assertPresenceOfFieldInMetamodelFor( Car.class, "tires", "Type should be inferred from field" );
		assertPresenceOfFieldInMetamodelFor( Tire.class, "car", "Type should be inferred from field" );
	}

	@Test
	public void testMetaModelGeneratedForEmbeddable() {
		assertPresenceOfFieldInMetamodelFor( Option.class, "period", "Embedded expected" );
		assertPresenceOfFieldInMetamodelFor( Period.class, "start", "Embedded expected" );
		assertPresenceOfFieldInMetamodelFor( Period.class, "end", "Embedded expected" );
	}
}

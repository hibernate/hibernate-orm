/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.xmlentity;

import org.hibernate.processor.HibernateProcessor;
import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.hibernate.processor.test.util.WithProcessorOption;
import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;

/**
 * Verifies that a persistence.xml declaring its mapping file through an external entity
 * is processed when {@value HibernateProcessor#ALLOW_DTD_AND_EXTERNAL_ENTITIES} is enabled.
 */
@CompilationTest
@WithClasses(XmlEntity.class)
@WithProcessorOption(key = HibernateProcessor.PERSISTENCE_XML_OPTION,
		value = "org/hibernate/processor/test/xmlentity/persistence.xml")
public class PersistenceXmlExternalEntityTest {
	@Test
	@WithProcessorOption(key = HibernateProcessor.ALLOW_DTD_AND_EXTERNAL_ENTITIES, value = "true")
	void testExternalEntityResolvedWhenAllowed() {
		assertMetamodelClassGeneratedFor( XmlEntity.class );
	}
}

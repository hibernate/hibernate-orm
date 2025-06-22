/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.xmlmetacomplete.multiplepus;

import org.hibernate.processor.HibernateProcessor;
import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestForIssue;
import org.hibernate.processor.test.util.WithClasses;
import org.hibernate.processor.test.util.WithProcessorOption;
import org.junit.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;

/**
 * @author Hardy Ferentschik
 */
@TestForIssue(jiraKey = "METAGEN-86")
public class XmlMetaDataCompleteMultiplePersistenceUnitsTest extends CompilationTest {

	@Test
	@WithClasses(Dummy.class)
	@WithProcessorOption(key = HibernateProcessor.PERSISTENCE_XML_OPTION,
			value = "org/hibernate/processor/test/xmlmetacomplete/multiplepus/persistence.xml")
	public void testMetaModelGenerated() {
		// only one of the xml files in the example uses 'xml-mapping-metadata-complete', hence annotation processing
		// kicks in
		assertMetamodelClassGeneratedFor( Dummy.class );
	}
}

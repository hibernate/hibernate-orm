/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.xmlmetacomplete.singlepu;

import org.hibernate.processor.HibernateProcessor;
import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.hibernate.processor.test.util.WithProcessorOption;
import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.assertNoSourceFileGeneratedFor;

/**
 * @author Hardy Ferentschik
 */
@CompilationTest
class XmlMetaDataCompleteSinglePersistenceUnitTest {
	@Test
	@WithClasses(org.hibernate.processor.test.xmlmetacomplete.multiplepus.Dummy.class)
	@WithProcessorOption(key = HibernateProcessor.PERSISTENCE_XML_OPTION,
			value = "org/hibernate/processor/test/xmlmetacomplete/singlepu/persistence.xml")
	void testNoMetaModelGenerated() {
		// the xml mapping files used in the example say that the xml data is meta complete. For that
		// reason there should be no meta model source file for the annotated Dummy entity
		assertNoSourceFileGeneratedFor( Dummy.class );
	}
}

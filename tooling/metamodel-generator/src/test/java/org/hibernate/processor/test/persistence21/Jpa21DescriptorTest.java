/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.persistence21;

import org.hibernate.processor.HibernateProcessor;
import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.TestForIssue;
import org.hibernate.processor.test.util.WithClasses;
import org.hibernate.processor.test.util.WithProcessorOption;
import org.junit.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;

/**
 * Test for parsing JPA 2.1 descriptors.
 *
 * @author Hardy Ferentschik
 */
public class Jpa21DescriptorTest extends CompilationTest {

	@Test
	@TestForIssue(jiraKey = "METAGEN-92")
	@WithClasses(Snafu.class)
	@WithProcessorOption(key = HibernateProcessor.PERSISTENCE_XML_OPTION,
			value = "org/hibernate/processor/test/persistence21/persistence.xml")
	public void testMetaModelGeneratedForXmlConfiguredEntity() {
		assertMetamodelClassGeneratedFor( Snafu.class );
	}
}

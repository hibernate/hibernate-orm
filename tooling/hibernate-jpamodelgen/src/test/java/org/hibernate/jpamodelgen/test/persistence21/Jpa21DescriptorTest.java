/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.test.persistence21;

import org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor;
import org.hibernate.jpamodelgen.test.util.CompilationTest;
import org.hibernate.jpamodelgen.test.util.TestForIssue;
import org.hibernate.jpamodelgen.test.util.WithClasses;
import org.hibernate.jpamodelgen.test.util.WithProcessorOption;
import org.junit.Test;

import static org.hibernate.jpamodelgen.test.util.TestUtil.assertMetamodelClassGeneratedFor;

/**
 * Test for parsing JPA 2.1 descriptors.
 *
 * @author Hardy Ferentschik
 */
public class Jpa21DescriptorTest extends CompilationTest {

	@Test
	@TestForIssue(jiraKey = "METAGEN-92")
	@WithClasses(Snafu.class)
	@WithProcessorOption(key = JPAMetaModelEntityProcessor.PERSISTENCE_XML_OPTION,
			value = "org/hibernate/jpamodelgen/test/persistence21/persistence.xml")
	public void testMetaModelGeneratedForXmlConfiguredEntity() {
		assertMetamodelClassGeneratedFor( Snafu.class );
	}
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.test.xmlmetacomplete.multiplepus;

import org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor;
import org.hibernate.jpamodelgen.test.util.CompilationTest;
import org.hibernate.jpamodelgen.test.util.TestForIssue;
import org.hibernate.jpamodelgen.test.util.WithClasses;
import org.hibernate.jpamodelgen.test.util.WithProcessorOption;
import org.junit.Test;

import static org.hibernate.jpamodelgen.test.util.TestUtil.assertMetamodelClassGeneratedFor;

/**
 * @author Hardy Ferentschik
 */
@TestForIssue(jiraKey = "METAGEN-86")
public class XmlMetaDataCompleteMultiplePersistenceUnitsTest extends CompilationTest {

	@Test
	@WithClasses(Dummy.class)
	@WithProcessorOption(key = JPAMetaModelEntityProcessor.PERSISTENCE_XML_OPTION,
			value = "org/hibernate/jpamodelgen/test/xmlmetacomplete/multiplepus/persistence.xml")
	public void testMetaModelGenerated() {
		// only one of the xml files in the example uses 'xml-mapping-metadata-complete', hence annotation processing
		// kicks in
		assertMetamodelClassGeneratedFor( Dummy.class );
	}
}


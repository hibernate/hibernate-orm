/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpamodelgen.test.xmlmetacomplete.singlepu;

import org.hibernate.jpamodelgen.JPAMetaModelEntityProcessor;
import org.hibernate.jpamodelgen.test.util.CompilationTest;
import org.hibernate.jpamodelgen.test.util.WithClasses;
import org.hibernate.jpamodelgen.test.util.WithProcessorOption;
import org.junit.Test;

import static org.hibernate.jpamodelgen.test.util.TestUtil.assertNoSourceFileGeneratedFor;

/**
 * @author Hardy Ferentschik
 */
public class XmlMetaDataCompleteSinglePersistenceUnitTest extends CompilationTest {
	@Test
	@WithClasses(org.hibernate.jpamodelgen.test.xmlmetacomplete.multiplepus.Dummy.class)
	@WithProcessorOption(key = JPAMetaModelEntityProcessor.PERSISTENCE_XML_OPTION,
			value = "org/hibernate/jpamodelgen/test/xmlmetacomplete/singlepu/persistence.xml")
	public void testNoMetaModelGenerated() {
		// the xml mapping files used in the example say that the xml data is meta complete. For that
		// reason there should be no meta model source file for the annotated Dummy entity
		assertNoSourceFileGeneratedFor( Dummy.class );
	}
}

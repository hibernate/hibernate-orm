/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.test.xmlmapped;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.hibernate.processor.test.util.WithMappingFiles;
import org.junit.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;

/**
 * @author Hardy Ferentschik
 */
public class IgnoreInvalidXmlTest extends CompilationTest {
	@Test
	@WithClasses(Superhero.class)
	@WithMappingFiles({ "orm.xml", "jpa1-orm.xml", "malformed-mapping.xml", "non-existend-class.xml" })
	public void testInvalidXmlFilesGetIgnored() {
		// this is only a indirect test, but if the invalid xml files would cause the processor to abort the
		// meta class would not have been generated
		assertMetamodelClassGeneratedFor( Superhero.class );
	}
}

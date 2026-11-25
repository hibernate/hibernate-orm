/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.test.xmlentity;

import org.hibernate.processor.HibernateProcessor;
import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.hibernate.processor.test.util.WithMappingFiles;
import org.junit.Test;

import static org.hibernate.processor.test.util.TestUtil.assertNoMetamodelClassGeneratedFor;

/**
 * Verifies that a mapping file including its entity through an external entity
 * is not processed unless {@value HibernateProcessor#ALLOW_DTD_AND_EXTERNAL_ENTITIES} is enabled.
 */
@WithClasses(XmlEntity.class)
@WithMappingFiles("orm.xml")
public class ExternalEntityTest extends CompilationTest {
	@Test
	public void testExternalEntityRejectedByDefault() {
		assertNoMetamodelClassGeneratedFor( XmlEntity.class );
	}
}

package org.hibernate.processor.test.xmlentity;

import org.hibernate.processor.HibernateProcessor;
import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.hibernate.processor.test.util.WithMappingFiles;
import org.hibernate.processor.test.util.WithProcessorOption;
import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.assertNoMetamodelClassGeneratedFor;

/**
 * Verifies that a mapping file including its entity through an external entity
 * is only processed when {@value HibernateProcessor#ALLOW_DTD_AND_EXTERNAL_ENTITIES} is enabled.
 */
@CompilationTest
@WithClasses(XmlEntity.class)
@WithMappingFiles("orm.xml")
public class ExternalEntityTest {
	@Test
	void testExternalEntityRejectedByDefault() {
		assertNoMetamodelClassGeneratedFor( XmlEntity.class );
	}

	@Test
	@WithProcessorOption(key = HibernateProcessor.ALLOW_DTD_AND_EXTERNAL_ENTITIES, value = "true")
	void testExternalEntityResolvedWhenAllowed() {
		assertMetamodelClassGeneratedFor( XmlEntity.class );
	}
}

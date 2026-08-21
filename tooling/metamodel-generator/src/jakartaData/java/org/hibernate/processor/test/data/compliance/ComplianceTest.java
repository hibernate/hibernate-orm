/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.compliance;

import org.hibernate.processor.HibernateProcessor;
import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.hibernate.processor.test.util.WithProcessorOption;
import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.assertMetamodelClassGeneratedFor;
import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;

// HHH-20230 Sort<?> and similar are allowed by the spec, but require a compliance flag with Hibernate ORM.
@CompilationTest
class ComplianceTest {
	@Test
	@WithClasses({ Book.class, BookRepository.class })
	@WithProcessorOption(key = HibernateProcessor.JAKARTA_DATA_SORT_COMPLIANCE, value = "true")
	void testWildcards() {
		System.out.println( getMetaModelSourceAsString( Book.class ) );
		System.out.println( getMetaModelSourceAsString( Book.class, true ) );
		System.out.println( getMetaModelSourceAsString( BookRepository.class ) );
		assertMetamodelClassGeneratedFor( Book.class, true );
		assertMetamodelClassGeneratedFor( Book.class );
		assertMetamodelClassGeneratedFor( BookRepository.class );
	}
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.securityannotation;

import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;
import static org.junit.jupiter.api.Assertions.assertTrue;

@CompilationTest
class SecurityAnnotationTest {
	@Test
	@WithClasses({ SecuredBook.class, SecuredBookRepository.class })
	void securityAnnotationsAreCopiedToRepositoryImplementation() {
		final String source = getMetaModelSourceAsString( SecuredBookRepository.class, true );
		System.out.println( source );

		// class-level @RolesAllowed
		assertTrue( source.contains( "@RolesAllowed(value={\"admin\"})" ),
				"class-level @RolesAllowed should be copied" );

		// method-level @DenyAll
		assertTrue( source.contains( "@Override\n\t@DenyAll" ),
				"method-level @DenyAll should be copied after @Override" );

		// method-level @PermitAll
		assertTrue( source.contains( "@Override\n\t@PermitAll" ),
				"method-level @PermitAll should be copied after @Override" );

		// method-level @RolesAllowed with multiple roles on delete method
		assertTrue( source.contains( "@Override\n\t@RolesAllowed(value={\"manager\",\"editor\"})" ),
				"method-level @RolesAllowed with multiple roles should be copied after @Override" );
	}
}

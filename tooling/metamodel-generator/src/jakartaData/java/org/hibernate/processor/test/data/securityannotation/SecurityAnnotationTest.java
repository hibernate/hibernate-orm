/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.securityannotation;

import org.hibernate.processor.HibernateProcessor;
import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.hibernate.processor.test.util.WithProcessorOption;
import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@CompilationTest
class SecurityAnnotationTest {
	@Test
	@WithClasses({ SecuredBook.class, SecuredBookRepository.class })
	void securityAnnotationsAreCopiedToRepositoryImplementation() {
		final String source = getMetaModelSourceAsString( SecuredBookRepository.class, true );
		System.out.println( source );

		// class-level @DeclareRoles
		assertTrue( source.contains( "@DeclareRoles(value={\"admin\",\"manager\",\"editor\"})" ),
				"class-level @DeclareRoles should be copied" );

		// class-level @RolesAllowed
		assertTrue( source.contains( "@RolesAllowed(value={\"admin\"})" ),
				"class-level @RolesAllowed should be copied" );

		// class-level @RunAs
		assertTrue( source.contains( "@RunAs(value=\"admin\")" ),
				"class-level @RunAs should be copied" );

		// method-level @DenyAll
		assertTrue( source.contains( "@Override\n\t@DenyAll" ),
				"method-level @DenyAll should be copied after @Override" );

		// method-level @PermitAll
		assertTrue( source.contains( "@Override\n\t@PermitAll" ),
				"method-level @PermitAll should be copied after @Override" );

		// method-level @RolesAllowed with multiple roles on insert method
		assertTrue( source.contains( "@Override\n\t@RolesAllowed(value={\"manager\",\"editor\"})" ),
				"method-level @RolesAllowed with multiple roles should be copied after @Override" );
	}

	@Test
	@WithClasses({ SecuredBook.class, SecuredBookRepository.class })
	@WithProcessorOption(key = HibernateProcessor.SUPPRESS_JAKARTA_DATA_SECURITY_ANNOTATIONS, value = "true")
	void securityAnnotationsAreNotCopiedWhenSuppressed() {
		final String source = getMetaModelSourceAsString( SecuredBookRepository.class, true );
		System.out.println( source );

		assertFalse( source.contains( "@DeclareRoles" ),
				"@DeclareRoles should not be copied when suppressed" );
		assertFalse( source.contains( "@RolesAllowed" ),
				"@RolesAllowed should not be copied when suppressed" );
		assertFalse( source.contains( "@RunAs" ),
				"@RunAs should not be copied when suppressed" );
		assertFalse( source.contains( "@DenyAll" ),
				"@DenyAll should not be copied when suppressed" );
		assertFalse( source.contains( "@PermitAll" ),
				"@PermitAll should not be copied when suppressed" );
	}
}

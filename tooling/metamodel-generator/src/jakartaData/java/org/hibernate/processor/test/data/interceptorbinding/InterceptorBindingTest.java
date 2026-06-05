/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.interceptorbinding;

import org.hibernate.processor.test.data.interceptorbinding.binding.Audited;
import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import static org.hibernate.processor.test.util.TestUtil.getMetaModelSourceAsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@CompilationTest
class InterceptorBindingTest {
	@Test
	@WithClasses({ Audited.class, InterceptedBook.class, InterceptedBookRepository.class })
	void copiedInterceptorBindingAnnotationsUseImports() {
		final String source = getMetaModelSourceAsString( InterceptedBookRepository.class, true );
		System.out.println( source );

		assertTrue( source.contains( "import org.hibernate.processor.test.data.interceptorbinding.binding.Audited;" ) );
		assertTrue( source.contains( "import org.hibernate.processor.test.data.interceptorbinding.binding.Audited.Mode;" ) );
		assertTrue( source.contains( "import org.hibernate.processor.test.data.interceptorbinding.binding.Audited.Nested;" ) );
		assertFalse( source.contains( "@org.hibernate.processor.test.data.interceptorbinding.binding.Audited" ) );
		assertFalse( source.contains( "org.hibernate.processor.test.data.interceptorbinding.InterceptedBook.class" ) );
		assertTrue( source.contains( "@Audited(" ) );
		assertTrue( source.contains( "@Override\n\t@Audited(" ) );
		assertTrue( source.contains( "mode=Mode.STRICT" ) );
		assertTrue( source.contains( "entity=InterceptedBook.class" ) );
		assertTrue( source.contains( "arrayType=InterceptedBook[].class" ) );
		assertTrue( source.contains( "nested=@Nested(value=InterceptedBook.class)" ) );
		assertTrue( source.contains( "nestedArray={@Nested(value=InterceptedBook.class)}" ) );
	}
}

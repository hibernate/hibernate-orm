/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.processor.test.data.nullablequery;

import jakarta.persistence.Reference;
import jakarta.persistence.StatementReference;
import jakarta.persistence.TypedQueryReference;
import org.hibernate.processor.test.util.CompilationTest;
import org.hibernate.processor.test.util.WithClasses;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.hibernate.processor.test.util.TestUtil.getMethodFromMetamodelFor;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

@CompilationTest
class NullableQueryTest {
	@Test
	@WithClasses({ Person.class, PersonRepository.class })
	void nonNullQueryArguments() throws ReflectiveOperationException {
		final var search = getMethodFromMetamodelFor( PersonRepository.class, "search", String.class, String.class );
		final var reference = (TypedQueryReference<?>) search.invoke( null, "Jane", "Doe" );
		assertEquals( List.of( "firstName", "lastName" ), reference.getParameterNames() );
		assertEquals( List.of( "Jane", "Doe" ), reference.getArguments() );
		assertUnmodifiableArguments( reference );
	}

	@Test
	@WithClasses({ Person.class, PersonRepository.class })
	void nullableQueryArguments() throws ReflectiveOperationException {
		final var search = getMethodFromMetamodelFor( PersonRepository.class, "search", String.class, String.class );
		for ( var arguments : new Object[][] { { null, "Doe" }, { "Jane", null }, { null, null } } ) {
			final var reference = (TypedQueryReference<?>) search.invoke( null, arguments );
			assertEquals( Arrays.asList( arguments ), reference.getArguments() );
			assertUnmodifiableArguments( reference );
		}
	}

	@Test
	@WithClasses({ Person.class, PersonRepository.class })
	void nullableStatementArguments() throws ReflectiveOperationException {
		final var rename = getMethodFromMetamodelFor( PersonRepository.class, "rename", String.class, String.class );
		for ( var arguments : new Object[][] { { "Jane", "Doe" }, { null, "Doe" }, { "Jane", null }, { null, null } } ) {
			final var reference = (StatementReference) rename.invoke( null, arguments );
			assertEquals( List.of( "firstName", "lastName" ), reference.getParameterNames() );
			assertEquals( Arrays.asList( arguments ), reference.getArguments() );
			assertUnmodifiableArguments( reference );
		}
	}

	@Test
	@WithClasses({ Person.class, PersonRepository.class })
	void arrayQueryArgument() throws ReflectiveOperationException {
		final var search = getMethodFromMetamodelFor( PersonRepository.class, "searchByAliases", String[].class );
		final var aliases = new String[] { "Jane", "John" };
		final var reference = (TypedQueryReference<?>) search.invoke( null, (Object) aliases );
		assertEquals( 1, reference.getArguments().size() );
		assertSame( aliases, reference.getArguments().get( 0 ) );
		assertUnmodifiableArguments( reference );

		final var nullReference = (TypedQueryReference<?>) search.invoke( null, (Object) null );
		assertEquals( 1, nullReference.getArguments().size() );
		assertNull( nullReference.getArguments().get( 0 ) );
		assertUnmodifiableArguments( nullReference );
	}

	@Test
	@WithClasses({ Person.class, PersonRepository.class })
	void noQueryArguments() throws ReflectiveOperationException {
		final var all = getMethodFromMetamodelFor( PersonRepository.class, "all" );
		final var reference = (TypedQueryReference<?>) all.invoke( null );
		assertEquals( List.of(), reference.getArguments() );
		assertThrows( UnsupportedOperationException.class, () -> reference.getArguments().add( "extra" ) );
	}

	private static void assertUnmodifiableArguments(Reference reference) {
		assertThrows( UnsupportedOperationException.class, () -> reference.getArguments().set( 0, "changed" ) );
		assertThrows( UnsupportedOperationException.class, () -> reference.getArguments().add( "extra" ) );
	}
}

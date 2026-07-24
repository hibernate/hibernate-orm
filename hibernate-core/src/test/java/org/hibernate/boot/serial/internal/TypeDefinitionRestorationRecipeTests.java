/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.boot.serial.internal;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Map;

import org.hibernate.boot.model.TypeDefinition;
import org.hibernate.boot.registry.classloading.spi.ClassLoaderService;
import org.hibernate.boot.registry.classloading.spi.ClassLoadingException;
import org.hibernate.usertype.UserType;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TypeDefinitionRestorationRecipeTests {
	@Test
	void implementorClassIsResolvedFromConsumerEnvironment() throws Exception {
		final var recipe = serializedRecipe();
		final ClassLoaderService classLoaderService = mock( ClassLoaderService.class );
		when( classLoaderService.classForTypeName( StringUserType.class.getName() ) )
				.thenReturn( (Class) StringUserType.class );

		final TypeDefinition restored = recipe.resolve( classLoaderService );

		assertThat( restored.getName() ).isEqualTo( "string-user-type" );
		assertThat( restored.getTypeImplementorClass() ).isEqualTo( StringUserType.class );
		assertThat( restored.getRegistrationKeys() ).containsExactly( "string-user-type", "alternate-name" );
		assertThat( restored.getParameters() ).containsEntry( "format", "compact" );
	}

	@Test
	void recipeDefensivelyCopiesDefinitionState() {
		final var registrationKeys = new String[] { "string-user-type" };
		final var parameters = new java.util.HashMap<String, String>();
		parameters.put( "format", "compact" );
		final var recipe = TypeDefinitionRestorationRecipe.from( new TypeDefinition(
				"string-user-type",
				StringUserType.class,
				registrationKeys,
				parameters
		) );

		registrationKeys[0] = "changed";
		parameters.put( "format", "expanded" );
		recipe.registrationKeys()[0] = "also-changed";

		assertThat( recipe.registrationKeys() ).containsExactly( "string-user-type" );
		assertThat( recipe.parameters() ).containsEntry( "format", "compact" );
	}

	@Test
	void missingImplementorClassReportsNamedDefinition() throws Exception {
		final var recipe = serializedRecipe();
		final ClassLoaderService classLoaderService = mock( ClassLoaderService.class );
		when( classLoaderService.classForTypeName( StringUserType.class.getName() ) )
				.thenThrow( new ClassLoadingException( "missing" ) );

		assertThatThrownBy( () -> recipe.resolve( classLoaderService ) )
				.isInstanceOf( IllegalStateException.class )
				.hasMessageContaining( "type definition 'string-user-type'" )
				.hasMessageContaining( StringUserType.class.getName() );
	}

	private static TypeDefinitionRestorationRecipe serializedRecipe() throws Exception {
		final var definition = new TypeDefinition(
				"string-user-type",
				StringUserType.class,
				new String[] { "string-user-type", "alternate-name" },
				Map.of( "format", "compact" )
		);
		final var bytes = new ByteArrayOutputStream();
		try ( var output = new ObjectOutputStream( bytes ) ) {
			output.writeObject( TypeDefinitionRestorationRecipe.from( definition ) );
		}
		try ( var input = new ObjectInputStream( new ByteArrayInputStream( bytes.toByteArray() ) ) ) {
			return (TypeDefinitionRestorationRecipe) input.readObject();
		}
	}

	private abstract static class StringUserType implements UserType<String> {
	}
}

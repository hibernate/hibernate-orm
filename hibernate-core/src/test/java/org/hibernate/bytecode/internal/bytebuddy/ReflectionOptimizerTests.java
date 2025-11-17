/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.internal.bytebuddy;

import org.hibernate.bytecode.spi.ReflectionOptimizer;
import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Steve Ebersole
 */
public class ReflectionOptimizerTests {

	@Test
	public void generateFastClassAndReflectionOptimizer() {
		BytecodeProviderImpl bytecodeProvider = new BytecodeProviderImpl();
		ReflectionOptimizer reflectionOptimizer = bytecodeProvider.getReflectionOptimizer( SimpleEntity.class,
				new String[]{ "getId", "getName" }, new String[]{ "setId", "setName" },
				new Class<?>[]{ Long.class, String.class } );
		assertThat( reflectionOptimizer ).isNotNull();

		final ReflectionOptimizer.AccessOptimizer accessOptimizer = reflectionOptimizer.getAccessOptimizer();
		assertThat( accessOptimizer.getPropertyNames() ).hasSize( 2 );

		final Object instance = reflectionOptimizer.getInstantiationOptimizer().newInstance();
		assertNotNull( instance );

		final Object[] initialValues = accessOptimizer.getPropertyValues( instance );
		assertThat( initialValues ).containsExactly( null, null );

		accessOptimizer.setPropertyValues( instance, new Object[] { 1L, "a name" } );

		final Object[] injectedValues = accessOptimizer.getPropertyValues( instance );
		assertThat( injectedValues ).containsExactly( 1L, "a name" );
	}

	@Test
	@JiraKey("HHH-16772")
	public void generateFastMappedSuperclassAndReflectionOptimizer() {
		BytecodeProviderImpl bytecodeProvider  = new BytecodeProviderImpl();
		ReflectionOptimizer reflectionOptimizer = bytecodeProvider.getReflectionOptimizer(
				MappedSuperclassEntity.class,
				new String[]{ "getId", "getValue" }, new String[]{ "setId", "setValue" },
				new Class<?>[]{ Long.class, String.class }
		);
		assertThat( reflectionOptimizer ).isNotNull();

		final ReflectionOptimizer.AccessOptimizer accessOptimizer = reflectionOptimizer.getAccessOptimizer();
		assertThat( accessOptimizer.getPropertyNames() ).hasSize( 2 );

		final Object instance = reflectionOptimizer.getInstantiationOptimizer().newInstance();
		assertNotNull( instance );

		final Object[] initialValues = accessOptimizer.getPropertyValues( instance );
		assertThat( initialValues ).containsExactly( null, null );

		accessOptimizer.setPropertyValues( instance, new Object[] { 1L, "a value" } );

		final Object[] injectedValues = accessOptimizer.getPropertyValues( instance );
		assertThat( injectedValues ).containsExactly( 1L, "a value" );
	}
}

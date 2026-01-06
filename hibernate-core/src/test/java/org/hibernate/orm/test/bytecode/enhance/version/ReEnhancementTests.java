/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhance.version;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.dynamic.ClassFileLocator;
import net.bytebuddy.dynamic.ClassFileLocator.Compound;
import net.bytebuddy.dynamic.ClassFileLocator.ForClassLoader;
import net.bytebuddy.pool.TypePool;
import org.hibernate.bytecode.enhance.VersionMismatchException;
import org.hibernate.bytecode.enhance.internal.bytebuddy.EnhancerImpl;
import org.hibernate.bytecode.enhance.internal.bytebuddy.FeatureMismatchException;
import org.hibernate.bytecode.enhance.spi.DefaultEnhancementContext;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.bytecode.internal.bytebuddy.ByteBuddyState;
import org.hibernate.bytecode.spi.ByteCodeHelper;
import org.hibernate.engine.spi.Managed;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
public class ReEnhancementTests {
	@Test
	void testVersionMismatch() {
		final DefaultEnhancementContext enhancementContext = new DefaultEnhancementContext();
		final ByteBuddyState byteBuddyState = new ByteBuddyState();
		final Enhancer enhancer = new EnhancerImpl( enhancementContext, byteBuddyState );

		try {
			attemptEnhancement( SimpleEntity.class, enhancer );
			fail( "Expecting a VersionMismatchException" );
		}
		catch (VersionMismatchException expected) {
			// expected
		}
	}

	@Test
	void testDirtyCheckingSettingMismatch() {
		final DefaultEnhancementContext enhancementContext = new DefaultEnhancementContext() {
			@Override
			public boolean doDirtyCheckingInline() {
				return true;
			}

			@Override
			public boolean doBiDirectionalAssociationManagement() {
				return false;
			}
		};
		final ByteBuddyState byteBuddyState = new ByteBuddyState();
		final Enhancer enhancer = new EnhancerImpl( enhancementContext, byteBuddyState );

		try {
			attemptEnhancement( SimpleEntity2.class, enhancer );
			fail( "Expecting a FeatureMismatchException" );
		}
		catch (FeatureMismatchException expected) {
			// expected
			assertThat( expected.getMessage() ).contains( "inline dirty checking" );
			assertThat( expected.getMessage() ).endsWith( "was previously enhanced with that support disabled." );
		}
	}

	@Test
	void testAssociationManagementSettingMismatch() {
		final DefaultEnhancementContext enhancementContext = new DefaultEnhancementContext() {
			@Override
			public boolean doDirtyCheckingInline() {
				return false;
			}

			@Override
			public boolean doBiDirectionalAssociationManagement() {
				return true;
			}
		};
		final ByteBuddyState byteBuddyState = new ByteBuddyState();
		final Enhancer enhancer = new EnhancerImpl( enhancementContext, byteBuddyState );

		try {
			attemptEnhancement( SimpleEntity2.class, enhancer );
			fail( "Expecting a FeatureMismatchException" );
		}
		catch (FeatureMismatchException expected) {
			// expected
			assertThat( expected.getMessage() ).contains( "bidirectional association management" );
			assertThat( expected.getMessage() ).endsWith( "was previously enhanced with that support disabled." );
		}
	}

	@ParameterizedTest
	@ValueSource(classes = {MappedSuper.class, EntityClass.class})
	void testAlreadyEnhancedEntitiesShouldNotGetEnhancedAgain(Class<?> testClass) {
		final var enhancementContext = new DefaultEnhancementContext();
		final var byteBuddyState = new ByteBuddyState();
		final var enhancer = new EnhancerImpl( enhancementContext, byteBuddyState );

		final var firstRoundEnhancement = attemptEnhancement( testClass, enhancer );
		final var secondRoundEnhancement = enhancer.enhance( testClass.getName(), firstRoundEnhancement );

		// Enhancer returns null if it decides that the class is already enhanced.
		assertThat( secondRoundEnhancement ).isNull();

		try (final var enhancedClassLocator = ClassFileLocator.Simple.of( testClass.getName(), firstRoundEnhancement );
			final var classLoaderLocator = ForClassLoader.of( ReEnhancementTests.class.getClassLoader() );
			final var locator = new Compound( enhancedClassLocator, classLoaderLocator )) {

			final var typePool = TypePool.Default.of( locator );
			final var typeDescription = typePool.describe( testClass.getName() ).resolve();

			assertThat( typeDescription.getInterfaces().stream()
					.map( TypeDefinition::asErasure ) )
					.anySatisfy( it -> it.isAssignableFrom( Managed.class ) );
		}
		catch (IOException e) {
			throw new UncheckedIOException( e );
		}
	}

	private byte[] attemptEnhancement(Class<?> clazz, Enhancer enhancer) {
		final String classFileName = clazz.getName().replace( '.', '/' ) + ".class";
		try (InputStream classFileStream = clazz.getClassLoader().getResourceAsStream( classFileName ) ) {
			final byte[] originalBytes = ByteCodeHelper.readByteCode( classFileStream );
			return enhancer.enhance( clazz.getName(), originalBytes );
		}
		catch (IOException e) {
			throw new RuntimeException( e );
		}
	}

	@MappedSuperclass
	static abstract class MappedSuper {
		@Id
		private Long id;
	}

	@Entity
	static class EntityClass {
		@Id
		private Long id;
	}
}

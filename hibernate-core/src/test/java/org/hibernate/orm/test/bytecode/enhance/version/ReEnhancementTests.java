/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhance.version;

import org.hibernate.bytecode.enhance.VersionMismatchException;
import org.hibernate.bytecode.enhance.internal.bytebuddy.EnhancerImpl;
import org.hibernate.bytecode.enhance.internal.bytebuddy.FeatureMismatchException;
import org.hibernate.bytecode.enhance.spi.DefaultEnhancementContext;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.bytecode.internal.bytebuddy.ByteBuddyState;
import org.hibernate.bytecode.spi.ByteCodeHelper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

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

	private void attemptEnhancement(Class<?> clazz, Enhancer enhancer) {
		final String classFileName = clazz.getName().replace( '.', '/' ) + ".class";
		try (InputStream classFileStream = clazz.getClassLoader().getResourceAsStream( classFileName ) ) {
			final byte[] originalBytes = ByteCodeHelper.readByteCode( classFileStream );
			enhancer.enhance( clazz.getName(), originalBytes );
		}
		catch (IOException e) {
			throw new RuntimeException( e );
		}
	}


}

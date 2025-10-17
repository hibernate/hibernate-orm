/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.internal.bytebuddy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.hibernate.bytecode.enhance.internal.bytebuddy.EnhancerImpl;
import org.hibernate.bytecode.enhance.spi.DefaultEnhancementContext;
import org.hibernate.bytecode.enhance.spi.Enhancer;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests that bytecode can be enhanced when the original class cannot be loaded from
 * the ClassLoader provided to ByteBuddy.
 */
public class EnhanceByteCodeNotInProvidedClassLoaderTest {

	@Test
	@JiraKey( value = "HHH-13343" )
	public void test() {
		Enhancer enhancer = createByteBuddyEnhancer();
		byte[] buffer = readResource( SimpleEntity.class );
		// Now use a fake class name so it won't be found in the ClassLoader
		// provided by DefaultEnhancementContext
		byte[] enhanced = enhancer.enhance( SimpleEntity.class.getName() + "Fake", buffer );
		Assertions.assertNotNull( enhanced, "This is null when there have been swallowed exceptions during enhancement. Check Logs!" );
		// Make sure enhanced bytecode is different from original bytecode.
		Assertions.assertFalse( Arrays.equals( buffer, enhanced ) );
	}

	private byte[] readResource(Class<?> clazz) {
		String internalName = clazz.getName().replace( '.', '/' );
		String resourceName = internalName + ".class";

		final int BUF_SIZE = 256;
		byte[] buffer = new byte[BUF_SIZE];
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		int readSize = 0;
		try ( InputStream inputStream = this.getClass().getClassLoader().getResourceAsStream( resourceName ) ) {
			while ( ( readSize = inputStream.read( buffer ) ) != -1 ) {
				os.write( buffer, 0, readSize );
			}
			os.flush();
			os.close();
		}
		catch (IOException ex) {
			Assertions.fail( "Should not have an IOException here" );
		}
		return os.toByteArray();
	}

	private Enhancer createByteBuddyEnhancer() {
		ByteBuddyState bytebuddy = new ByteBuddyState();
		DefaultEnhancementContext enhancementContext = new DefaultEnhancementContext();
		EnhancerImpl impl = new EnhancerImpl( enhancementContext, bytebuddy );
		return impl;
	}
}

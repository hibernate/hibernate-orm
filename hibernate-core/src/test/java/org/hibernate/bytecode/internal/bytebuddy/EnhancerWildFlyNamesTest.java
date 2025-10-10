/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.internal.bytebuddy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.hibernate.bytecode.enhance.internal.bytebuddy.EnhancerImpl;
import org.hibernate.bytecode.enhance.spi.DefaultEnhancementContext;
import org.hibernate.bytecode.enhance.spi.Enhancer;

import org.hibernate.testing.orm.junit.JiraKey;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * WildFly will use class names in "internal JVM format" when invoking the enhancer,
 * meaning the package separator is '/' rather than '.'.
 * We need to make sure this is handled.
 */
public class EnhancerWildFlyNamesTest {

	@Test
	@JiraKey( value = "HHH-12545" )
	public void test() {
		Enhancer enhancer = createByteBuddyEnhancer();
		String internalName = SimpleEntity.class.getName().replace( '.', '/' );
		String resourceName = internalName + ".class";
		byte[] buffer = new byte[0];
		try {
			buffer = readResource( resourceName );
		}
		catch (IOException e) {
			Assertions.fail( "Should not have an IOException here" );
		}
		byte[] enhanced = enhancer.enhance( internalName, buffer );
		Assertions.assertNotNull( enhanced, "This is null when there have been swallowed exceptions during enhancement. Check Logs!" );
	}

	private byte[] readResource(String resourceName) throws IOException {
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
		return os.toByteArray();
	}

	private Enhancer createByteBuddyEnhancer() {
		ByteBuddyState bytebuddy = new ByteBuddyState();
		DefaultEnhancementContext enhancementContext = new DefaultEnhancementContext();
		EnhancerImpl impl = new EnhancerImpl( enhancementContext, bytebuddy );
		return impl;
	}

}

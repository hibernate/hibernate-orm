/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.orm.test.bytecode.enhance.version;

import java.io.IOException;
import java.io.InputStream;

import org.hibernate.bytecode.enhance.VersionMismatchException;
import org.hibernate.bytecode.enhance.internal.bytebuddy.EnhancerImpl;
import org.hibernate.bytecode.enhance.spi.DefaultEnhancementContext;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.bytecode.internal.bytebuddy.ByteBuddyState;
import org.hibernate.bytecode.spi.ByteCodeHelper;

import org.hibernate.testing.orm.junit.Jira;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.fail;

/**
 * @author Steve Ebersole
 */
@Jira( "https://hibernate.atlassian.net/browse/HHH-16529" )
public class VersionMismatchTests {
	@Test
	void testVersionMismatch() throws IOException {
		final DefaultEnhancementContext enhancementContext = new DefaultEnhancementContext();
		final ByteBuddyState byteBuddyState = new ByteBuddyState();
		final Enhancer enhancer = new EnhancerImpl( enhancementContext, byteBuddyState );

		final String classFile = SimpleEntity.class.getName().replace( '.', '/' ) + ".class";
		try (InputStream classFileStream = SimpleEntity.class.getClassLoader().getResourceAsStream( classFile )) {
			final byte[] originalBytes = ByteCodeHelper.readByteCode( classFileStream );
			enhancer.enhance( SimpleEntity.class.getName(), originalBytes );
			fail( "Expecting failure" );
		}
		catch (VersionMismatchException expected) {
			// expected
		}
	}

}

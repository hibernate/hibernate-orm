/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.internal.bytebuddy;

import org.hibernate.testing.bytecode.enhancement.EnhancementTestConfiguration;
import org.hibernate.bytecode.enhance.spi.EnhancementException;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.bytecode.spi.ByteCodeHelper;
import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * @author Steve Ebersole
 */
public class SimpleEnhancerTests {
	@Test
	public void generateEnhancedClass() throws EnhancementException, IOException {
		Enhancer enhancer = EnhancementTestConfiguration.createEnhancer( new EnhancementTestConfiguration(), new ByteBuddyState() );
		enhancer.enhance( SimpleEntity.class.getName(),
				ByteCodeHelper.readByteCode( SimpleEntity.class.getClassLoader()
						.getResourceAsStream( SimpleEntity.class.getName().replace( '.', '/' ) + ".class" ) ) );
	}
}

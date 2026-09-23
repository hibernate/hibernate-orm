/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.testing.bytecode.enhancement;

import org.hibernate.bytecode.enhance.internal.bytebuddy.ByteBuddyEnhancementSession;
import org.hibernate.bytecode.enhance.spi.DefaultEnhancementModel;
import org.hibernate.bytecode.enhance.spi.EnhancementEnvironment;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.bytecode.internal.bytebuddy.ByteBuddyState;
import org.hibernate.bytecode.spi.BytecodeProvider;

/// Test fixture allowing one subclass to customize independent model and option inputs.
/// Production integration supplies these contracts separately.
///
/// @author Steve Ebersole
@org.hibernate.SPI
public class EnhancementTestConfiguration extends DefaultEnhancementModel
		implements org.hibernate.bytecode.enhance.spi.EnhancementOptions {
	public ClassLoader getLoadingClassLoader() { return getClass().getClassLoader(); }
	@Override
	public boolean doBiDirectionalAssociationManagement(org.hibernate.bytecode.enhance.spi.UnloadedField field) {
		return true;
	}
	public Enhancer createEnhancer(BytecodeProvider provider) {
		return provider.createEnhancementSession(this, EnhancementEnvironment.forClassLoader(getLoadingClassLoader()))
				.createEnhancer(this);
	}
	@org.hibernate.Internal
	public static Enhancer createEnhancer(EnhancementTestConfiguration configuration, ByteBuddyState state) {
		return new ByteBuddyEnhancementSession(configuration,
				EnhancementEnvironment.forClassLoader(configuration.getLoadingClassLoader()), state)
				.createEnhancer(configuration);
	}
}

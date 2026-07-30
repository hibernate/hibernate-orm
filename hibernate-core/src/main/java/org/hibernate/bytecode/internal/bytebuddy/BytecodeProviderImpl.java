/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.bytecode.internal.bytebuddy;

import java.lang.reflect.Member;

import jakarta.annotation.Nonnull;
import org.hibernate.bytecode.enhance.internal.bytebuddy.EnhancerClassLocator;
import org.hibernate.bytecode.enhance.internal.bytebuddy.EnhancerImpl;
import org.hibernate.bytecode.enhance.internal.bytebuddy.EnhancerImplConstants;
import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.bytecode.spi.ProxyFactoryFactory;
import org.hibernate.proxy.pojo.bytebuddy.ByteBuddyProxyHelper;

import net.bytebuddy.ClassFileVersion;
import jakarta.annotation.Nullable;

public class BytecodeProviderImpl implements BytecodeProvider {

	/**
	 * Sentinel member used to represent embedded (component) properties
	 * in bulk accessor arrays.
	 */
	public static final Member EMBEDDED_MEMBER = new Member() {
		@Override
		public Class<?> getDeclaringClass() {
			return null;
		}

		@Override
		public String getName() {
			return null;
		}

		@Override
		public int getModifiers() {
			return 0;
		}

		@Override
		public boolean isSynthetic() {
			return false;
		}
	};

	private final ByteBuddyState byteBuddyState;
	private final EnhancerImplConstants constants;

	private final ByteBuddyProxyHelper byteBuddyProxyHelper;

	/**
	 * Constructs a ByteBuddy BytecodeProvider instance which attempts to auto-detect the target JVM version
	 * from the currently running one, with a fallback on Java 17.
	 */
	public BytecodeProviderImpl() {
		this( ClassFileVersion.ofThisVm( ClassFileVersion.JAVA_V17 ) );
	}

	/**
	 * Constructs a ByteBuddy BytecodeProvider instance which aims to produce code compatible
	 * with the specified target JVM version.
	 */
	public BytecodeProviderImpl(ClassFileVersion targetCompatibleJVM) {
		this.byteBuddyState = new ByteBuddyState( targetCompatibleJVM );
		this.byteBuddyProxyHelper = new ByteBuddyProxyHelper( byteBuddyState );
		this.constants = byteBuddyState.getEnhancerConstants();
	}

	@Nonnull
	@Override
	public ProxyFactoryFactory getProxyFactoryFactory() {
		return new ProxyFactoryFactoryImpl( byteBuddyState, byteBuddyProxyHelper );
	}

	public ByteBuddyProxyHelper getByteBuddyProxyHelper() {
		return byteBuddyProxyHelper;
	}

	@Override
	public @Nullable Enhancer getEnhancer(@Nonnull EnhancementContext enhancementContext) {
		return new EnhancerImpl( enhancementContext, byteBuddyState );
	}

	/**
	 * Similar to {@link #getEnhancer(EnhancementContext)} but intended for advanced users who wish
	 * to customize how ByteBuddy is locating the class files and caching the types.
	 * Used in Quarkus.
	 */
	public @Nullable Enhancer getEnhancer(
			@Nonnull EnhancementContext enhancementContext,
			@Nonnull EnhancerClassLocator classLocator) {
		return new EnhancerImpl( enhancementContext, byteBuddyState, classLocator );
	}

	@Override
	public void resetCaches() {
		byteBuddyState.clearState();
	}

}

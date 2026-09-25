package org.hibernate.bytecode.internal.bytebuddy;

import java.lang.reflect.Member;

import jakarta.annotation.Nonnull;
import org.hibernate.bytecode.enhance.internal.bytebuddy.EnhancerClassLocator;
import org.hibernate.bytecode.enhance.internal.bytebuddy.ByteBuddyEnhancementSession;
import org.hibernate.bytecode.enhance.spi.EnhancementEnvironment;
import org.hibernate.bytecode.enhance.spi.EnhancementModel;
import org.hibernate.bytecode.enhance.spi.EnhancementSession;
import org.hibernate.bytecode.enhance.internal.bytebuddy.EnhancerImplConstants;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.bytecode.spi.ProxyFactoryFactory;
import org.hibernate.proxy.pojo.bytebuddy.ByteBuddyProxyHelper;

import net.bytebuddy.ClassFileVersion;

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
	public EnhancementSession createEnhancementSession(
			EnhancementModel model,
			EnhancementEnvironment environment) {
		return new ByteBuddyEnhancementSession(model, environment, byteBuddyState);
	}

	/// Creates a session with a borrowed custom locator.
	public EnhancementSession createEnhancementSession(
			EnhancementModel model, EnhancerClassLocator locator) {
		return new ByteBuddyEnhancementSession(model, byteBuddyState, locator);
	}

	@Override
	public void resetCaches() {
		byteBuddyState.clearState();
	}

}

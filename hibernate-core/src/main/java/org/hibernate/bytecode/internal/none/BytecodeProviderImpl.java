package org.hibernate.bytecode.internal.none;

import jakarta.annotation.Nonnull;
import org.hibernate.bytecode.enhance.spi.EnhancementEnvironment;
import org.hibernate.bytecode.enhance.spi.EnhancementModel;
import org.hibernate.bytecode.enhance.spi.EnhancementSession;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.bytecode.spi.ProxyFactoryFactory;



/**
 * This BytecodeProvider represents the "no-op" enhancer; mostly useful
 * as an optimisation when not needing any byte code optimisation applied,
 * for example when the entities have been enhanced at compile time.
 * Choosing this BytecodeProvider allows to exclude the bytecode enhancement
 * libraries from the runtime classpath.
 *
 * @since 5.4
 */
public final class BytecodeProviderImpl implements BytecodeProvider {

	@Nonnull
	@Override
	public ProxyFactoryFactory getProxyFactoryFactory() {
		return new NoProxyFactoryFactory();
	}

	@Override
	public EnhancementSession createEnhancementSession(
			EnhancementModel model,
			EnhancementEnvironment environment) {
		throw new UnsupportedOperationException("Bytecode enhancement is disabled");
	}
}

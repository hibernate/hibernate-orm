package org.hibernate.jpa.internal.enhance;

import java.security.ProtectionDomain;
import org.hibernate.bytecode.enhance.spi.EnhancementModel;
import org.hibernate.bytecode.enhance.spi.EnhancementOptions;
import org.hibernate.bytecode.spi.BytecodeProvider;

import jakarta.persistence.spi.ClassTransformer;
import jakarta.persistence.spi.TransformerException;
import org.hibernate.bytecode.enhance.internal.bytebuddy.CorePrefixFilter;

/// Rewrites client instructions without performing managed-class enhancement.
/// Shares defining-loader discovery and metadata with its paired managed transformer.
///
/// @since 8.0
/// @author Steve Ebersole
public final class ClientClassTransformer implements ClassTransformer {

	private final PersistenceUnitEnhancementState state;
	private final BytecodeProvider provider;
	private final Object transformerToken = new Object();
	private final EnhancementOptions options = EnhancementOptions.of(false, false, false);

	public ClientClassTransformer(EnhancementModel model, BytecodeProvider provider, ClassLoader temporaryLoader) {
		this(new PersistenceUnitEnhancementState(model), provider);
		state.discoverTemporaryTypes(temporaryLoader, this.provider);
	}

	public ClientClassTransformer(PersistenceUnitEnhancementState state, BytecodeProvider provider) {
		this.state = state;
		this.provider = state.resolveProvider(provider);
	}

	@Override
	public byte[] transform(
			ClassLoader loader,
			String className,
			Class<?> classBeingRedefined,
			ProtectionDomain protectionDomain,
			byte[] classfileBuffer) throws TransformerException {
		if ( loader == null || CorePrefixFilter.DEFAULT_INSTANCE.isCoreClassName( className.replace( '/', '.' ) ) ) {
			return null;
		}
		try {
			return state.transform(loader, provider, transformerToken, options, true, className, classfileBuffer);
		}
		catch (RuntimeException e) {
			throw new TransformerException( "Error performing client enhancement of " + className, e );
		}
	}

}

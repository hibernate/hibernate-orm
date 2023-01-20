/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.internal.enhance;

import java.security.ProtectionDomain;
import java.util.Objects;

import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.enhance.spi.EnhancementContextWrapper;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.bytecode.internal.BytecodeProviderInitiator;
import org.hibernate.bytecode.spi.BytecodeProvider;
import org.hibernate.bytecode.spi.ClassTransformer;
import org.hibernate.cfg.Environment;

import jakarta.persistence.spi.TransformerException;

/**
 * @author Steve Ebersole
 * @author Luis Barreiro
 */
public class EnhancingClassTransformerImpl implements ClassTransformer {

	private final EnhancementContext enhancementContext;
	private final BytecodeProvider bytecodeProvider;

	public EnhancingClassTransformerImpl(EnhancementContext enhancementContext) {
		Objects.requireNonNull( enhancementContext );
		this.enhancementContext = enhancementContext;
		this.bytecodeProvider = BytecodeProviderInitiator.buildDefaultBytecodeProvider();
	}

	@Override
	public byte[] transform(
			ClassLoader loader,
			String className,
			Class<?> classBeingRedefined,
			ProtectionDomain protectionDomain,
			byte[] classfileBuffer)  throws TransformerException {

		// The first design had the enhancer as a class variable. That approach had some goods and bads.
		// We don't have to create an enhancer for each class, but on the other end it would stay in memory forever.
		// It also assumed that all calls come from the same class loader, which is fair, but this makes it more robust.

		try {
			Enhancer enhancer = bytecodeProvider.getEnhancer( new EnhancementContextWrapper( enhancementContext, loader ) );
			return enhancer.enhance( className, classfileBuffer );
		}
		catch (final Exception e) {
			throw new TransformerException( "Error performing enhancement of " + className, e );
		}
		finally {
			bytecodeProvider.resetCaches();
		}
	}

}

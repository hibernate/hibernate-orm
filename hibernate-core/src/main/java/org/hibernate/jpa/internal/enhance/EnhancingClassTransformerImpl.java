/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.internal.enhance;

import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.enhance.spi.EnhancementContextWrapper;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.bytecode.spi.ClassTransformer;
import org.hibernate.cfg.Environment;

/**
 * @author Steve Ebersole
 * @author Luis Barreiro
 */
public class EnhancingClassTransformerImpl implements ClassTransformer {

	private final EnhancementContext enhancementContext;

	public EnhancingClassTransformerImpl(EnhancementContext enhancementContext) {
		this.enhancementContext = enhancementContext;
	}

	@Override
	public byte[] transform(
			ClassLoader loader,
			String className,
			Class<?> classBeingRedefined,
			ProtectionDomain protectionDomain,
			byte[] classfileBuffer) throws IllegalClassFormatException {

		// The first design had the enhancer as a class variable. That approach had some goods and bads.
		// We don't had to create an enhancer for each class, but on the other end it would stay in memory forever.
		// It also assumed that all calls come from the same class loader, which is fair, but this makes it more robust.

		try {
			Enhancer enhancer = Environment.getBytecodeProvider().getEnhancer( new EnhancementContextWrapper( enhancementContext, loader ) );
			return enhancer.enhance( className, classfileBuffer );
		}
		catch (final Exception e) {
			throw new IllegalClassFormatException( "Error performing enhancement of " + className ) {
				@Override
				public synchronized Throwable getCause() {
					return e;
				}
			};
		}
	}

}

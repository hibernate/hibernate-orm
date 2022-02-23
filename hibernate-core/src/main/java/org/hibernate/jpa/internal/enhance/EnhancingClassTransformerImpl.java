/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.internal.enhance;

import java.lang.instrument.IllegalClassFormatException;
import java.lang.reflect.Constructor;
import java.security.ProtectionDomain;

import org.hibernate.bytecode.enhance.spi.EnhancementContext;
import org.hibernate.bytecode.enhance.spi.EnhancementContextWrapper;
import org.hibernate.bytecode.enhance.spi.Enhancer;
import org.hibernate.bytecode.spi.ClassTransformer;
import org.hibernate.cfg.Environment;
import org.hibernate.internal.util.ExceptionHelper;

/**
 * @author Steve Ebersole
 * @author Luis Barreiro
 */
public class EnhancingClassTransformerImpl implements ClassTransformer {

	private static final Constructor<Exception> TRANSFORMER_CONSTRUCTOR;

	static {
		Constructor<Exception> constructor = null;
		try {
			final Class<?> transformerException = Class.forName( "jakarta.persistence.spi.TransformerException" );
			//noinspection unchecked
			constructor = (Constructor<Exception>) transformerException.getConstructor( String.class, Throwable.class );
		}
		catch (ClassNotFoundException | NoSuchMethodException e) {
			// Ignore
		}
		TRANSFORMER_CONSTRUCTOR = constructor;
	}

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
			byte[] classfileBuffer) {

		// The first design had the enhancer as a class variable. That approach had some goods and bads.
		// We don't have to create an enhancer for each class, but on the other end it would stay in memory forever.
		// It also assumed that all calls come from the same class loader, which is fair, but this makes it more robust.

		try {
			Enhancer enhancer = Environment.getBytecodeProvider().getEnhancer( new EnhancementContextWrapper( enhancementContext, loader ) );
			return enhancer.enhance( className, classfileBuffer );
		}
		catch (final Exception e) {
			Throwable t;
			if ( TRANSFORMER_CONSTRUCTOR == null ) {
				t = new IllegalClassFormatException( "Error performing enhancement of " + className ) {
					@Override
					public synchronized Throwable getCause() {
						return e;
					}
				};
			}
			else {
				try {
					t = TRANSFORMER_CONSTRUCTOR.newInstance( "Error performing enhancement of " + className, e );
				}
				catch (Exception ex) {
					t = new IllegalClassFormatException( "Error performing enhancement of " + className ) {
						@Override
						public synchronized Throwable getCause() {
							return e;
						}
					};
				}
			}
			ExceptionHelper.doThrow( t );
			return null;
		}
	}

}

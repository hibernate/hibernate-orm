/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.internal.enhance;

import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Collection;
import javax.persistence.spi.ClassTransformer;

import org.hibernate.bytecode.enhance.spi.Enhancer;

/**
 * @author Steve Ebersole
 */
public class EnhancingClassTransformerImpl implements ClassTransformer {
	private final Collection<String> classNames;

	private Enhancer enhancer;

	public EnhancingClassTransformerImpl(Collection<String> incomingClassNames) {
		this.classNames = new ArrayList<String>( incomingClassNames.size() );
		this.classNames.addAll( incomingClassNames );
	}

	@Override
	public byte[] transform(
			ClassLoader loader,
			String className,
			Class<?> classBeingRedefined,
			ProtectionDomain protectionDomain,
			byte[] classfileBuffer) throws IllegalClassFormatException {
		if ( enhancer == null ) {
			enhancer = new Enhancer( new EnhancementContextImpl( classNames, loader ) );
		}

		try {
			return enhancer.enhance( className, classfileBuffer );
		}
		catch (final Exception e) {
			throw new IllegalClassFormatException( "Error performing enhancement" ) {
				@Override
				public synchronized Throwable getCause() {
					return e;
				}
			};
		}
	}

}

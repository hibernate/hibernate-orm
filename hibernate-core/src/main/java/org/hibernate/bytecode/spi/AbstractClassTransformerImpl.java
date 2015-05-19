/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.bytecode.spi;

import java.security.ProtectionDomain;

import org.hibernate.bytecode.buildtime.spi.ClassFilter;
import org.hibernate.bytecode.buildtime.spi.FieldFilter;

/**
 * Basic implementation of the {@link ClassTransformer} contract.
 *
 * @author Emmanuel Bernard
 * @author Steve Ebersole
 */
public abstract class AbstractClassTransformerImpl implements ClassTransformer {
	protected final ClassFilter classFilter;
	protected final FieldFilter fieldFilter;

	protected AbstractClassTransformerImpl(ClassFilter classFilter, FieldFilter fieldFilter) {
		this.classFilter = classFilter;
		this.fieldFilter = fieldFilter;
	}

	@Override
	public byte[] transform(
			ClassLoader loader,
			String className,
			Class classBeingRedefined,
			ProtectionDomain protectionDomain,
			byte[] classfileBuffer) {
		// to be safe...
		className = className.replace( '/', '.' );
		if ( classFilter.shouldInstrumentClass( className ) ) {
			return doTransform( loader, className, classBeingRedefined, protectionDomain, classfileBuffer );
		}
		else {
			return classfileBuffer;
		}
	}

	/**
	 * Delegate the transformation call from {@link #transform}
	 *
	 * @param loader The class loader to use
	 * @param className The name of the class to transform
	 * @param classBeingRedefined If an already loaded class is being redefined, then pass this as a parameter
	 * @param protectionDomain The protection domain of the class being (re)defined
	 * @param classfileBuffer The bytes of the class file.
	 *
	 * @return The transformed (enhanced/instrumented) bytes.
	 */
	protected abstract byte[] doTransform(
			ClassLoader loader,
			String className,
			Class classBeingRedefined,
			ProtectionDomain protectionDomain,
			byte[] classfileBuffer);
}

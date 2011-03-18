/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008-2011, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
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

	protected abstract byte[] doTransform(
			ClassLoader loader,
			String className,
			Class classBeingRedefined,
			ProtectionDomain protectionDomain,
			byte[] classfileBuffer);
}

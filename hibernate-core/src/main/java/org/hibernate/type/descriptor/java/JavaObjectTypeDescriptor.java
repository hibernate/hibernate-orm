/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.java;

import org.hibernate.engine.spi.SharedSessionContractImplementor;

/**
 * @author Steve Ebersole
 */
public class JavaObjectTypeDescriptor extends AbstractTypeDescriptor<Object> {
	/**
	 * Singleton access
	 */
	public static final JavaObjectTypeDescriptor INSTANCE = new JavaObjectTypeDescriptor();

	public JavaObjectTypeDescriptor() {
		super( Object.class );
	}

	@Override
	public <X> X unwrap(Object value, Class<X> type, SharedSessionContractImplementor options) {
		return (X) value;
	}

	@Override
	public <X> Object wrap(X value, SharedSessionContractImplementor options) {
		return value;
	}
}

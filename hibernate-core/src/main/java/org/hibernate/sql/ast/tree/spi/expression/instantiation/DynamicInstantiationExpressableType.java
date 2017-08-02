/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression.instantiation;

import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * An ExpressableType specifically for dynamic-instantiations
 *
 * @author Steve Ebersole
 */
public class DynamicInstantiationExpressableType<T> implements ExpressableType<T> {
	private final JavaTypeDescriptor<T> javaTypeDescriptor;

	public DynamicInstantiationExpressableType(JavaTypeDescriptor<T> javaTypeDescriptor) {
		this.javaTypeDescriptor = javaTypeDescriptor;
	}

	@Override
	public JavaTypeDescriptor<T> getJavaTypeDescriptor() {
		return javaTypeDescriptor;
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.BASIC;
	}

	@Override
	public Class<T> getJavaType() {
		return javaTypeDescriptor.getJavaType();
	}
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import org.hibernate.metamodel.model.domain.AnyMappingDomainType;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;

/**
 * @author Steve Ebersole
 */
public class AnyMappingDomainTypeImpl<T> implements AnyMappingDomainType<T> {
	private final JavaTypeDescriptor<T> baseJtd;

	public AnyMappingDomainTypeImpl(JavaTypeDescriptor<T> baseJtd) {
		this.baseJtd = baseJtd;
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.ENTITY;
	}

	@Override
	public Class<T> getJavaType() {
		return baseJtd.getJavaTypeClass();
	}

	@Override
	public JavaTypeDescriptor<T> getExpressableJavaTypeDescriptor() {
		return baseJtd;
	}
}

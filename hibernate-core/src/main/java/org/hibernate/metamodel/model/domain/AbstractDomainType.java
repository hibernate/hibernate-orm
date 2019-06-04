/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain;

import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractDomainType<J> implements SimpleDomainType<J> {
	private final TypeConfiguration typeConfiguration;
	private final JavaTypeDescriptor<J> javaTypeDescriptor;

	@SuppressWarnings("WeakerAccess")
	public AbstractDomainType(JavaTypeDescriptor<J> javaTypeDescriptor, TypeConfiguration typeConfiguration) {
		this.javaTypeDescriptor = javaTypeDescriptor;
		this.typeConfiguration = typeConfiguration;
	}

	protected JpaMetamodel jpaMetamodel() {
		return typeConfiguration.getSessionFactory().getJpaMetamodel();
	}

	@Override
	public JavaTypeDescriptor<J> getExpressableJavaTypeDescriptor() {
		return javaTypeDescriptor;
	}

	@Override
	public Class<J> getJavaType() {
		return getExpressableJavaTypeDescriptor().getJavaType();
	}
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain;

import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractDomainType<J> implements SimpleDomainType<J> {
	private final JpaMetamodel domainMetamodel;
	private final JavaType<J> javaType;

	@SuppressWarnings("WeakerAccess")
	public AbstractDomainType(JavaType<J> javaType, JpaMetamodel domainMetamodel) {
		this.javaType = javaType;
		this.domainMetamodel = domainMetamodel;
	}

	protected JpaMetamodel jpaMetamodel() {
		return domainMetamodel;
	}

	@Override
	public JavaType<J> getExpressableJavaType() {
		return javaType;
	}

	@Override
	public Class<J> getJavaType() {
		return this.getExpressableJavaType().getJavaTypeClass();
	}

	@Override
	public Class<J> getBindableJavaType() {
		return getJavaType();
	}
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.query.sqm.SqmBindableType;
import org.hibernate.query.sqm.tree.domain.SqmDomainType;
import org.hibernate.type.descriptor.java.JavaType;

import static jakarta.persistence.metamodel.Type.PersistenceType.BASIC;

/**
 * @author Steve Ebersole
 */
public class NullSqmExpressible implements SqmBindableType<Object> {
	/**
	 * Singleton access
	 */
	public static final NullSqmExpressible NULL_SQM_EXPRESSIBLE = new NullSqmExpressible();

	@Override
	public PersistenceType getPersistenceType() {
		return BASIC;
	}

	@Override
	public Class<Object> getJavaType() {
		return null;
	}

	@Override
	public JavaType<Object> getExpressibleJavaType() {
		return null;
	}

	@Override
	public SqmDomainType<Object> getSqmType() {
		return null;
	}
}

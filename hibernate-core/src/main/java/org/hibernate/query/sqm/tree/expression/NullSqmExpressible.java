/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.query.sqm.SqmBindable;
import org.hibernate.query.sqm.tree.domain.SqmDomainType;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Steve Ebersole
 */
public class NullSqmExpressible implements SqmBindable<Object> {
	/**
	 * Singleton access
	 */
	public static final NullSqmExpressible NULL_SQM_EXPRESSIBLE = new NullSqmExpressible();

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.BASIC;
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

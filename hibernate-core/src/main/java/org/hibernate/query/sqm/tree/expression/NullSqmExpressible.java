/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.tree.from.SqmDomainType;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Steve Ebersole
 */
public class NullSqmExpressible implements SqmExpressible<Object> {
	/**
	 * Singleton access
	 */
	public static final NullSqmExpressible NULL_SQM_EXPRESSIBLE = new NullSqmExpressible();

	@Override
	public Class<Object> getBindableJavaType() {
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

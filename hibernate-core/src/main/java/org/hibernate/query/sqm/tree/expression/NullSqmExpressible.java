/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.query.sqm.SqmExpressible;
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
	public DomainType<Object> getSqmType() {
		return null;
	}
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain;

import org.hibernate.query.BindableType;

/**
 * Describes any non-collection type
 *
 * @author Steve Ebersole
 */
public interface SimpleDomainType<J>
		extends DomainType<J>, BindableType<J>, jakarta.persistence.metamodel.Type<J> {
	@Override
	default Class<J> getBindableJavaType() {
		return getJavaType();
	}

	@Override
	default Class<J> getJavaType() {
		return getExpressibleJavaType().getJavaTypeClass();
	}
}

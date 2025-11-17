/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain;

import jakarta.persistence.metamodel.Type;

/**
 * Describes any non-collection type.
 *
 * @author Steve Ebersole
 */
public interface SimpleDomainType<J> extends DomainType<J>, Type<J> {
	@Override
	default Class<J> getJavaType() {
		return getExpressibleJavaType().getJavaTypeClass();
	}
}

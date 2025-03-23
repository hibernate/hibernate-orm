/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain;

/**
 * Describes any non-collection type
 *
 * @author Steve Ebersole
 */
public interface SimpleDomainType<J> extends DomainType<J>, jakarta.persistence.metamodel.Type<J> {
	@Override
	default Class<J> getBindableJavaType() {
		return getJavaType();
	}
}

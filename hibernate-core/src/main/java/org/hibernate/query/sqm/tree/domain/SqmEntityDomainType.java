/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.domain;

import org.hibernate.Incubating;
import org.hibernate.metamodel.model.domain.EntityDomainType;

@Incubating
public interface SqmEntityDomainType<E> extends EntityDomainType<E>, SqmTreatableDomainType<E> {
	@Override
	default SqmEntityDomainType<E> getSqmType() {
		return this;
	}
}

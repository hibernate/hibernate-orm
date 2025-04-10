/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.from;

import org.hibernate.metamodel.model.domain.EntityDomainType;

public interface SqmEntityDomainType<E> extends EntityDomainType<E>, SqmTreatableDomainType<E> {
	@Override
	default EntityDomainType<E> getSqmType() {
		return EntityDomainType.super.getSqmType();
	}
}

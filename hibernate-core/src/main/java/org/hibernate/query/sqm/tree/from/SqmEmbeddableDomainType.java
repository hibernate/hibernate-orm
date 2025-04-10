/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.from;

import org.hibernate.metamodel.model.domain.EmbeddableDomainType;

public interface SqmEmbeddableDomainType<E> extends EmbeddableDomainType<E>, SqmTreatableDomainType<E> {
	@Override
	default EmbeddableDomainType<E> getSqmType() {
		return EmbeddableDomainType.super.getSqmType();
	}
}

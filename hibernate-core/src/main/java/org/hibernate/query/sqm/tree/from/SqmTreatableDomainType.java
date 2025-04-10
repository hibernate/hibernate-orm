/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.tree.from;

import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.metamodel.model.domain.TreatableDomainType;
import org.hibernate.query.sqm.SqmPathSource;

public interface SqmTreatableDomainType<T> extends TreatableDomainType<T>, SqmPathSource<T> {
	@Override
	DomainType<T> getSqmType();
}

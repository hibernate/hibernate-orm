/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain;

import org.hibernate.query.sqm.SqmPathSource;

/**
 * @author Marco Belladelli
 */
public interface TreatableDomainType<J> extends ManagedDomainType<J>, SqmPathSource<J> {
	@Override
	default DomainType<J> getSqmType() {
		return this;
	}
}

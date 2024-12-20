/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain;

import org.hibernate.query.sqm.SqmPathSource;

import jakarta.persistence.metamodel.MappedSuperclassType;

/**
 * Extension of the JPA {@link MappedSuperclassType} contract
 *
 * @author Steve Ebersole
 */
public interface MappedSuperclassDomainType<J> extends IdentifiableDomainType<J>, MappedSuperclassType<J>, SqmPathSource<J> {
	@Override
	default DomainType<J> getSqmType() {
		return IdentifiableDomainType.super.getSqmType();
	}
}

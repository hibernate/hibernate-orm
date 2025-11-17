/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain;


import jakarta.persistence.metamodel.MappedSuperclassType;

/**
 * Extension of the JPA {@link MappedSuperclassType} contract
 *
 * @author Steve Ebersole
 */
public interface MappedSuperclassDomainType<J>
		extends IdentifiableDomainType<J>, MappedSuperclassType<J>, PathSource<J> {
}

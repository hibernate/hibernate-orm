/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain;


/**
 * Models Hibernate's ANY mapping (reverse discrimination) as a JPA domain model type
 *
 * @param <J> The base Java type defined for the {@code any} mapping
 *
 * @author Steve Ebersole
 */
public interface AnyMappingDomainType<J> extends SimpleDomainType<J> {
	SimpleDomainType<?> getDiscriminatorType();
	SimpleDomainType<?> getKeyType();
}

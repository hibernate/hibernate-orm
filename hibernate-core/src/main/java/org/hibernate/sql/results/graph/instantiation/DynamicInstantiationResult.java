/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph.instantiation;

import org.hibernate.sql.results.graph.DomainResult;

/**
 * Specialization of DomainResult to model
 * {@linkplain jakarta.persistence.ConstructorResult dynamic instantiation}
 *
 * @author Steve Ebersole
 */
@org.hibernate.SPI({ org.hibernate.SPI.Role.USE, org.hibernate.SPI.Role.IMPLEMENT })
public interface DynamicInstantiationResult<R> extends DomainResult<R> {
}

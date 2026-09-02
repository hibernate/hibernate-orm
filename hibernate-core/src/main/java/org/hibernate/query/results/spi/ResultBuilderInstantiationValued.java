/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.spi;

/// Builds a dynamic-instantiation result for a result-set mapping.
///
/// Supply an implementation through
/// [ResultSetMapping#addResultBuilder(ResultBuilder)].
///
/// @see jakarta.persistence.ConstructorResult
/// @see ResultSetMapping#addResultBuilder(ResultBuilder)
///
/// @author Steve Ebersole
@org.hibernate.SPI({ org.hibernate.SPI.Role.USE, org.hibernate.SPI.Role.IMPLEMENT, org.hibernate.SPI.Role.SUPPLY })
public interface ResultBuilderInstantiationValued extends ResultBuilder {
}

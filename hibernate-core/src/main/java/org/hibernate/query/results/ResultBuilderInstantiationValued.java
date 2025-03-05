/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results;

/**
 * ResultBuilder specialization for cases involving dynamic-instantiation results.
 *
 * @see jakarta.persistence.ConstructorResult
 *
 * @author Steve Ebersole
 */
public interface ResultBuilderInstantiationValued extends ResultBuilder {
}

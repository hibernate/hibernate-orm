/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
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

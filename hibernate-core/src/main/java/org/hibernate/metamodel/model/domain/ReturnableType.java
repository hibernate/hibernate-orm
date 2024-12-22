/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.model.domain;

import org.hibernate.Incubating;

/**
 * Specialization of {@link SimpleDomainType} for types that can
 * be used as function returns.
 *
 * @author Steve Ebersole
 */
@Incubating
public interface ReturnableType<T> extends SimpleDomainType<T> {
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.named.internal;

import org.hibernate.query.named.NamedMutationMemento;
import org.hibernate.query.named.NamedSqmQueryMemento;

/// Union of `NamedSqmQueryMemento` and `NamedMutationMemento`.
///
/// @author Steve Ebersole
public interface SqmMutationMemento<T> extends NamedSqmQueryMemento<T>, NamedMutationMemento<T> {
}

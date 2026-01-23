/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.named.internal;

import org.hibernate.query.named.NamedSelectionMemento;
import org.hibernate.query.named.NamedSqmQueryMemento;

/// Union of `NamedSqmQueryMemento` and `NamedSelectionMemento`
///
/// @author Steve Ebersole
public interface SqmSelectionMemento<T> extends NamedSqmQueryMemento<T>, NamedSelectionMemento<T> {
}

/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.named;

import org.hibernate.query.spi.JpaStatementReference;

/**
 * @author Steve Ebersole
 */
public interface NamedMutationMemento<T> extends NamedQueryMemento<T>, JpaStatementReference<T> {
}

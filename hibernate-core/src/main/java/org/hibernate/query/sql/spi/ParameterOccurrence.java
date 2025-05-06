/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sql.spi;

import org.hibernate.query.spi.QueryParameterImplementor;

/**
 * @author Christian Beikov
 */
public record ParameterOccurrence(QueryParameterImplementor<?> parameter, int sourcePosition) {
}

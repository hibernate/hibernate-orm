/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.spi;

import org.hibernate.Incubating;
import org.hibernate.ScrollableResults;

/**
 * @author Steve Ebersole
 *
 * @since 5.2
 */
@Incubating
public interface ScrollableResultsImplementor<R> extends ScrollableResults<R> {
}

/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.spi;

import java.util.Iterator;

import org.hibernate.Incubating;

/**
 * Unification of Iterator and AutoCloseable
 *
 * @author Steve Ebersole
 *
 * @since 5.2
 */
@Incubating
public interface CloseableIterator<T> extends Iterator<T>, AutoCloseable {
	@Override
	void close();
}

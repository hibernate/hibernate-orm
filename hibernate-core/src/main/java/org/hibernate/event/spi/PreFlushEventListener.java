/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import org.hibernate.HibernateException;
import org.hibernate.Incubating;

/**
 * A listener for events of type {@link PreFlushEvent}.
 *
 * @author Gavin King
 * @since 7.2
 */
@Incubating
public interface PreFlushEventListener {
	void onAutoPreFlush(PreFlushEvent event) throws HibernateException;
}

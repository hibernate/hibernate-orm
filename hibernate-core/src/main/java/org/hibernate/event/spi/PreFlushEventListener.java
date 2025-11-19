/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import org.hibernate.HibernateException;

/**
 * @author Gavin King
 * @since 7.2
 */
public interface PreFlushEventListener {
	void onAutoPreFlush(AutoFlushEvent event) throws HibernateException;
}

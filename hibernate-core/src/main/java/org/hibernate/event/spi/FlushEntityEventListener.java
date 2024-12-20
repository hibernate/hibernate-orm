/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import org.hibernate.HibernateException;

/**
 * @author Gavin King
 */
public interface FlushEntityEventListener {
	void onFlushEntity(FlushEntityEvent event) throws HibernateException;
}

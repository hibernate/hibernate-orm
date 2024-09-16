/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import org.hibernate.HibernateException;

/**
 * Defines the contract for handling of resolve natural id events generated from a session.
 *
 * @author Eric Dalquist
 * @author Steve Ebersole
 */
public interface ResolveNaturalIdEventListener {

	/**
	 * Handle the given resolve natural id event.
	 *
	 * @param event The resolve natural id event to be handled.
	 *
	 * @throws HibernateException Indicates a problem resolving natural id to primary key
	 */
	void onResolveNaturalId(ResolveNaturalIdEvent event) throws HibernateException;

}

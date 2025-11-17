/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import org.hibernate.HibernateException;

/**
 * Defines the contract for handling of replicate events generated from a session.
 *
 * @author Steve Ebersole
 *
 * @deprecated since {@link org.hibernate.Session#replicate} is deprecated
 */
@Deprecated(since="6")
public interface ReplicateEventListener {

	/** Handle the given replicate event.
	 *
	 * @param event The replicate event to be handled.
	 */
	void onReplicate(ReplicateEvent event) throws HibernateException;

}

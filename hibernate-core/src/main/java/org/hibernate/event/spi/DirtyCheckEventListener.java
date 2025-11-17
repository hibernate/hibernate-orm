/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.event.spi;

import org.hibernate.HibernateException;

/**
 * Defines the contract for handling of session dirty-check events.
 *
 * @author Steve Ebersole
 */
public interface DirtyCheckEventListener {

	/** Handle the given dirty-check event.
	 *
	 * @param event The dirty-check event to be handled.
	 */
	void onDirtyCheck(DirtyCheckEvent event) throws HibernateException;

}

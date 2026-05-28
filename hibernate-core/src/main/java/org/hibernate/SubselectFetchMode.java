/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate;

import jakarta.persistence.EntityManager;

/**
 * Allow explicitly enabling or disabling subselect fetching for an EntityManager.
 *
 * @see org.hibernate.cfg.FetchSettings#USE_SUBSELECT_FETCH
 *
 * @author Steve Ebersole
 */
public enum SubselectFetchMode implements EntityManager.CreationOption {
	/**
	 * Enables subselect fetching.
	 */
	ENABLED,
	/**
	 * Disables subselect fetching.
	 */
	DISABLED
}

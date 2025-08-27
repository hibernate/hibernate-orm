/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.mutation.spi;

/**
 * Actions to perform in regards to a temporary table prior to each use.
 *
 * @author Steve Ebersole
 */
public enum BeforeUseAction {
	CREATE,
	NONE
}

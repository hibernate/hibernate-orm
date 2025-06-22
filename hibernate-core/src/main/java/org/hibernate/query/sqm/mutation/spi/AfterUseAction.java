/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.mutation.spi;

/**
 * Actions to perform in regard to a temporary table after each use.
 *
 * @author Steve Ebersole
 */
public enum AfterUseAction {
	CLEAN,
	DROP,
	NONE
}

/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.mutation.internal.temptable;

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

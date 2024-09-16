/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.mutation.internal.temptable;

/**
 * Actions to perform in regards to a temporary table prior to each use.
 *
 * @author Steve Ebersole
 */
public enum BeforeUseAction {
	CREATE,
	NONE
}

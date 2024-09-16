/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm;

/**
 * Informational - used to identify the source of an SQM statement.
 *
 * @see org.hibernate.query.sqm.tree.SqmStatement#getQuerySource
 *
 * @author Steve Ebersole
 */
public enum SqmQuerySource {
	HQL,
	CRITERIA,
	OTHER
}

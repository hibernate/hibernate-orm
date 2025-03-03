/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm;

/**
 * Identifies the source of an SQM statement.
 *
 * @see org.hibernate.query.sqm.tree.SqmStatement#getQuerySource
 *
 * @author Steve Ebersole
 */
public enum SqmQuerySource {
	/**
	 * The SQM tree represents a query written in HQL or JPQL.
	 */
	HQL,
	/**
	 * The SQM tree was built via the
	 * {@linkplain org.hibernate.query.criteria.HibernateCriteriaBuilder
	 * criteria query API}.
	 */
	CRITERIA,
	/**
	 * The SQM tree came from somewhere else.
	 */
	OTHER
}

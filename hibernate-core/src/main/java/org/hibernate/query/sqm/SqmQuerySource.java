/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
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

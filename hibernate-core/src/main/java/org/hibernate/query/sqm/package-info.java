/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

/**
 * = Semantic Query Model (SQM)
 *
 * Hibernate's SQM defines:
 *
 * * An actual query tree modeling the "meaning" (semantic) of a user supplied query.  See
 *         the {@link org.hibernate.query.sqm.tree} package.
 * * Support for producing an SQM tree based on HQL/JPQL via Antlr or
 *         based on JPA-based (although eventually extended) Criteria tree via
 *         walking.  See {@link org.hibernate.query.sqm.produce}.
 * * Walking (or generally consuming) SQM trees.  See {@link org.hibernate.query.sqm.consume}.
 *
 * Note that this entire package is considered incubating
 */
@org.hibernate.Incubating
package org.hibernate.query.sqm;

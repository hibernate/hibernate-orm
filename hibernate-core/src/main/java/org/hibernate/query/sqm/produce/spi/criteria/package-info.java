/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

/**
 * todo (6.0) : This entire package should just go away.
 *
 * This (like the sqm-specific typing) are left-overs of the initial
 * development of SQM as a separate project.
 *
 * The sole purpose of this of this package (and its sister `internal` package)
 * was to define an abstraction for modeling a JPA criteria tree for the
 * purpose of walking that tree and generating an SQM tree from it.
 *
 * In that original split project design, ORM implemented these interfaces in
 * order for the SQM project to be able to walk its JPA criteria impls.  The important
 * thing to understand is that this approach uses an additional tree walk:
 *
 * 		1. Criteria -> SQM
 * 		2. SQM -> SQL AST
 * 		3. ...
 *
 * Much better approach here is to either:
 *
 * 		1. (ideal) have our SQM nodes directly implement the JPA criteria contracts
 *		2. have our JPA criteria impls maintain (wrap) the corresponding SQM nodes
 */
package org.hibernate.query.sqm.produce.spi.criteria;

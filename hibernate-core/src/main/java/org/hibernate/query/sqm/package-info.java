/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

/**
 * @asciidoc
 *
 * = Semantic Query Model (SQM)
 *
 * SQM is a tree (AST) based representation of the *semantic* interpretation of a query
 * (HQL or Criteria).  It is semantic in that it is more than a simple syntax tree.
 *
 * The SQM package is broken down as:
 *
 * {@link org.hibernate.query.sqm.tree}:: The SQM tree nodes
 * {@link org.hibernate.query.sqm.produce}:: Support for producing an SQM tree based on HQL/JPQL (via Antlr) or
 * 		based on Criteria tree (via walking)
 * {@link org.hibernate.query.sqm.consume}:: Support for walking (or generally consuming) SQM trees
 *
 * Note that this entire package is considered incubating
 */
@org.hibernate.Incubating
package org.hibernate.query.sqm;

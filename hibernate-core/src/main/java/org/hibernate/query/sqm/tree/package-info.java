/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

/**
 * This package contains the classes that make up the SQM tree nodes.
 * <p/>
 * NOTE: the choice to prefix all SQM tree node class names with <b>Sqm</b> was
 * made to help make consumers easier to write - consumers are likely to
 * have classes with the same purpose in their AST (e.g. the notion of a
 * "From".  But if both projects define a class named {@code From} then the
 * consumer would have to fully qualify one reference to distinguish.
 * That makes for overly long code.  Prefixing the names of classes helps
 * alleviate that problem.
 */
package org.hibernate.query.sqm.tree;

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.criteria.internal.predicate;


/**
 * Models what ANSI-SQL terms a <i>truth value</i>.  Specifically, ANSI-SQL defines <tt>TRUE</tt>, <tt>FALSE</tt> and
 * <tt>UNKNOWN</tt> as <i>truth values</i>.  These <i>truth values</i> are used to explicitly check the result of a
 * boolean expression (the syntax is like <tt>a > b IS TRUE</tt>.  <tt>IS TRUE</tt> is the assumed default.
 * <p/>
 * JPA defines support for only <tt>IS TRUE</tt> and <tt>IS FALSE</tt>, not <tt>IS UNKNOWN</tt> (<tt>a > NULL</tt>
 * is an example where the result would be UNKNOWN).  All 3 are provided here for completeness.
 *
 * @author Steve Ebersole
 */
public enum TruthValue {
	TRUE,
	FALSE,
	UNKNOWN
}

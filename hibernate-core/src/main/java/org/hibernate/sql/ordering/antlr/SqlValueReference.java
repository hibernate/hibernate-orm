/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.sql.ordering.antlr;

/**
 * Unifying interface between column and formula references mainly to give more strictly typed result
 * to {@link ColumnMapper#map(String)}
 *
 * @see ColumnReference
 * @see FormulaReference
 *
 * @author Steve Ebersole
 */
public interface SqlValueReference {
}

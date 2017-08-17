/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.spi;

/**
 * Unifying contract for things that are capable of being an expression at
 * the SQL level.
 *
 * Such an expressable can also be part of the SQL select-clause
 *
 * @author Steve Ebersole
 */
public interface SqlExpressable {
}

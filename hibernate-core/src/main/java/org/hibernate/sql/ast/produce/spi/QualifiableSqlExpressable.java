/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.spi;

/**
 * Specialization for SqlSelectables that need to be qualified by a
 * {@link SqlExpressionQualifier} to be treated as a SqlSelection.
 * E.g. a Column
 *
 * @author Steve Ebersole
 */
public interface QualifiableSqlExpressable extends SqlExpressable {

}

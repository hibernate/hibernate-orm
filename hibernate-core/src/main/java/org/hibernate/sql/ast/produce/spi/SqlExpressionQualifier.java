/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.spi;

import org.hibernate.sql.ast.tree.spi.expression.Expression;

/**
 * Contract for things that can act as the qualifier for a SqlSelection for a
 * given (Qualifiable)SqlSelectable.  Think TableGroup.  E.g., given a
 * mapped column named `PERSON.NAME` and a query like
 * `select p.name from Person p ...`, `p` is a reference to the specific
 * `Person` reference which is backed by a TableGroup that acts as the
 * qualifier - the `name` attribute reference is relative to that specific
 * qualifier.
 *
 * @author Steve Ebersole
 */
public interface SqlExpressionQualifier {
	/**
	 * Resolve the SqlSelection for the given QualifiableSqlSelectable
	 * relative to this Qualifier.
	 */
	Expression qualify(QualifiableSqlExpressable sqlSelectable);
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.spi;

import org.hibernate.metamodel.model.relational.spi.Column;
import org.hibernate.metamodel.model.relational.spi.Table;
import org.hibernate.sql.ast.tree.spi.expression.ColumnReference;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.ast.tree.spi.from.TableReference;

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
public interface ColumnReferenceQualifier {
	String getUniqueIdentifier();

	/**
	 * todo (6.0) : currently implementors can (and do) return null - either change that to throw exception or make sure all callers implement null checks
	 */
	TableReference locateTableReference(Table table);

	ColumnReference resolveColumnReference(Column column);

	Expression qualify(QualifiableSqlExpressable sqlSelectable);
}

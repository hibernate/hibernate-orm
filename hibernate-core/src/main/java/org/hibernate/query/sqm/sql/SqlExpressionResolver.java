/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.sql;

import java.util.function.Function;

import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Resolution of a SqlSelection reference for a given SqlSelectable.  Some
 * SqlSelectable are required to be qualified (e.g. a Column) - this is indicated
 * by the QualifiableSqlSelectable sub-type.  The NonQualifiableSqlSelectable
 * sub-type indicates a SqlSelectable that does not require qualification (e.g. a
 * literal).
 * <p/>
 * The point of this contract is to allow "unique-ing" of SqlSelectable references
 * in a query to a single SqlSelection reference - effectively a caching of
 * SqlSelection instances keyed by the SqlSelectable (+ qualifier when applicable)
 * that it refers to.
 *
 * Note also that the returns can be a specialized Expression represented by
 * {@link org.hibernate.sql.ast.tree.expression.SqlSelectionExpression}
 *
 * @author Steve Ebersole
 */
public interface SqlExpressionResolver {
	/**
	 * Given a qualifier + a qualifiable SqlExpressable, resolve the
	 * (Sql)Expression reference.
	 */
	Expression resolveSqlExpression(String key, Function<SqlAstProcessingState,Expression> creator);

	static String createColumnReferenceKey(String tableExpression, String columnExpression) {
		return tableExpression + '.' + columnExpression;
	}

	/**
	 * Resolve the SqlSelection for the given expression
	 */
	SqlSelection resolveSqlSelection(
			Expression expression,
			JavaTypeDescriptor javaTypeDescriptor,
			TypeConfiguration typeConfiguration);

	SqlSelection emptySqlSelection();
}

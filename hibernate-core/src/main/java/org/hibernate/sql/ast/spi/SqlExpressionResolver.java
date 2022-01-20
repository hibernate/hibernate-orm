/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.spi;

import java.util.Objects;
import java.util.function.Function;

import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.spi.TypeConfiguration;

import static java.util.Locale.ROOT;

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
	 * Helper for generating an expression key for a column reference.
	 *
	 * @see #resolveSqlExpression
	 */
	static String createColumnReferenceKey(String tableExpression, String columnExpression) {
		return tableExpression + columnExpression;
	}
	/**
	 * Helper for generating an expression key for a column reference.
	 *
	 * @see #resolveSqlExpression
	 */
	static String createColumnReferenceKey(TableReference tableReference, String columnExpression) {
		assert tableReference != null : "tableReference expected to be non-null";
		assert columnExpression != null : "columnExpression expected to be non-null";
		assert tableReference.getIdentificationVariable() != null : "tableReference#identificationVariable expected to be non-null";
		final String qualifier = tableReference.getIdentificationVariable();
		return qualifier + columnExpression;
	}

	/**
	 * Convenience form for creating a key from TableReference and SelectableMapping
	 */
	static String createColumnReferenceKey(TableReference tableReference, SelectableMapping selectable) {
		assert Objects.equals( selectable.getContainingTableExpression(), tableReference.getTableId() )
				: String.format( ROOT, "Expecting tables to match between TableReference (%s) and SelectableMapping (%s)", tableReference.getTableId(), selectable.getContainingTableExpression() );
		return createColumnReferenceKey( tableReference, selectable.getSelectionExpression() );
	}

	/**
	 * Given a qualifier + a qualifiable SqlExpressable, resolve the
	 * (Sql)Expression reference.
	 */
	Expression resolveSqlExpression(String key, Function<SqlAstProcessingState,Expression> creator);

	/**
	 * Resolve the SqlSelection for the given expression
	 */
	SqlSelection resolveSqlSelection(
			Expression expression,
			JavaType<?> javaType,
			TypeConfiguration typeConfiguration);
}

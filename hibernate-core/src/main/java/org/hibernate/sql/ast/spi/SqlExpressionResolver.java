/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.spi;

import java.util.function.Function;

import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SelectablePath;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.spi.TypeConfiguration;

import static java.util.Locale.ROOT;

/**
 * Resolution of a SqlSelection reference for a given SqlSelectable.  Some
 * SqlSelectable are required to be qualified (e.g. a Column) - this is indicated
 * by the QualifiableSqlSelectable sub-type.  The NonQualifiableSqlSelectable
 * sub-type indicates a SqlSelectable that does not require qualification (e.g. a
 * literal).
 * <p>
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
	static ColumnReferenceKey createColumnReferenceKey(String tableExpression, String columnExpression) {
		return new ColumnReferenceKey(tableExpression, new SelectablePath( columnExpression ) );
	}
	/**
	 * Helper for generating an expression key for a column reference.
	 *
	 * @see #resolveSqlExpression
	 */
	static ColumnReferenceKey createColumnReferenceKey(TableReference tableReference, String columnExpression) {
		return createColumnReferenceKey( tableReference, new SelectablePath( columnExpression ) );
	}
	static ColumnReferenceKey createColumnReferenceKey(TableReference tableReference, SelectablePath selectablePath) {
		assert tableReference != null : "tableReference expected to be non-null";
		assert selectablePath != null : "selectablePath expected to be non-null";
		assert tableReference.getIdentificationVariable() != null : "tableReference#identificationVariable expected to be non-null";
		final String qualifier = tableReference.getIdentificationVariable();
		return createColumnReferenceKey( qualifier, selectablePath );
	}

	static ColumnReferenceKey createColumnReferenceKey(String qualifier, SelectablePath selectablePath) {
		assert qualifier != null : "qualifier expected to be non-null";
		assert selectablePath != null : "selectablePath expected to be non-null";
		return new ColumnReferenceKey( qualifier, selectablePath );
	}

	static ColumnReferenceKey createColumnReferenceKey(String columnExpression) {
		assert columnExpression != null : "columnExpression expected to be non-null";
		return new ColumnReferenceKey( "", new SelectablePath( columnExpression ) );
	}

	/**
	 * Convenience form for creating a key from table expression and SelectableMapping
	 */
	static ColumnReferenceKey createColumnReferenceKey(String tableExpression, SelectableMapping selectable) {
		return createColumnReferenceKey( tableExpression, selectable.getSelectablePath() );
	}

	/**
	 * Convenience form for creating a key from TableReference and SelectableMapping
	 */
	static ColumnReferenceKey createColumnReferenceKey(TableReference tableReference, SelectableMapping selectable) {
		assert tableReference.containsAffectedTableName( selectable.getContainingTableExpression() )
				: String.format( ROOT, "Expecting tables to match between TableReference (%s) and SelectableMapping (%s)", tableReference.getTableId(), selectable.getContainingTableExpression() );
		return createColumnReferenceKey( tableReference, selectable.getSelectablePath() );
	}

	default Expression resolveSqlExpression(TableReference tableReference, SelectableMapping selectableMapping) {
		return resolveSqlExpression(
				createColumnReferenceKey( tableReference, selectableMapping ),
				processingState -> new ColumnReference(
						tableReference,
						selectableMapping
				)
		);
	}


	/**
	 * Given a qualifier + a qualifiable {@link org.hibernate.metamodel.mapping.SqlExpressible},
	 * resolve the (Sql)Expression reference.
	 */
	Expression resolveSqlExpression(ColumnReferenceKey key, Function<SqlAstProcessingState,Expression> creator);

	/**
	 * Resolve the SqlSelection for the given expression
	 */
	SqlSelection resolveSqlSelection(
			Expression expression,
			JavaType<?> javaType,
			FetchParent fetchParent,
			TypeConfiguration typeConfiguration);

	final class ColumnReferenceKey {
		private final String tableQualifier;
		private final SelectablePath selectablePath;

		public ColumnReferenceKey(String tableQualifier, SelectablePath selectablePath) {
			this.tableQualifier = tableQualifier;
			this.selectablePath = selectablePath;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( o == null || getClass() != o.getClass() ) {
				return false;
			}

			ColumnReferenceKey that = (ColumnReferenceKey) o;

			if ( !tableQualifier.equals( that.tableQualifier ) ) {
				return false;
			}
			return selectablePath.equals( that.selectablePath );
		}

		@Override
		public int hashCode() {
			int result = tableQualifier.hashCode();
			result = 31 * result + selectablePath.hashCode();
			return result;
		}
	}
}

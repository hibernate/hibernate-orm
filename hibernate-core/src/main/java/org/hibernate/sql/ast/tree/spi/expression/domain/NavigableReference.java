/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression.domain;

import java.util.List;

import org.hibernate.metamodel.model.domain.spi.Navigable;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.NotYetImplementedException;
import org.hibernate.sql.ast.produce.spi.FromClauseIndex;
import org.hibernate.sql.ast.produce.spi.SqlExpressionQualifier;
import org.hibernate.sql.ast.tree.spi.expression.ColumnReference;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.ast.tree.spi.from.TableGroup;
import org.hibernate.sql.results.spi.QueryResult;
import org.hibernate.sql.results.spi.QueryResultCreationContext;
import org.hibernate.sql.results.spi.QueryResultProducer;
import org.hibernate.sql.results.spi.SqlSelection;

/**
 * An expression that is a reference to some part of the application domain model.
 *
 * NOTE : DO NOT ADD TO SQL AST.  Only meant to be used by `#createQueryResult`
 * for `Navigable`
 *
 * @author Steve Ebersole
 */
public interface NavigableReference extends Expression, QueryResultProducer {

	// todo (6.0) : remove this contract completely.  A navigable is never a (Sql)Expression

	SqlExpressionQualifier getSqlExpressionQualifier();

	@Override
	default SqlSelection createSqlSelection(int jdbcPosition) {
		throw new UnsupportedOperationException(  );
	}

	@Override
	@SuppressWarnings("unchecked")
	public QueryResult createQueryResult(
			String resultVariable,
			QueryResultCreationContext creationContext) {
		getExportedFromElement()
		FromClauseIndex idx - creationContext.getFromClauseIndex().;
		idx.
		final TableGroup tableGroup = idx.resolveTableGroup( getExportedFromElement().getUniqueIdentifier() );
		return getReferencedNavigable().createQueryResult( tableGroup, this, resultVariable, creationContext );
	}
	@Override
	default QueryResult createQueryResult(
			String resultVariable,
			QueryResultCreationContext creationContext) {
		getSqlExpressionQualifier().
		// todo (6.0) : the problem here is needing to be able to resolve the TableGroup(s) we need to pass in to the Navigable
		throw new NotYetImplementedException(  );
		creationContext.

		return getNavigable().createQueryResult( null, this, resultVariable, creationContext );
		return null;
	}

	/**
	 * Get the Navigable referenced by this expression
	 *
	 * @return The Navigable
	 */
	Navigable getNavigable();

	NavigablePath getNavigablePath();

	/**
	 * Corollary to {@link Navigable#getContainer()} on the reference/expression side
	 */
	NavigableContainerReference getNavigableContainerReference();

//	/**
//	 * If this expression leads to the generation of a TableGroup we
//	 * expose that here.
//	 * E.g. consider these 3 cases:
//	 * <p/>
//	 * <h1>Example 1</h1>
//	 * <pre>
//	 * 	{@code select p from Person p}
//	 * </pre>
//	 * Here we have the selection of a root TableGroup by alias.  The
//	 * "expressable" representation of a TableGroup is obtained via its
//	 * `#asExpression` method.  Here, the expression would have no (null)
//	 * `#getNavigableContainerReference` (lhs).
//	 * <p/>
//	 * <h1>Example 2</h1>
//	 * <pre>
//	 * 	{@code select p.name from Person p}
//	 * </pre>
//	 * Here we have the selection of a non-association Navigable.  In this case the
//	 * selected expression naturally resolved to a NavigableReference as opposed
//	 * to a TableGroup.  However, the NavigableReference's `#getNavigableContainerReference`
//	 * points to that root TableGroup's Expression.  The contribute TableGroup would be null
//	 * here, since a basic type does not contribute a TableGroup.
//	 * <p/>
//	 * <h1>Example 3</h1>
//	 * <pre>
//	 * 	{@code select p.partner from Person p}
//	 * </pre>
//	 * Here we have the selection of an association Navigable.  Again here the
//	 * selected expression naturally resolved to a NavigableReference as opposed
//	 * to a TableGroup.  That NavigableReference's `#getNavigableContainerReference`
//	 * points to that root TableGroup's Expression as above, however this NavigableReference
//	 * also contributes a TableGroup - the joined Person TableGroup for partner.
//	 * <p/>
//	 *
//	 * todo (6.0) : consider moving the link between NavigableReference and ColumnReferenceSource into FromClauseIndex
//	 * 		there is a chicken-and-egg aspect to building TableGroup -> (#asExpression) NavigableReference that
//	 * 		might be best solved via cross-referencing NavigableReference -> ColumnReferenceSource in the FromClauseIndex.
//	 *
//	 * 		Another option is to add a `#injectColumnReferenceSource` method here to call from the Table group
//	 * 		after it is fully initialized
//	 */
//	ColumnReferenceSource getContributedColumnReferenceSource();

	// todo (6.0) : this should go away..
	default List<ColumnReference> getColumnReferences() {
		throw new RuntimeException( "Should go away" );
	}
}

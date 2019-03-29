/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.ordering.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.query.internal.QueryOptionsImpl;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.spi.QueryParameterBindings;
import org.hibernate.query.sqm.consume.spi.BaseSqmToSqlAstConverter;
import org.hibernate.query.sqm.internal.DomainParameterXref;
import org.hibernate.query.sqm.tree.select.SqmOrderByClause;
import org.hibernate.query.sqm.tree.select.SqmSortSpecification;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.produce.internal.SqlAstProcessingStateImpl;
import org.hibernate.sql.ast.produce.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.tree.sort.SortSpecification;

/**
 * @author Steve Ebersole
 */
public class OrderByFragmentConverter extends BaseSqmToSqlAstConverter {
	@SuppressWarnings("WeakerAccess")
	public static final QueryOptions QUERY_OPTIONS = new QueryOptionsImpl();

	@SuppressWarnings("WeakerAccess")
	public static List<SortSpecification> convertOrderByFragmentSqmTree(
			SqlAstCreationContext creationContext,
			SqmOrderByClause sqmOrderByClause) {
		return new OrderByFragmentConverter( creationContext ).doConversion( sqmOrderByClause );
	}

	private final List<SortSpecification> collectedSortSpecs = new ArrayList<>();


	@SuppressWarnings("WeakerAccess")
	protected OrderByFragmentConverter(SqlAstCreationContext creationContext) {
		super(
				creationContext,
				QUERY_OPTIONS,
				DomainParameterXref.empty(),
				QueryParameterBindings.NO_PARAM_BINDINGS,
				LoadQueryInfluencers.NONE,
				afterLoadAction -> {}
		);

		getCurrentClauseStack().push( Clause.ORDER );

		getProcessingStateStack().push(
				new SqlAstProcessingStateImpl(
						null,
						this,
						getCurrentClauseStack()::getCurrent,
						() -> (expr) -> {}
				)
		);
	}

	private List<SortSpecification> doConversion(SqmOrderByClause sqmOrderByClause) {
		for ( SqmSortSpecification sqmSortSpecification : sqmOrderByClause.getSortSpecifications() ) {
			collectedSortSpecs.add( visitSortSpecification( sqmSortSpecification ) );
		}

		return collectedSortSpecs;
	}
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.produce.ordering.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.internal.QueryOptionsImpl;
import org.hibernate.query.spi.QueryOptions;
import org.hibernate.query.sqm.consume.spi.BaseSqmToSqlAstConverter;
import org.hibernate.query.sqm.tree.order.SqmOrderByClause;
import org.hibernate.query.sqm.tree.order.SqmSortSpecification;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.sql.ast.produce.spi.SqlAstBuildingContext;
import org.hibernate.sql.ast.produce.sqm.spi.Callback;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.ast.tree.spi.sort.SortSpecification;
import org.hibernate.sql.results.spi.SqlSelection;

/**
 * @author Steve Ebersole
 */
public class OrderByFragmentConverter extends BaseSqmToSqlAstConverter implements SqlAstBuildingContext {
	public static final QueryOptions QUERY_OPTIONS = new QueryOptionsImpl();

	public static List<SortSpecification> convertOrderByFragmentSqmTree(
			SqlAstBuildingContext sqlAstBuildingContext,
			SqmOrderByClause sqmOrderByClause) {
		return new OrderByFragmentConverter( sqlAstBuildingContext ).doConversion( sqmOrderByClause );
	}

	private final List<SortSpecification> collectedSortSpecs = new ArrayList<>();


	protected OrderByFragmentConverter(SqlAstBuildingContext sqlAstBuildingContext) {
		super( sqlAstBuildingContext, QUERY_OPTIONS );
	}

	@Override
	public SessionFactoryImplementor getSessionFactory() {
		return getSqlAstBuildingContext().getSessionFactory();
	}

	@Override
	public Callback getCallback() {
		return afterLoadAction -> {};
	}

	private List<SortSpecification> doConversion(SqmOrderByClause sqmOrderByClause) {
		for ( SqmSortSpecification sqmSortSpecification : sqmOrderByClause.getSortSpecifications() ) {
			collectedSortSpecs.add( visitSortSpecification( sqmSortSpecification ) );
		}

		return collectedSortSpecs;
	}

//	@Override
//	public SqlSelectionGroup resolveSqlSelectionGroup(Navigable navigable) {
//		return super.resolveSqlSelectionGroup( navigable );
//	}

	@Override
	public SqlSelection resolveSqlSelection(Expression expression) {
		throw new NotYetImplementedFor6Exception(  );
	}
}

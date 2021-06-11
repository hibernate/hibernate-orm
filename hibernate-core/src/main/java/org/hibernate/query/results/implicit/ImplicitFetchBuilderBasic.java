/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.results.implicit;

import java.util.function.BiFunction;

import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.ConvertibleModelPart;
import org.hibernate.metamodel.model.convert.spi.BasicValueConverter;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.results.DomainResultCreationStateImpl;
import org.hibernate.query.results.ResultsHelper;
import org.hibernate.query.results.SqlSelectionImpl;
import org.hibernate.query.results.dynamic.DynamicFetchBuilderLegacy;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.basic.BasicFetch;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

import static org.hibernate.query.results.ResultsHelper.jdbcPositionToValuesArrayPosition;
import static org.hibernate.sql.ast.spi.SqlExpressionResolver.createColumnReferenceKey;

/**
 * @author Steve Ebersole
 */
public class ImplicitFetchBuilderBasic implements ImplicitFetchBuilder {
	private final NavigablePath fetchPath;
	private final BasicValuedModelPart fetchable;

	public ImplicitFetchBuilderBasic(NavigablePath fetchPath, BasicValuedModelPart fetchable) {
		this.fetchPath = fetchPath;
		this.fetchable = fetchable;
	}

	@Override
	public BasicFetch<?> buildFetch(
			FetchParent parent,
			NavigablePath fetchPath,
			JdbcValuesMetadata jdbcResultsMetadata,
			BiFunction<String, String, DynamicFetchBuilderLegacy> legacyFetchResolver,
			DomainResultCreationState domainResultCreationState) {
		final DomainResultCreationStateImpl creationStateImpl = ResultsHelper.impl( domainResultCreationState );

		final TableGroup parentTableGroup = creationStateImpl
				.getFromClauseAccess()
				.getTableGroup( parent.getNavigablePath() );

		final String column = fetchable.getSelectionExpression();
		final String table = fetchable.getContainingTableExpression();

		final Expression expression = creationStateImpl.resolveSqlExpression(
				createColumnReferenceKey( parentTableGroup.getTableReference( fetchPath, table ), column ),
				processingState -> {
					final int jdbcPosition = jdbcResultsMetadata.resolveColumnPosition( column );
					final int valuesArrayPosition = jdbcPositionToValuesArrayPosition( jdbcPosition );
					return new SqlSelectionImpl( valuesArrayPosition, fetchable );
				}
		);

		final SqlSelection sqlSelection = creationStateImpl.resolveSqlSelection(
				expression,
				fetchable.getJavaTypeDescriptor(),
				domainResultCreationState.getSqlAstCreationState()
						.getCreationContext()
						.getSessionFactory()
						.getTypeConfiguration()
		);

		final BasicValueConverter<?, ?> valueConverter;
		if ( fetchable instanceof ConvertibleModelPart ) {
			valueConverter = ( (ConvertibleModelPart) fetchable ).getValueConverter();
		}
		else {
			valueConverter = null;
		}

		return new BasicFetch<>(
				sqlSelection.getValuesArrayPosition(),
				parent,
				fetchPath,
				fetchable,
				// todo (6.0) - we don't know
				true,
				valueConverter,
				FetchTiming.IMMEDIATE,
				domainResultCreationState
		);
	}

	@Override
	public String toString() {
		return "ImplicitFetchBuilderBasic(" + fetchPath + ")";
	}
}

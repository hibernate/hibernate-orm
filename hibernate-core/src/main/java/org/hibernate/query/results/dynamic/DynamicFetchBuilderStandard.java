/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.results.dynamic;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.BasicValuedMapping;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.results.DomainResultCreationStateImpl;
import org.hibernate.query.results.ResultsHelper;
import org.hibernate.query.results.SqlSelectionImpl;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

import static org.hibernate.sql.ast.spi.SqlExpressionResolver.createColumnReferenceKey;

/**
 * @author Steve Ebersole
 * @author Christian Beikov
 */
public class DynamicFetchBuilderStandard
		implements DynamicFetchBuilder, NativeQuery.ReturnProperty {

	private final DynamicFetchBuilderContainer container;
	private final String fetchableName;

	private final List<String> columnNames = new ArrayList<>();

	public DynamicFetchBuilderStandard(
			DynamicFetchBuilderContainer container,
			String fetchableName) {
		this.container = container;
		this.fetchableName = fetchableName;
	}

	@Override
	public Fetch buildFetch(
			FetchParent parent,
			NavigablePath fetchPath,
			JdbcValuesMetadata jdbcResultsMetadata,
			BiFunction<String, String, DynamicFetchBuilderLegacy> legacyFetchResolver,
			DomainResultCreationState domainResultCreationState) {
		final DomainResultCreationStateImpl creationStateImpl = ResultsHelper.impl( domainResultCreationState );

		final TableGroup ownerTableGroup = creationStateImpl.getFromClauseAccess().getTableGroup( parent.getNavigablePath() );

		final Fetchable attributeMapping = (Fetchable) parent.getReferencedMappingContainer().findSubPart( fetchableName, null );
		final SqlExpressionResolver sqlExpressionResolver = domainResultCreationState.getSqlAstCreationState().getSqlExpressionResolver();

		final SelectableConsumer selectableConsumer = (selectionIndex, selectableMapping) -> {
			final TableReference tableReference = ownerTableGroup.resolveTableReference(
					fetchPath,
					selectableMapping.getContainingTableExpression()
			);
			final String columnAlias = columnNames.get( selectionIndex );
			sqlExpressionResolver.resolveSqlSelection(
					sqlExpressionResolver.resolveSqlExpression(
							createColumnReferenceKey( tableReference, selectableMapping.getSelectionExpression() ),
							state -> {
								final int resultSetPosition = jdbcResultsMetadata.resolveColumnPosition( columnAlias );
								final int valuesArrayPosition = resultSetPosition - 1;
								return new SqlSelectionImpl( valuesArrayPosition, selectableMapping.getJdbcMapping() );
							}
					),
					selectableMapping.getJdbcMapping().getMappedJavaTypeDescriptor(),
					domainResultCreationState.getSqlAstCreationState()
							.getCreationContext()
							.getSessionFactory()
							.getTypeConfiguration()
			);
		};
		if ( attributeMapping instanceof BasicValuedMapping ) {
			attributeMapping.forEachSelectable( selectableConsumer );
			return parent.generateFetchableFetch(
					attributeMapping,
					fetchPath,
					FetchTiming.IMMEDIATE,
					true,
					null,
					creationStateImpl
			);
		}
		else if ( attributeMapping instanceof ToOneAttributeMapping ) {
			final ToOneAttributeMapping toOneAttributeMapping = (ToOneAttributeMapping) attributeMapping;
			toOneAttributeMapping.getForeignKeyDescriptor().visitKeySelectables( selectableConsumer );
			return parent.generateFetchableFetch(
					attributeMapping,
					fetchPath,
					FetchTiming.DELAYED,
					false,
					null,
					creationStateImpl
			);
		}
		else {
			assert attributeMapping instanceof PluralAttributeMapping;
			final PluralAttributeMapping pluralAttributeMapping = (PluralAttributeMapping) attributeMapping;
			pluralAttributeMapping.getKeyDescriptor().visitTargetSelectables( selectableConsumer );
			return parent.generateFetchableFetch(
					attributeMapping,
					fetchPath,
					FetchTiming.DELAYED,
					false,
					null,
					creationStateImpl
			);
		}
	}

	@Override
	public NativeQuery.ReturnProperty addColumnAlias(String columnAlias) {
		columnNames.add( columnAlias );
		return this;
	}
}

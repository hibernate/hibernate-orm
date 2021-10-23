/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.results.dynamic;

import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import org.hibernate.LockMode;
import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.results.DomainResultCreationStateImpl;
import org.hibernate.query.results.FetchBuilder;
import org.hibernate.query.results.ResultsHelper;
import org.hibernate.query.results.SqlSelectionImpl;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.spi.SqlAliasBase;
import org.hibernate.sql.ast.spi.SqlAliasBaseConstant;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableGroupJoinProducer;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

import static org.hibernate.sql.ast.spi.SqlExpressionResolver.createColumnReferenceKey;

/**
 * @author Steve Ebersole
 * @author Christian Beikov
 */
public class DynamicFetchBuilderLegacy implements DynamicFetchBuilder, NativeQuery.FetchReturn {

	private final String tableAlias;

	private final String ownerTableAlias;
	private final String fetchableName;

	private final List<String> columnNames;
	private final Map<String, FetchBuilder> fetchBuilderMap;
	private final DynamicResultBuilderEntityStandard resultBuilderEntity;

	public DynamicFetchBuilderLegacy(
			String tableAlias,
			String ownerTableAlias,
			String fetchableName,
			List<String> columnNames,
			Map<String, FetchBuilder> fetchBuilderMap) {
		this.tableAlias = tableAlias;
		this.ownerTableAlias = ownerTableAlias;
		this.fetchableName = fetchableName;
		this.columnNames = columnNames;
		this.fetchBuilderMap = fetchBuilderMap;
		this.resultBuilderEntity = null;
	}

	public DynamicFetchBuilderLegacy(
			String tableAlias,
			String ownerTableAlias,
			String fetchableName,
			List<String> columnNames,
			Map<String, FetchBuilder> fetchBuilderMap,
			DynamicResultBuilderEntityStandard resultBuilderEntity) {
		this.tableAlias = tableAlias;
		this.ownerTableAlias = ownerTableAlias;
		this.fetchableName = fetchableName;
		this.columnNames = columnNames;
		this.fetchBuilderMap = fetchBuilderMap;
		this.resultBuilderEntity = resultBuilderEntity;
	}

	@Override
	public String getTableAlias() {
		return tableAlias;
	}

	@Override
	public String getOwnerAlias() {
		return ownerTableAlias;
	}

	@Override
	public String getFetchableName() {
		return fetchableName;
	}

	@Override
	public Fetch buildFetch(
			FetchParent parent,
			NavigablePath fetchPath,
			JdbcValuesMetadata jdbcResultsMetadata,
			BiFunction<String, String, DynamicFetchBuilderLegacy> legacyFetchResolver,
			DomainResultCreationState domainResultCreationState) {
		final DomainResultCreationStateImpl creationState = ResultsHelper.impl( domainResultCreationState );
		final TableGroup ownerTableGroup = creationState.getFromClauseAccess().findByAlias( ownerTableAlias );
		final AttributeMapping attributeMapping = parent.getReferencedMappingContainer()
				.findContainingEntityMapping()
				.findDeclaredAttributeMapping( fetchableName );
		final TableGroup tableGroup;
		if ( attributeMapping instanceof TableGroupJoinProducer ) {
			final SqlAliasBase sqlAliasBase = new SqlAliasBaseConstant( tableAlias );
			final TableGroupJoin tableGroupJoin = ( (TableGroupJoinProducer) attributeMapping ).createTableGroupJoin(
					fetchPath,
					ownerTableGroup,
					tableAlias,
					SqlAstJoinType.INNER,
					true,
					false,
					s -> sqlAliasBase,
					creationState.getSqlExpressionResolver(),
					creationState.getCreationContext()
			);
			creationState.getFromClauseAccess().registerTableGroup( fetchPath, tableGroup = tableGroupJoin.getJoinedGroup() );
		}
		else {
			tableGroup = ownerTableGroup;
		}

		if ( columnNames != null ) {
			final ForeignKeyDescriptor keyDescriptor;
			if ( attributeMapping instanceof PluralAttributeMapping ) {
				final PluralAttributeMapping pluralAttributeMapping = (PluralAttributeMapping) attributeMapping;
				keyDescriptor = pluralAttributeMapping.getKeyDescriptor();
			}
			else {
				// Not sure if this fetch builder can also be used with other attribute mappings
				assert attributeMapping instanceof ToOneAttributeMapping;

				final ToOneAttributeMapping toOneAttributeMapping = (ToOneAttributeMapping) attributeMapping;
				keyDescriptor = toOneAttributeMapping.getForeignKeyDescriptor();
			}

			keyDescriptor.forEachSelectable(
					(selectionIndex, selectableMapping) -> {
						resolveSqlSelection(
								columnNames.get( selectionIndex ),
								createColumnReferenceKey(
										tableGroup.getTableReference( selectableMapping.getContainingTableExpression() ),
										selectableMapping.getSelectionExpression()
								),
								selectableMapping.getJdbcMapping(),
								jdbcResultsMetadata,
								domainResultCreationState
						);
					}
			);

			// We process the fetch builder such that it contains a resultBuilderEntity before calling this method in ResultSetMappingProcessor
			assert resultBuilderEntity != null;

			return resultBuilderEntity.buildFetch(
					parent,
					attributeMapping,
					jdbcResultsMetadata,
					creationState
			);
		}
		else {
			return parent.generateFetchableFetch(
					attributeMapping,
					fetchPath,
					FetchTiming.IMMEDIATE,
					true,
					null,
					domainResultCreationState
			);
		}
	}

	private void resolveSqlSelection(
			String columnAlias,
			String columnKey,
			JdbcMapping jdbcMapping,
			JdbcValuesMetadata jdbcResultsMetadata,
			DomainResultCreationState domainResultCreationState) {
		final SqlExpressionResolver sqlExpressionResolver = domainResultCreationState.getSqlAstCreationState().getSqlExpressionResolver();
		sqlExpressionResolver.resolveSqlSelection(
				sqlExpressionResolver.resolveSqlExpression(
						columnKey,
						state -> {
							final int jdbcPosition = jdbcResultsMetadata.resolveColumnPosition( columnAlias );
							final int valuesArrayPosition = jdbcPosition - 1;
							return new SqlSelectionImpl( valuesArrayPosition, jdbcMapping );
						}
				),
				jdbcMapping.getMappedJavaTypeDescriptor(),
				domainResultCreationState.getSqlAstCreationState().getCreationContext().getSessionFactory().getTypeConfiguration()
		);
	}

	@Override
	public NativeQuery.ReturnProperty addColumnAlias(String columnAlias) {
		columnNames.add( columnAlias );
		return this;
	}

	@Override
	public NativeQuery.FetchReturn setLockMode(LockMode lockMode) {
		return null;
	}

	@Override
	public NativeQuery.FetchReturn addProperty(String propertyName, String columnAlias) {
		return null;
	}

	@Override
	public NativeQuery.ReturnProperty addProperty(String propertyName) {
		return null;
	}

	@Override
	public void visitFetchBuilders(BiConsumer<String, FetchBuilder> consumer) {
		fetchBuilderMap.forEach( consumer );
	}
}

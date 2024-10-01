/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.dynamic;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import org.hibernate.LockMode;
import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.EntityAssociationMapping;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.ValuedModelPart;
import org.hibernate.metamodel.mapping.internal.EmbeddedAttributeMapping;
import org.hibernate.metamodel.mapping.internal.EntityCollectionPart;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.results.DomainResultCreationStateImpl;
import org.hibernate.query.results.FetchBuilder;
import org.hibernate.query.results.ResultsHelper;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.spi.SqlAliasBase;
import org.hibernate.sql.ast.spi.SqlAliasBaseConstant;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableGroupJoinProducer;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

import static org.hibernate.query.results.ResultsHelper.impl;

/**
 * @author Steve Ebersole
 * @author Christian Beikov
 */
public class DynamicFetchBuilderLegacy implements DynamicFetchBuilder, NativeQuery.FetchReturn, DynamicFetchBuilderContainer {

	private static final String ELEMENT_PREFIX = "element.";
	private static final int ELEMENT_PREFIX_LENGTH = 8;

	private final String tableAlias;

	private final String ownerTableAlias;
	private final Fetchable fetchable;

	private final List<String> columnNames;
	private final Map<Fetchable, FetchBuilder> fetchBuilderMap;
	private final DynamicResultBuilderEntityStandard resultBuilderEntity;

	public DynamicFetchBuilderLegacy(
			String tableAlias,
			String ownerTableAlias,
			Fetchable fetchable,
			List<String> columnNames,
			Map<Fetchable, FetchBuilder> fetchBuilderMap) {
		this( tableAlias, ownerTableAlias, fetchable, columnNames, fetchBuilderMap, null );
	}

	public DynamicFetchBuilderLegacy(
			String tableAlias,
			String ownerTableAlias,
			Fetchable fetchable,
			List<String> columnNames,
			Map<Fetchable, FetchBuilder> fetchBuilderMap,
			DynamicResultBuilderEntityStandard resultBuilderEntity) {
		this.tableAlias = tableAlias;
		this.ownerTableAlias = ownerTableAlias;
		this.fetchable = fetchable;
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
	public Fetchable getFetchable() {
		return fetchable;
	}

	@Override
	public NativeQuery.FetchReturn setLockMode(LockMode lockMode) {
		return this;
	}

	@Override
	public NativeQuery.FetchReturn addProperty(String propertyName, String columnAlias) {
		addProperty( resolveFetchable(propertyName),columnAlias );
		return this;
	}

	private Fetchable resolveFetchable(String propertyName) {
		if ( fetchable instanceof EntityAssociationMapping attributeMapping ) {
			return (Fetchable) attributeMapping.findByPath( propertyName );
		}
		else if ( fetchable instanceof PluralAttributeMapping pluralAttributeMapping ) {
			if ( propertyName.equals( "key" ) ) {
				return pluralAttributeMapping.getIndexDescriptor();
			}
			else if ( propertyName.equals( "element" ) ) {
				return pluralAttributeMapping.getElementDescriptor();
			}
			else {
				final CollectionPart elementDescriptor = pluralAttributeMapping.getElementDescriptor();
				if ( elementDescriptor instanceof EntityCollectionPart entityCollectionPart ) {
					if ( propertyName.startsWith( ELEMENT_PREFIX ) ) {
						propertyName = propertyName.substring( ELEMENT_PREFIX_LENGTH );
					}
					return (Fetchable) entityCollectionPart.getEntityMappingType().findByPath( propertyName );
				}
			}
		}
		throw new UnsupportedOperationException( "Unsupported fetchable type: " + fetchable.getClass().getName() );
	}

	@Override
	public NativeQuery.ReturnProperty addProperty(String propertyName) {
		return addProperty( resolveFetchable( propertyName ) );
	}

	@Override
	public DynamicFetchBuilderLegacy cacheKeyInstance() {
		final Map<Fetchable, FetchBuilder> fetchBuilderMap;
		if ( this.fetchBuilderMap == null ) {
			fetchBuilderMap = null;
		}
		else {
			fetchBuilderMap = new HashMap<>( this.fetchBuilderMap.size() );
			for ( Map.Entry<Fetchable, FetchBuilder> entry : this.fetchBuilderMap.entrySet() ) {
				fetchBuilderMap.put( entry.getKey(), entry.getValue().cacheKeyInstance() );
			}
		}
		return new DynamicFetchBuilderLegacy(
				tableAlias,
				ownerTableAlias,
				fetchable,
				columnNames == null ? null : List.copyOf( columnNames ),
				fetchBuilderMap,
				resultBuilderEntity == null ? null : resultBuilderEntity.cacheKeyInstance()
		);
	}

	@Override
	public Fetch buildFetch(
			FetchParent parent,
			NavigablePath fetchPath,
			JdbcValuesMetadata jdbcResultsMetadata,
			BiFunction<String, String, DynamicFetchBuilderLegacy> legacyFetchResolver,
			DomainResultCreationState domainResultCreationState) {
		final DomainResultCreationStateImpl creationState = impl( domainResultCreationState );
		final TableGroup ownerTableGroup = creationState.getFromClauseAccess().findByAlias( ownerTableAlias );
		final TableGroup tableGroup;
		if ( fetchable instanceof TableGroupJoinProducer ) {
			final SqlAliasBase sqlAliasBase = new SqlAliasBaseConstant( tableAlias );
			final TableGroupJoin tableGroupJoin = ( (TableGroupJoinProducer) fetchable ).createTableGroupJoin(
					fetchPath,
					ownerTableGroup,
					tableAlias,
					sqlAliasBase,
					SqlAstJoinType.INNER,
					true,
					false,
					creationState
			);
			ownerTableGroup.addTableGroupJoin( tableGroupJoin );
			creationState.getFromClauseAccess().registerTableGroup( fetchPath, tableGroup = tableGroupJoin.getJoinedGroup() );
		}
		else {
			tableGroup = ownerTableGroup;
		}

		if ( !columnNames.isEmpty() ) {
			final ForeignKeyDescriptor keyDescriptor;
			if ( fetchable instanceof EmbeddedAttributeMapping embeddedAttributeMapping ) {
				embeddedAttributeMapping.forEachSelectable(
						(selectionIndex, selectableMapping) ->
								resolveSqlSelection(
										columnNames.get( selectionIndex ),
										tableGroup.resolveTableReference(
												fetchPath,
												(ValuedModelPart) selectableMapping,
												selectableMapping.getContainingTableExpression()
										),
										selectableMapping,
										jdbcResultsMetadata,
										domainResultCreationState
								)
				);
			}
			else {
				if ( fetchable instanceof PluralAttributeMapping pluralAttributeMapping ) {
					keyDescriptor = pluralAttributeMapping.getKeyDescriptor();
				}
				else {
					// Not sure if this fetch builder can also be used with other attribute mappings
					assert fetchable instanceof ToOneAttributeMapping;

					final ToOneAttributeMapping toOneAttributeMapping = (ToOneAttributeMapping) fetchable;
					keyDescriptor = toOneAttributeMapping.getForeignKeyDescriptor();
				}

				keyDescriptor.forEachSelectable(
						(selectionIndex, selectableMapping) ->
								resolveSqlSelection(
										columnNames.get( selectionIndex ),
										tableGroup.resolveTableReference(
												fetchPath,
												keyDescriptor.getKeyPart(),
												selectableMapping.getContainingTableExpression()
										),
										selectableMapping,
										jdbcResultsMetadata,
										domainResultCreationState
								)
				);
			}
			// We process the fetch builder such that it contains a resultBuilderEntity before calling this method in ResultSetMappingProcessor
			if ( resultBuilderEntity != null ) {
				return resultBuilderEntity.buildFetch(
						parent,
						fetchable,
						jdbcResultsMetadata,
						creationState
				);
			}
		}
		try {
			creationState.pushExplicitFetchMementoResolver(
					fetchable -> {
						if ( fetchable != null ) {
							return findFetchBuilder( fetchable );
						}
						return null;
					}
			);
			return parent.generateFetchableFetch(
					fetchable,
					parent.resolveNavigablePath( fetchable ),
					FetchTiming.IMMEDIATE,
					true,
					null,
					domainResultCreationState
			);
		}
		finally {
			creationState.popExplicitFetchMementoResolver();
		}
	}

	@Override
	public void visitFetchBuilders(BiConsumer<Fetchable, FetchBuilder> consumer) {
		fetchBuilderMap.forEach( consumer );
	}

	private void resolveSqlSelection(
			String columnAlias,
			TableReference tableReference,
			SelectableMapping selectableMapping,
			JdbcValuesMetadata jdbcResultsMetadata,
			DomainResultCreationState domainResultCreationState) {
		final DomainResultCreationStateImpl creationStateImpl = impl( domainResultCreationState );
		creationStateImpl.resolveSqlSelection(
				ResultsHelper.resolveSqlExpression(
						creationStateImpl,
						jdbcResultsMetadata,
						tableReference,
						selectableMapping,
						columnAlias
				),
				selectableMapping.getJdbcMapping().getJdbcJavaType(),
				null,
				domainResultCreationState.getSqlAstCreationState()
						.getCreationContext()
						.getSessionFactory()
						.getTypeConfiguration()
		);
	}

	@Override
	public NativeQuery.ReturnProperty addColumnAlias(String columnAlias) {
		columnNames.add( columnAlias );
		return this;
	}

	@Override
	public List<String> getColumnAliases() {
		return columnNames;
	}

	@Override
	public DynamicFetchBuilder addProperty(Fetchable fetchable) {
		DynamicFetchBuilderStandard fetchBuilder = new DynamicFetchBuilderStandard( fetchable );
		fetchBuilderMap.put( fetchable, fetchBuilder );
		return fetchBuilder;
	}

	@Override
	public FetchBuilder findFetchBuilder(Fetchable fetchable) {
		return fetchBuilderMap.get( fetchable );
	}

	@Override
	public DynamicFetchBuilderContainer addProperty(Fetchable fetchable, String columnAlias) {
		final DynamicFetchBuilder fetchBuilder = addProperty( fetchable );
		fetchBuilder.addColumnAlias( columnAlias );
		return this;
	}

	@Override
	public DynamicFetchBuilderContainer addProperty(Fetchable fetchable, String... columnAliases) {
		final DynamicFetchBuilder fetchBuilder = addProperty( fetchable );
		for ( String columnAlias : columnAliases ) {
			fetchBuilder.addColumnAlias( columnAlias );
		}
		return this;
	}

	@Override
	public void addFetchBuilder(Fetchable fetchable, FetchBuilder fetchBuilder) {
		fetchBuilderMap.put( fetchable, fetchBuilder );
	}

//	@Override
//	public void visitFetchBuilders(BiConsumer<Fetchable, FetchBuilder> consumer) {
//		fetchBuilderMap.forEach( consumer );
//	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		final DynamicFetchBuilderLegacy that = (DynamicFetchBuilderLegacy) o;
		return tableAlias.equals( that.tableAlias )
				&& ownerTableAlias.equals( that.ownerTableAlias )
				&& fetchable.equals( that.fetchable )
				&& Objects.equals( columnNames, that.columnNames )
				&& Objects.equals( fetchBuilderMap, that.fetchBuilderMap )
				&& Objects.equals( resultBuilderEntity, that.resultBuilderEntity );
	}

	@Override
	public int hashCode() {
		int result = tableAlias.hashCode();
		result = 31 * result + ownerTableAlias.hashCode();
		result = 31 * result + fetchable.hashCode();
		result = 31 * result + ( columnNames != null ? columnNames.hashCode() : 0 );
		result = 31 * result + ( fetchBuilderMap != null ? fetchBuilderMap.hashCode() : 0 );
		result = 31 * result + ( resultBuilderEntity != null ? resultBuilderEntity.hashCode() : 0 );
		return result;
	}
}

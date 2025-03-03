/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.internal.dynamic;

import org.hibernate.AssertionFailure;
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
import org.hibernate.query.results.FetchBuilder;
import org.hibernate.query.results.LegacyFetchBuilder;
import org.hibernate.query.results.internal.DomainResultCreationStateImpl;
import org.hibernate.query.results.internal.ResultsHelper;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.SqlAstJoinType;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;

import static org.hibernate.query.results.internal.ResultsHelper.impl;

/**
 * @author Steve Ebersole
 * @author Christian Beikov
 */
public class DynamicFetchBuilderLegacy
		implements LegacyFetchBuilder, DynamicFetchBuilder,
				NativeQuery.FetchReturn, NativeQuery.ReturnableResultNode, DynamicFetchBuilderContainer {

	private static final String ELEMENT_PREFIX = "element.";
	private static final int ELEMENT_PREFIX_LENGTH = 8;

	private final String tableAlias;

	private final String ownerTableAlias;
	private final Fetchable fetchable;

	private final List<String> columnNames;
	private final Map<Fetchable, FetchBuilder> fetchBuilderMap;
	private final DynamicResultBuilderEntityStandard resultBuilderEntity;

	private LockMode lockMode;

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
	public String getFetchableName() {
		return fetchable.getFetchableName();
	}

	@Override
	public NativeQuery.FetchReturn setLockMode(LockMode lockMode) {
		this.lockMode = lockMode;
		return this;
	}

	@Override
	public NativeQuery.FetchReturn addProperty(String propertyName, String columnAlias) {
		addProperty( resolveFetchable( propertyName ), columnAlias );
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
				return pluralAttributeMapping.getElementDescriptor().getCollectionAttribute();
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
		return new DynamicFetchBuilderLegacy(
				tableAlias,
				ownerTableAlias,
				fetchable,
				columnNames == null ? null : List.copyOf( columnNames ),
				fetchBuilderMap(),
				resultBuilderEntity == null ? null : resultBuilderEntity.cacheKeyInstance()
		);
	}

	private Map<Fetchable, FetchBuilder> fetchBuilderMap() {
		if ( this.fetchBuilderMap == null ) {
			return null;
		}
		else {
			final Map<Fetchable, FetchBuilder> fetchBuilderMap = new HashMap<>( this.fetchBuilderMap.size() );
			for ( Map.Entry<Fetchable, FetchBuilder> entry : this.fetchBuilderMap.entrySet() ) {
				fetchBuilderMap.put( entry.getKey(), entry.getValue().cacheKeyInstance() );
			}
			return fetchBuilderMap;
		}
	}

	@Override
	public Fetch buildFetch(
			FetchParent parent,
			NavigablePath fetchPath,
			JdbcValuesMetadata jdbcResultsMetadata,
			DomainResultCreationState domainResultCreationState) {
		final DomainResultCreationStateImpl creationState = impl( domainResultCreationState );
		final TableGroup ownerTableGroup = creationState.getFromClauseAccess().findByAlias( ownerTableAlias );
		final TableGroup tableGroup = tableGroup( fetchPath, ownerTableGroup, creationState );
		if ( lockMode != null ) {
			domainResultCreationState.getSqlAstCreationState().registerLockMode( tableAlias, lockMode );
		}
		if ( columnNames != null ) {
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
				final ForeignKeyDescriptor keyDescriptor = getForeignKeyDescriptor( fetchable );
				if ( !columnNames.isEmpty() ) {
					keyDescriptor.forEachSelectable( (selectionIndex, selectableMapping) -> {
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
								);
							}
					);
				}
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

	private TableGroup tableGroup(
			NavigablePath fetchPath,
			TableGroup ownerTableGroup,
			DomainResultCreationStateImpl creationState) {
		if ( fetchable instanceof TableGroupJoinProducer tableGroupJoinProducer ) {
			final TableGroupJoin tableGroupJoin = tableGroupJoinProducer.createTableGroupJoin(
					fetchPath,
					ownerTableGroup,
					tableAlias,
					new SqlAliasBaseConstant( tableAlias ),
					SqlAstJoinType.INNER,
					true,
					false,
					creationState
			);
			ownerTableGroup.addTableGroupJoin( tableGroupJoin );
			final TableGroup tableGroup = tableGroupJoin.getJoinedGroup();
			creationState.getFromClauseAccess().registerTableGroup( fetchPath, tableGroup );
			return tableGroup;
		}
		else {
			return ownerTableGroup;
		}
	}

	private static ForeignKeyDescriptor getForeignKeyDescriptor(Fetchable fetchable) {
		if ( fetchable instanceof PluralAttributeMapping pluralAttributeMapping ) {
			return pluralAttributeMapping.getKeyDescriptor();
		}
		else if ( fetchable instanceof ToOneAttributeMapping toOneAttributeMapping ) {
			return toOneAttributeMapping.getForeignKeyDescriptor();
		}
		else {
			// Not sure if this fetch builder can also be used with other attribute mappings
			throw new AssertionFailure( "Unrecognized AttributeMapping" );
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
				domainResultCreationState.getSqlAstCreationState().getCreationContext().getTypeConfiguration()
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
			&& lockMode.equals( that.lockMode )
			&& Objects.equals( columnNames, that.columnNames )
			&& Objects.equals( fetchBuilderMap, that.fetchBuilderMap )
			&& Objects.equals( resultBuilderEntity, that.resultBuilderEntity );
	}

	@Override
	public int hashCode() {
		int result = tableAlias.hashCode();
		result = 31 * result + ownerTableAlias.hashCode();
		result = 31 * result + fetchable.hashCode();
		result = 31 * result + lockMode.hashCode();
		result = 31 * result + ( columnNames != null ? columnNames.hashCode() : 0 );
		result = 31 * result + ( fetchBuilderMap != null ? fetchBuilderMap.hashCode() : 0 );
		result = 31 * result + ( resultBuilderEntity != null ? resultBuilderEntity.hashCode() : 0 );
		return result;
	}
}

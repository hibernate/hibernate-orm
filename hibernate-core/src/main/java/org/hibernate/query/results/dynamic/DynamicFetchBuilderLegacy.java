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
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
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
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

import static org.hibernate.query.results.ResultsHelper.impl;

/**
 * @author Steve Ebersole
 * @author Christian Beikov
 */
public class DynamicFetchBuilderLegacy implements DynamicFetchBuilder, NativeQuery.FetchReturn, DynamicFetchBuilderContainer {

	private static final String ELEMENT_PREFIX = CollectionPart.Nature.ELEMENT.getName() + ".";
	private static final String INDEX_PREFIX = CollectionPart.Nature.INDEX.getName() + ".";

	private final String tableAlias;

	private final String ownerTableAlias;
	private final String fetchableName;

	private final List<String> columnNames;
	private final Map<String, FetchBuilder> fetchBuilderMap;
	private final DynamicResultBuilderEntityStandard resultBuilderEntity;

	private LockMode lockMode;

	public DynamicFetchBuilderLegacy(
			String tableAlias,
			String ownerTableAlias,
			String fetchableName,
			List<String> columnNames,
			Map<String, FetchBuilder> fetchBuilderMap) {
		this( tableAlias, ownerTableAlias, fetchableName, columnNames, fetchBuilderMap, null );
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
	public DynamicFetchBuilderLegacy cacheKeyInstance() {
		final Map<String, FetchBuilder> fetchBuilderMap;
		if ( this.fetchBuilderMap == null ) {
			fetchBuilderMap = null;
		}
		else {
			fetchBuilderMap = new HashMap<>( this.fetchBuilderMap.size() );
			for ( Map.Entry<String, FetchBuilder> entry : this.fetchBuilderMap.entrySet() ) {
				fetchBuilderMap.put( entry.getKey(), entry.getValue().cacheKeyInstance() );
			}
		}
		return new DynamicFetchBuilderLegacy(
				tableAlias,
				ownerTableAlias,
				fetchableName,
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

			if ( !columnNames.isEmpty() ) {
				keyDescriptor.forEachSelectable(
						(selectionIndex, selectableMapping) -> {
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

			// We process the fetch builder such that it contains a resultBuilderEntity before calling this method in ResultSetMappingProcessor
			if ( resultBuilderEntity != null ) {
				return resultBuilderEntity.buildFetch(
						parent,
						attributeMapping,
						jdbcResultsMetadata,
						creationState
				);
			}
		}
		try {
			final Map.Entry<String, NavigablePath> currentRelativePath = creationState.getCurrentRelativePath();
			final String prefix;
			if ( currentRelativePath == null ) {
				prefix = "";
			}
			else {
				prefix = currentRelativePath.getKey()
						.replace( ELEMENT_PREFIX, "" )
						.replace( INDEX_PREFIX, "" ) + ".";
			}
			creationState.pushExplicitFetchMementoResolver(
					relativePath -> {
						if ( relativePath.startsWith( prefix ) ) {
							return findFetchBuilder( relativePath.substring( prefix.length() ) );
						}
						return null;
					}
			);
			return parent.generateFetchableFetch(
					attributeMapping,
					parent.resolveNavigablePath( attributeMapping ),
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
	public NativeQuery.FetchReturn setLockMode(LockMode lockMode) {
		this.lockMode = lockMode;
		return this;
	}

	@Override
	public DynamicFetchBuilderLegacy addProperty(String propertyName, String columnAlias) {
		addProperty( propertyName ).addColumnAlias( columnAlias );
		return this;
	}

	@Override
	public DynamicFetchBuilder addProperty(String propertyName) {
		DynamicFetchBuilderStandard fetchBuilder = new DynamicFetchBuilderStandard( propertyName );
		fetchBuilderMap.put( propertyName, fetchBuilder );
		return fetchBuilder;
	}

	@Override
	public FetchBuilder findFetchBuilder(String fetchableName) {
		return fetchBuilderMap.get( fetchableName );
	}

	@Override
	public DynamicFetchBuilderContainer addProperty(String propertyName, String... columnAliases) {
		final DynamicFetchBuilder fetchBuilder = addProperty( propertyName );
		for ( String columnAlias : columnAliases ) {
			fetchBuilder.addColumnAlias( columnAlias );
		}
		return this;
	}

	@Override
	public void addFetchBuilder(String propertyName, FetchBuilder fetchBuilder) {
		fetchBuilderMap.put( propertyName, fetchBuilder );
	}

	@Override
	public void visitFetchBuilders(BiConsumer<String, FetchBuilder> consumer) {
		fetchBuilderMap.forEach( consumer );
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
				&& fetchableName.equals( that.fetchableName )
				&& Objects.equals( columnNames, that.columnNames )
				&& Objects.equals( fetchBuilderMap, that.fetchBuilderMap )
				&& Objects.equals( resultBuilderEntity, that.resultBuilderEntity );
	}

	@Override
	public int hashCode() {
		int result = tableAlias.hashCode();
		result = 31 * result + ownerTableAlias.hashCode();
		result = 31 * result + fetchableName.hashCode();
		result = 31 * result + ( columnNames != null ? columnNames.hashCode() : 0 );
		result = 31 * result + ( fetchBuilderMap != null ? fetchBuilderMap.hashCode() : 0 );
		result = 31 * result + ( resultBuilderEntity != null ? resultBuilderEntity.hashCode() : 0 );
		return result;
	}
}

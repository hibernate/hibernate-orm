/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.internal.complete;

import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.results.spi.ResultBuilder;
import org.hibernate.query.results.internal.DomainResultCreationStateImpl;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlAliasBaseConstant;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

import java.util.Arrays;

import static org.hibernate.query.results.internal.ResultsHelper.impl;
import static org.hibernate.query.results.internal.ResultsHelper.resolveSqlExpression;

/**
 * @author Steve Ebersole
 */
public class CompleteResultBuilderCollectionStandard implements CompleteResultBuilderCollection, NativeQuery.CollectionReturn {

	private final String tableAlias;
	private final NavigablePath navigablePath;
	private final PluralAttributeMapping pluralAttributeDescriptor;
	private final String[] keyColumnNames;
	private final String[] indexColumnNames;
	private final String[] elementColumnNames;

	public CompleteResultBuilderCollectionStandard(
			String tableAlias,
			NavigablePath navigablePath,
			PluralAttributeMapping pluralAttributeDescriptor) {
		this( tableAlias, navigablePath, pluralAttributeDescriptor, null, null, null );
	}

	public CompleteResultBuilderCollectionStandard(
			String tableAlias,
			NavigablePath navigablePath,
			PluralAttributeMapping pluralAttributeDescriptor,
			String[] keyColumnNames,
			String[] indexColumnNames,
			String[] elementColumnNames) {
		this.tableAlias = tableAlias;
		this.navigablePath = navigablePath;
		this.pluralAttributeDescriptor = pluralAttributeDescriptor;
		this.keyColumnNames = keyColumnNames;
		this.indexColumnNames = indexColumnNames;
		this.elementColumnNames = elementColumnNames;
	}

	@Override
	public Class<?> getJavaType() {
		return pluralAttributeDescriptor.getExpressibleJavaType().getJavaTypeClass();
	}

	@Override
	public String getTableAlias() {
		return tableAlias;
	}

	@Override
	public PluralAttributeMapping getPluralAttribute() {
		return pluralAttributeDescriptor;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public ResultBuilder cacheKeyInstance() {
		return this;
	}

	@Override
	public DomainResult<?> buildResult(
			JdbcValuesMetadata jdbcResultsMetadata,
			int resultPosition,
			DomainResultCreationState domainResultCreationState) {
		final var creationStateImpl = impl( domainResultCreationState );
		final var fromClauseAccess = creationStateImpl.getFromClauseAccess();
		final var rootTableGroup = pluralAttributeDescriptor.createRootTableGroup(
				false,
				navigablePath,
				tableAlias,
				new SqlAliasBaseConstant( tableAlias ),
				null,
				creationStateImpl
		);
		fromClauseAccess.registerTableGroup( navigablePath, rootTableGroup );

		resolveSelections(
				rootTableGroup,
				pluralAttributeDescriptor.getKeyDescriptor(),
				keyColumnNames,
				jdbcResultsMetadata,
				creationStateImpl
		);
		if ( pluralAttributeDescriptor.getIndexDescriptor() != null ) {
			resolveSelections(
					rootTableGroup,
					pluralAttributeDescriptor.getIndexDescriptor(),
					indexColumnNames,
					jdbcResultsMetadata,
					creationStateImpl
			);
		}
		resolveSelections(
				rootTableGroup,
				pluralAttributeDescriptor.getElementDescriptor(),
				elementColumnNames,
				jdbcResultsMetadata,
				creationStateImpl
		);

		return pluralAttributeDescriptor.createDomainResult(
				navigablePath,
				rootTableGroup,
				null,
				domainResultCreationState
		);
	}

	private void resolveSelections(
			TableGroup tableGroup,
			ModelPart modelPart,
			String[] columnNames,
			JdbcValuesMetadata jdbcResultsMetadata,
			DomainResultCreationStateImpl creationStateImpl) {
		final var typeConfiguration =
				creationStateImpl.getSessionFactory()
						.getTypeConfiguration();
		resolveSelections( modelPart, (selectionIndex, selectableMapping) ->
				creationStateImpl.resolveSqlSelection(
						resolveSqlExpression(
								creationStateImpl,
								jdbcResultsMetadata,
								tableGroup.resolveTableReference(
										selectableMapping.getContainingTableExpression() ),
								selectableMapping,
								columnNames[selectionIndex]
						),
						selectableMapping.getJdbcMapping().getJdbcJavaType(),
						null,
						typeConfiguration
				) );
	}

	private static void resolveSelections(ModelPart modelPart, SelectableConsumer consumer) {
		if ( modelPart instanceof EntityValuedModelPart entityValuedModelPart ) {
			final var entityMappingType = entityValuedModelPart.getEntityMappingType();
			int index = entityMappingType.getIdentifierMapping().forEachSelectable( consumer );
			final var discriminatorMapping = entityMappingType.getDiscriminatorMapping();
			if ( discriminatorMapping != null ) {
				index += discriminatorMapping.forEachSelectable( index, consumer );
			}
			entityMappingType.forEachSelectable( index, consumer );
		}
		else {
			modelPart.forEachSelectable( consumer );
		}
	}

	@Override
	public boolean equals(Object object) {
		if ( this == object ) {
			return true;
		}
		else if ( !( object instanceof CompleteResultBuilderCollectionStandard that ) ) {
			return false;
		}
		else {
			return tableAlias.equals( that.tableAlias )
				&& navigablePath.equals( that.navigablePath )
				&& pluralAttributeDescriptor.equals( that.pluralAttributeDescriptor )
				&& Arrays.equals( keyColumnNames, that.keyColumnNames )
				&& Arrays.equals( indexColumnNames, that.indexColumnNames )
				&& Arrays.equals( elementColumnNames, that.elementColumnNames );
		}
	}

	@Override
	public int hashCode() {
		int result = tableAlias.hashCode();
		result = 31 * result + navigablePath.hashCode();
		result = 31 * result + pluralAttributeDescriptor.hashCode();
		result = 31 * result + Arrays.hashCode( keyColumnNames );
		result = 31 * result + Arrays.hashCode( indexColumnNames );
		result = 31 * result + Arrays.hashCode( elementColumnNames );
		return result;
	}
}

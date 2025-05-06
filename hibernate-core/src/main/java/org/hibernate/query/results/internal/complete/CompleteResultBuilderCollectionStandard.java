/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.results.internal.complete;

import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.results.ResultBuilder;
import org.hibernate.query.results.internal.DomainResultCreationStateImpl;
import org.hibernate.query.results.internal.FromClauseAccessImpl;
import org.hibernate.query.results.internal.ResultsHelper;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlAliasBaseConstant;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

import java.util.Arrays;

import static org.hibernate.query.results.internal.ResultsHelper.impl;

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
		final DomainResultCreationStateImpl creationStateImpl = impl( domainResultCreationState );
		final FromClauseAccessImpl fromClauseAccess = creationStateImpl.getFromClauseAccess();
		final TableGroup rootTableGroup = pluralAttributeDescriptor.createRootTableGroup(
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
		resolveSelections( modelPart, (selectionIndex, selectableMapping) ->
				creationStateImpl.resolveSqlSelection(
						ResultsHelper.resolveSqlExpression(
								creationStateImpl,
								jdbcResultsMetadata,
								tableGroup.resolveTableReference( selectableMapping.getContainingTableExpression() ),
								selectableMapping,
								columnNames[selectionIndex]
						),
						selectableMapping.getJdbcMapping().getJdbcJavaType(),
						null,
						creationStateImpl.getSessionFactory().getTypeConfiguration()
				) );
	}

	private static void resolveSelections(ModelPart modelPart, SelectableConsumer consumer) {
		if ( modelPart instanceof EntityValuedModelPart entityValuedModelPart ) {
			final EntityMappingType entityMappingType = entityValuedModelPart.getEntityMappingType();
			int index = entityMappingType.getIdentifierMapping().forEachSelectable( consumer );
			if ( entityMappingType.getDiscriminatorMapping() != null ) {
				index += entityMappingType.getDiscriminatorMapping().forEachSelectable( index, consumer );
			}
			entityMappingType.forEachSelectable( index, consumer );
		}
		else {
			modelPart.forEachSelectable( consumer );
		}
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}

		final CompleteResultBuilderCollectionStandard that = (CompleteResultBuilderCollectionStandard) o;
		return tableAlias.equals( that.tableAlias )
			&& navigablePath.equals( that.navigablePath )
			&& pluralAttributeDescriptor.equals( that.pluralAttributeDescriptor )
			&& Arrays.equals( keyColumnNames, that.keyColumnNames )
			&& Arrays.equals( indexColumnNames, that.indexColumnNames )
			&& Arrays.equals( elementColumnNames, that.elementColumnNames );
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

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.results.complete;

import java.util.Arrays;
import java.util.function.BiFunction;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.query.NativeQuery;
import org.hibernate.query.results.DomainResultCreationStateImpl;
import org.hibernate.query.results.FromClauseAccessImpl;
import org.hibernate.query.results.ResultBuilder;
import org.hibernate.query.results.ResultsHelper;
import org.hibernate.query.results.dynamic.DynamicFetchBuilderLegacy;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlAliasBaseConstant;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesMetadata;

import static org.hibernate.query.results.ResultsHelper.impl;

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
			BiFunction<String, String, DynamicFetchBuilderLegacy> legacyFetchResolver,
			DomainResultCreationState domainResultCreationState) {
		final DomainResultCreationStateImpl creationStateImpl = impl( domainResultCreationState );
		final SessionFactoryImplementor sessionFactory = creationStateImpl.getSessionFactory();
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
		final SelectableConsumer consumer = (selectionIndex, selectableMapping) -> {
			final String columnName = columnNames[selectionIndex];
			creationStateImpl.resolveSqlSelection(
					ResultsHelper.resolveSqlExpression(
							creationStateImpl,
							jdbcResultsMetadata,
							tableGroup.resolveTableReference( selectableMapping.getContainingTableExpression() ),
							selectableMapping,
							columnName
					),
					selectableMapping.getJdbcMapping().getJdbcJavaType(),
					null,
					creationStateImpl.getSessionFactory().getTypeConfiguration()
			);
		};
		if ( modelPart instanceof EntityValuedModelPart ) {
			final EntityMappingType entityMappingType = ( (EntityValuedModelPart) modelPart ).getEntityMappingType();
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

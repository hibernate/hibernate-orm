/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.function.Consumer;

import org.hibernate.LockMode;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.CascadeStyle;
import org.hibernate.metamodel.mapping.CollectionIdentifierDescriptor;
import org.hibernate.metamodel.mapping.CollectionMappingType;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.StateArrayContributorMetadataAccess;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.spi.SqlAliasBase;
import org.hibernate.sql.ast.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.spi.SqlAliasStemHelper;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupBuilder;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableReferenceCollector;
import org.hibernate.sql.results.internal.domain.collection.DelayedCollectionFetch;
import org.hibernate.sql.results.internal.domain.collection.EagerCollectionFetch;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.FetchParent;

/**
 * @author Steve Ebersole
 */
public class PluralAttributeMappingImpl extends AbstractAttributeMapping implements PluralAttributeMapping {
	private final int stateArrayPosition;
	private final PropertyAccess propertyAccess;
	private final StateArrayContributorMetadataAccess stateArrayContributorMetadataAccess;

	private final ForeignKeyDescriptor fkDescriptor;
	private final CollectionPart elementDescriptor;
	private final CollectionPart indexDescriptor;
	private final CollectionIdentifierDescriptor identifierDescriptor;

	private final FetchStrategy fetchStrategy;
	private final CascadeStyle cascadeStyle;

	private final CollectionPersister collectionDescriptor;

	private final String sqlAliasStem;

	@SuppressWarnings("WeakerAccess")
	public PluralAttributeMappingImpl(
			String attributeName,
			PropertyAccess propertyAccess,
			StateArrayContributorMetadataAccess stateArrayContributorMetadataAccess,
			CollectionMappingType collectionMappingType,
			int stateArrayPosition,
			ForeignKeyDescriptor fkDescriptor,
			CollectionPart elementDescriptor,
			CollectionPart indexDescriptor,
			CollectionIdentifierDescriptor identifierDescriptor,
			FetchStrategy fetchStrategy,
			CascadeStyle cascadeStyle,
			ManagedMappingType declaringType,
			CollectionPersister collectionDescriptor) {
		super( attributeName, collectionMappingType, declaringType );
		this.propertyAccess = propertyAccess;
		this.stateArrayContributorMetadataAccess = stateArrayContributorMetadataAccess;
		this.stateArrayPosition = stateArrayPosition;
		this.fkDescriptor = fkDescriptor;
		this.elementDescriptor = elementDescriptor;
		this.indexDescriptor = indexDescriptor;
		this.identifierDescriptor = identifierDescriptor;
		this.fetchStrategy = fetchStrategy;
		this.cascadeStyle = cascadeStyle;
		this.collectionDescriptor = collectionDescriptor;

		this.sqlAliasStem = SqlAliasStemHelper.INSTANCE.generateStemFromAttributeName( attributeName );
	}

	@Override
	public CollectionMappingType getMappedTypeDescriptor() {
		return (CollectionMappingType) super.getMappedTypeDescriptor();
	}

	@Override
	public ForeignKeyDescriptor getKeyDescriptor() {
		return fkDescriptor;
	}

	@Override
	public CollectionPersister getCollectionDescriptor() {
		return collectionDescriptor;
	}

	@Override
	public CollectionPart getElementDescriptor() {
		return elementDescriptor;
	}

	@Override
	public CollectionPart getIndexDescriptor() {
		return indexDescriptor;
	}

	@Override
	public CollectionIdentifierDescriptor getIdentifierDescriptor() {
		return identifierDescriptor;
	}

	@Override
	public int getStateArrayPosition() {
		return stateArrayPosition;
	}

	@Override
	public StateArrayContributorMetadataAccess getAttributeMetadataAccess() {
		return stateArrayContributorMetadataAccess;
	}

	@Override
	public PropertyAccess getPropertyAccess() {
		return propertyAccess;
	}

	@Override
	public String getFetchableName() {
		return getAttributeName();
	}

	@Override
	public FetchStrategy getMappedFetchStrategy() {
		return fetchStrategy;
	}

	@Override
	public Fetch generateFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			FetchTiming fetchTiming,
			boolean selected,
			LockMode lockMode,
			String resultVariable,
			DomainResultCreationState creationState) {
		final SqlAstCreationState sqlAstCreationState = creationState.getSqlAstCreationState();

		if ( fetchTiming == FetchTiming.IMMEDIATE || selected ) {
			final TableGroup collectionTableGroup = sqlAstCreationState.getFromClauseAccess().resolveTableGroup(
					fetchablePath,
					p -> {
						final TableGroupJoin tableGroupJoin = createTableGroupJoin(
								fetchablePath,
								sqlAstCreationState.getFromClauseAccess().getTableGroup( fetchParent.getNavigablePath() ),
								null,
								JoinType.LEFT,
								lockMode,
								creationState.getSqlAliasBaseManager(),
								creationState.getSqlAstCreationState().getSqlExpressionResolver(),
								creationState.getSqlAstCreationState().getCreationContext()
						);
						return tableGroupJoin.getJoinedGroup();
					}
			);

			return new EagerCollectionFetch(
					fetchablePath,
					this,
					fkDescriptor.createDomainResult( fetchablePath, collectionTableGroup, creationState ),
					getAttributeMetadataAccess().resolveAttributeMetadata( null ).isNullable(),
					fetchParent,
					creationState
			);
		}


		return new DelayedCollectionFetch(
				fetchablePath,
				this,
				true,
				fetchParent
		);
	}

	@Override
	public String getSqlAliasStem() {
		return sqlAliasStem;
	}

	@Override
	public TableGroupJoin createTableGroupJoin(
			NavigablePath navigablePath,
			TableGroup lhs,
			String explicitSourceAlias,
			JoinType joinType,
			LockMode lockMode,
			SqlAliasBaseGenerator aliasBaseGenerator,
			SqlExpressionResolver sqlExpressionResolver,
			SqlAstCreationContext creationContext) {
		final String aliasRoot = explicitSourceAlias == null ? sqlAliasStem : explicitSourceAlias;
		final SqlAliasBase sqlAliasBase = aliasBaseGenerator.createSqlAliasBase( aliasRoot );

		final TableGroupBuilder tableGroupBuilder = TableGroupBuilder.builder(
				navigablePath,
				this,
				lockMode,
				sqlAliasBase,
				creationContext.getSessionFactory()
		);

		applyTableReferences(
				sqlAliasBase,
				joinType,
				tableGroupBuilder,
				sqlExpressionResolver,
				creationContext
		);

		return new TableGroupJoin( navigablePath, joinType, tableGroupBuilder.build() );
	}

	@Override
	public void applyTableReferences(
			SqlAliasBase sqlAliasBase,
			JoinType baseJoinType,
			TableReferenceCollector collector,
			SqlExpressionResolver sqlExpressionResolver,
			SqlAstCreationContext creationContext) {
		getCollectionDescriptor().applyTableReferences( sqlAliasBase, baseJoinType, collector, sqlExpressionResolver, creationContext );
	}

	@Override
	public ModelPart findSubPart(String name, EntityMappingType treatTargetType) {
		final CollectionPart.Nature nature = CollectionPart.Nature.fromName( name );
		if ( nature == CollectionPart.Nature.ELEMENT ) {
			return elementDescriptor;
		}
		else if ( nature == CollectionPart.Nature.INDEX ) {
			return indexDescriptor;
		}

		return null;
	}

	@Override
	public void visitSubParts(Consumer<ModelPart> consumer, EntityMappingType treatTargetType) {
		consumer.accept( elementDescriptor );
		if ( indexDescriptor != null ) {
			consumer.accept( indexDescriptor );
		}
	}

	@Override
	public int getNumberOfFetchables() {
		return indexDescriptor == null ? 1 : 2;
	}

	@Override
	public String toString() {
		return "PluralAttribute(" + getCollectionDescriptor().getRole() + ")";
	}
}

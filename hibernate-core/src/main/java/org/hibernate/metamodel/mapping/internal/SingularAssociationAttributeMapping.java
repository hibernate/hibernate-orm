/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import org.hibernate.LockMode;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchTiming;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.StateArrayContributorMetadataAccess;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.JoinType;
import org.hibernate.sql.ast.spi.SqlAliasBase;
import org.hibernate.sql.ast.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.spi.SqlAliasStemHelper;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlAstProcessingState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupBuilder;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableGroupJoinProducer;
import org.hibernate.sql.ast.tree.from.TableReferenceCollector;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.results.internal.domain.entity.DelayedEntityFetchImpl;
import org.hibernate.sql.results.internal.domain.entity.EntityFetch;
import org.hibernate.sql.results.spi.DomainResultCreationState;
import org.hibernate.sql.results.spi.Fetch;
import org.hibernate.sql.results.spi.FetchParent;

/**
 * @author Steve Ebersole
 */
public class SingularAssociationAttributeMapping extends AbstractSingularAttributeMapping
		implements EntityValuedModelPart, TableGroupJoinProducer {
	private final String sqlAliasStem;
	private final boolean isNullable;
	private final ForeignKeyDescriptor foreignKeyDescriptor;

	public SingularAssociationAttributeMapping(
			String name,
			int stateArrayPosition,
			boolean isNullable,
			ForeignKeyDescriptor foreignKeyDescriptor,
			StateArrayContributorMetadataAccess attributeMetadataAccess,
			FetchStrategy mappedFetchStrategy,
			EntityMappingType type,
			ManagedMappingType declaringType,
			PropertyAccess propertyAccess) {
		super(
				name,
				stateArrayPosition,
				attributeMetadataAccess,
				mappedFetchStrategy,
				type,
				declaringType,
				propertyAccess
		);
		this.sqlAliasStem = SqlAliasStemHelper.INSTANCE.generateStemFromAttributeName( name );
		this.isNullable = isNullable;
		this.foreignKeyDescriptor = foreignKeyDescriptor;
	}

	@Override
	public EntityMappingType getMappedTypeDescriptor() {
		return (EntityMappingType) super.getMappedTypeDescriptor();
	}

	@Override
	public EntityMappingType getEntityMappingType() {
		return getMappedTypeDescriptor();
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
		final TableGroup lhsTableGroup = sqlAstCreationState.getFromClauseAccess()
				.getTableGroup( fetchParent.getNavigablePath() );

		if ( fetchTiming == FetchTiming.IMMEDIATE && selected ) {
			if ( sqlAstCreationState.getFromClauseAccess().findTableGroup( fetchablePath ) == null ) {
				// todo (6.0) : verify the JoinType is correct
				JoinType joinType;
				if ( isNullable ) {
					joinType = JoinType.LEFT;
				}
				else {
					joinType = JoinType.INNER;
				}
				final TableGroupJoin tableGroupJoin = createTableGroupJoin(
						fetchablePath,
						lhsTableGroup,
						null,
						joinType,
						lockMode,
						creationState.getSqlAliasBaseManager(),
						creationState.getSqlAstCreationState().getSqlExpressionResolver(),
						creationState.getSqlAstCreationState().getCreationContext()
				);

				lhsTableGroup.addTableGroupJoin( tableGroupJoin );

				sqlAstCreationState.getFromClauseAccess().registerTableGroup(
						fetchablePath,
						tableGroupJoin.getJoinedGroup()
				);
			}

			return new EntityFetch(
					fetchParent,
					this,
					lockMode,
					!selected,
					fetchablePath,
					creationState
			);
		}

		return new DelayedEntityFetchImpl(
				fetchParent,
				this,
				lockMode,
				isNullable,
				fetchablePath,
				foreignKeyDescriptor.createDomainResult( fetchablePath, lhsTableGroup, creationState ),
				creationState
		);
	}

	@Override
	public int getNumberOfFetchables() {
		return getEntityMappingType().getNumberOfFetchables();
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

		getMappedTypeDescriptor().getIdentifierMapping();

		final TableGroup tableGroup = tableGroupBuilder.build();
		final TableGroupJoin tableGroupJoin = new TableGroupJoin(
				navigablePath,
				joinType,
				tableGroup,
				null
		);

		lhs.addTableGroupJoin( tableGroupJoin );

		final Predicate predicate = foreignKeyDescriptor.generateJoinPredicate(
				lhs,
				tableGroup,
				joinType,
				sqlExpressionResolver,
				creationContext
		);
		tableGroupJoin.applyPredicate( predicate );

		return tableGroupJoin;
	}

	@Override
	public String getSqlAliasStem() {
		return sqlAliasStem;
	}

	@Override
	public void applyTableReferences(
			SqlAliasBase sqlAliasBase,
			JoinType baseJoinType,
			TableReferenceCollector collector,
			SqlExpressionResolver sqlExpressionResolver,
			SqlAstCreationContext creationContext) {
		getMappedTypeDescriptor().applyTableReferences(
				sqlAliasBase,
				baseJoinType,
				collector,
				sqlExpressionResolver,
				creationContext
		);
	}

	@Override
	public boolean isCircular(FetchParent fetchParent, SqlAstProcessingState creationState) {
		final NavigablePath navigablePath = fetchParent.getNavigablePath();

		final NavigablePath parent = navigablePath.getParent();
		if ( parent == null ) {
			return false;
		}
		else {
			final String entityName = getMappedTypeDescriptor().getEntityName();
			if ( parent.getLocalName().equals( entityName ) ) {
				return true;
			}
			else {
				NavigablePath parentOfParent = parent.getParent();
				if ( parentOfParent == null ) {
					return false;
				}
				else {
					if ( parentOfParent.getParent() != null ) {
						return false;
					}
					else {
						final EntityPersister entityDescriptor = creationState.getSqlAstCreationState()
								.getCreationContext()
								.getDomainModel()
								.findEntityDescriptor( parentOfParent.getFullPath() );
						final String parentEntityName = entityDescriptor.findSubPart( parent.getLocalName() ).getJavaTypeDescriptor()
								.getJavaType()
								.getName();
						if ( parentEntityName.equals( entityName ) ) {
							return true;
						}
					}
				}
				return false;
			}
		}
	}

}

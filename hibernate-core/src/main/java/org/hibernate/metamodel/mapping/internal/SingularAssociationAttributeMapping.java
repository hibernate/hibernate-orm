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
import org.hibernate.mapping.ToOne;
import org.hibernate.metamodel.mapping.EntityAssociationMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.metamodel.mapping.StateArrayContributorMetadataAccess;
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
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.entity.EntityFetch;
import org.hibernate.sql.results.graph.entity.EntityValuedFetchable;
import org.hibernate.sql.results.graph.entity.internal.EntityFetchDelayedImpl;
import org.hibernate.sql.results.graph.entity.internal.EntityFetchJoinedImpl;
import org.hibernate.sql.results.graph.entity.internal.EntityFetchSelectImpl;

/**
 * @author Steve Ebersole
 */
public class SingularAssociationAttributeMapping extends AbstractSingularAttributeMapping
		implements EntityValuedFetchable, EntityAssociationMapping, TableGroupJoinProducer {
	private final String sqlAliasStem;
	private final boolean isNullable;
	private final boolean referringPrimaryKey;
	final protected boolean unwrapProxy;
	private final String referencedPropertyName;

	private ForeignKeyDescriptor foreignKeyDescriptor;


	public SingularAssociationAttributeMapping(
			String name,
			int stateArrayPosition,
			ToOne value,
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
		this.isNullable = value.isNullable();
		referencedPropertyName = value.getReferencedPropertyName();
		referringPrimaryKey = value.isReferenceToPrimaryKey();
		unwrapProxy = value.isUnwrapProxy();
	}

	public void setForeignKeyDescriptor(ForeignKeyDescriptor foreignKeyDescriptor) {
		this.foreignKeyDescriptor = foreignKeyDescriptor;
	}

	public ForeignKeyDescriptor getForeignKeyDescriptor() {
		return this.foreignKeyDescriptor;
	}

	public String getReferencedPropertyName() {
		return referencedPropertyName;
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
	public EntityFetch generateFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			FetchTiming fetchTiming,
			boolean selected,
			LockMode lockMode,
			String resultVariable,
			DomainResultCreationState creationState) {
		final SqlAstCreationState sqlAstCreationState = creationState.getSqlAstCreationState();
		final TableGroup lhsTableGroup = sqlAstCreationState.getFromClauseAccess().getTableGroup(
				fetchParent.getNavigablePath()
		);

		if ( fetchTiming == FetchTiming.IMMEDIATE && selected ) {
			if ( sqlAstCreationState.getFromClauseAccess().findTableGroup( fetchablePath ) == null ) {
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

			return new EntityFetchJoinedImpl(
					fetchParent,
					this,
					lockMode,
					true,
					fetchablePath,
					creationState
			);
		}

		final DomainResult keyResult;

		if ( referringPrimaryKey ) {
			keyResult = foreignKeyDescriptor.createDomainResult( fetchablePath, lhsTableGroup, creationState );
		}
		else {
			keyResult = ( (EntityPersister) getDeclaringType() ).getIdentifierMapping()
					.createDomainResult( fetchablePath, lhsTableGroup, null, creationState );
		}

		assert !selected;
		if ( fetchTiming == FetchTiming.IMMEDIATE ) {
			return new EntityFetchSelectImpl(
					fetchParent,
					this,
					lockMode,
					isNullable,
					fetchablePath,
					keyResult,
					creationState
			);
		}

		return new EntityFetchDelayedImpl(
				fetchParent,
				this,
				lockMode,
				isNullable,
				fetchablePath,
				keyResult
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
		predicate.forceTableReferenceJoinRendering();
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
		final NavigablePath panentNaviblePath = fetchParent.getNavigablePath();
		final NavigablePath parentParentNavigablePath = panentNaviblePath.getParent();
		if ( parentParentNavigablePath == null ) {
			return false;
		}
		if ( getAttributeName().equals( parentParentNavigablePath.getLocalName() ) ) {
			return true;
		}
		else {
			final ModelPartContainer modelPart = creationState.getSqlAstCreationState()
					.getFromClauseAccess()
					.findTableGroup( parentParentNavigablePath )
					.getModelPart();
//			final SingularAssociationAttributeMapping part = (SingularAssociationAttributeMapping) modelPart
//					.findSubPart( panentNaviblePath.getLocalName(), null );
			final EntityAssociationMapping part = (EntityAssociationMapping) modelPart.findSubPart( panentNaviblePath.getLocalName(), null );

			if ( panentNaviblePath.getLocalName().equals( referencedPropertyName )
					&& part.getFetchableName().equals( referencedPropertyName ) ) {
				return true;
			}
			else if ( part.getKeyTargetMatchPart() != null
//					&& part.getKeyTargetMatchPart().equals( this ) ) {
					&& part.getKeyTargetMatchPart().getPartName().equals( getAttributeName() ) ) {
				return true;
			}
		}

		return false;
	}

	public boolean isNullable() {
		return isNullable;
	}

	public boolean isUnwrapProxy() {
		return unwrapProxy;
	}

	@Override
	public EntityMappingType getAssociatedEntityMappingType() {
		return getEntityMappingType();
	}

	@Override
	public ModelPart getKeyTargetMatchPart() {
		return foreignKeyDescriptor;
	}
}

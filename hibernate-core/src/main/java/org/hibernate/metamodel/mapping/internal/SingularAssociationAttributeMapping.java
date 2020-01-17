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
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.spi.SqlAliasBase;
import org.hibernate.sql.ast.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.spi.SqlAliasStemHelper;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlAstProcessingState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.from.StandardTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableGroupJoinProducer;
import org.hibernate.sql.ast.tree.from.TableReference;
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
	private final NavigableRole navigableRole;
	private final String subRole;

	private final String sqlAliasStem;
	private final boolean isNullable;
	private final boolean referringPrimaryKey;
	private final boolean unwrapProxy;
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

		this.navigableRole = declaringType.getNavigableRole().appendContainer( name );
		final int containerMarkerPosition = navigableRole.getFullPath().lastIndexOf( '#' );
		if ( containerMarkerPosition < 0 ) {
			subRole = name;
		}
		else {
			subRole = navigableRole.getFullPath().substring( containerMarkerPosition + 1 );
		}
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
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Override
	public boolean isCircular(FetchParent fetchParent, SqlAstProcessingState creationState) {
		// E.g. say we have a query like:
		// 		select p
		//		from Person p
		//		join fetch p.address a
		//		join fetch a.owner
		//
		// where `owner` is the "owner" (in the mapped-by sense) of the association.  In other words it is a
		// bi-directional mapping.
		//
		// This call is trying to generate a fetch for the NavigablePath `Person(p).address`.
		// What we need to determine is whether owner is the same as address's container.  This might include
		// multiple parent-paths which we need to walk up to find the container (an entity of collection)

		final NavigablePath parentNavigablePath = fetchParent.getNavigablePath();
		NavigablePath pathToParentParent = parentNavigablePath.getParent();
		if ( pathToParentParent == null ) {
			return false;
		}

		for ( int i = 0; i < numberOfPathElementsToContainer; i++ ) {
			pathToParentParent = pathToParentParent.getParent();
		}

		assert pathToParentParent != null;

		final ModelPartContainer modelPart = creationState.getSqlAstCreationState()
				.getFromClauseAccess()
				.findTableGroup( pathToParentParent )
				.getModelPart();


		final ModelPart subPart = modelPart.findSubPart( parentNavigablePath.getLocalName(), null );
		if ( subPart instanceof EntityAssociationMapping ) {
			final EntityAssociationMapping part = (EntityAssociationMapping) subPart;

			if ( parentNavigablePath.getLocalName().equals( referencedPropertyName )
					&& part.getFetchableName().equals( referencedPropertyName ) ) {
				return true;
			}
			else if ( part.getKeyTargetMatchPart() != null
					&& part.getKeyTargetMatchPart().getPartName().equals( getAttributeName() ) ) {
				return true;
			}
		}

		return false;
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
				SqlAstJoinType sqlAstJoinType;
				if ( isNullable ) {
					sqlAstJoinType = SqlAstJoinType.LEFT;
				}
				else {
					sqlAstJoinType = SqlAstJoinType.INNER;
				}
				final TableGroupJoin tableGroupJoin = createTableGroupJoin(
						fetchablePath,
						lhsTableGroup,
						null,
						sqlAstJoinType,
						lockMode,
						creationState.getSqlAliasBaseManager(),
						creationState.getSqlAstCreationState().getSqlExpressionResolver(),
						creationState.getSqlAstCreationState().getCreationContext()
				);

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
			SqlAstJoinType sqlAstJoinType,
			LockMode lockMode,
			SqlAliasBaseGenerator aliasBaseGenerator,
			SqlExpressionResolver sqlExpressionResolver,
			SqlAstCreationContext creationContext) {
		final String aliasRoot = explicitSourceAlias == null ? sqlAliasStem : explicitSourceAlias;
		final SqlAliasBase sqlAliasBase = aliasBaseGenerator.createSqlAliasBase( aliasRoot );

		final TableReference primaryTableReference = getEntityMappingType().createPrimaryTableReference(
				sqlAliasBase,
				sqlExpressionResolver,
				creationContext
		);

		final TableGroup tableGroup = new StandardTableGroup(
				navigablePath,
				this,
				lockMode,
				primaryTableReference,
				sqlAliasBase,
				(tableExpression, tg) -> getEntityMappingType().createTableReferenceJoin(
						tableExpression,
						sqlAliasBase,
						primaryTableReference,
						false,
						sqlExpressionResolver,
						creationContext
				),
				creationContext.getSessionFactory()
		);

		final TableGroupJoin tableGroupJoin = new TableGroupJoin(
				navigablePath,
				sqlAstJoinType,
				tableGroup,
				null
		);

		lhs.addTableGroupJoin( tableGroupJoin );

		final Predicate predicate = foreignKeyDescriptor.generateJoinPredicate(
				lhs,
				tableGroup,
				sqlAstJoinType,
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

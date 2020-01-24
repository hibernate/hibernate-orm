/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import org.hibernate.LockMode;
import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.FetchStrategy;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.internal.JoinHelper;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.ToOne;
import org.hibernate.metamodel.mapping.EntityAssociationMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
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
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.entity.EntityFetch;
import org.hibernate.sql.results.graph.entity.EntityValuedFetchable;
import org.hibernate.sql.results.graph.entity.internal.EntityFetchDelayedImpl;
import org.hibernate.sql.results.graph.entity.internal.EntityFetchJoinedImpl;
import org.hibernate.sql.results.graph.entity.internal.EntityFetchSelectImpl;
import org.hibernate.sql.results.internal.domain.BiDirectionalFetchImpl;

/**
 * @author Steve Ebersole
 */
public class SingularAssociationAttributeMapping extends AbstractSingularAttributeMapping
		implements EntityValuedFetchable, EntityAssociationMapping, TableGroupJoinProducer {

	public enum Cardinality {
		ONE_TO_ONE,
		MANY_TO_ONE,
		LOGICAL_ONE_TO_ONE
	}

	private final NavigableRole navigableRole;

	private final String sqlAliasStem;
	private final boolean isNullable;
	private final boolean unwrapProxy;

	private final String referencedPropertyName;
	private final boolean referringPrimaryKey;

	private final Cardinality cardinality;

	private ForeignKeyDescriptor foreignKeyDescriptor;


	public SingularAssociationAttributeMapping(
			String name,
			int stateArrayPosition,
			ToOne bootValue,
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
		this.isNullable = bootValue.isNullable();
		this.referencedPropertyName = bootValue.getReferencedPropertyName();
		this.referringPrimaryKey = bootValue.isReferenceToPrimaryKey();
		this.unwrapProxy = bootValue.isUnwrapProxy();

		if ( referringPrimaryKey ) {
			assert referencedPropertyName == null;
		}
		else {
			assert referencedPropertyName != null;
		}

		if ( bootValue instanceof ManyToOne ) {
			final ManyToOne manyToOne = (ManyToOne) bootValue;
			if ( manyToOne.isLogicalOneToOne() ) {
				cardinality = Cardinality.LOGICAL_ONE_TO_ONE;
			}
			else {
				cardinality = Cardinality.MANY_TO_ONE;
			}
		}
		else {
			assert bootValue instanceof OneToOne;
			cardinality = Cardinality.ONE_TO_ONE;
		}

		this.navigableRole = declaringType.getNavigableRole().appendContainer( name );
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
	public Fetch resolveCircularFetch(
			NavigablePath fetchablePath,
			FetchParent fetchParent,
			SqlAstProcessingState creationState) {
		// given a typical Order/LineItem model and a query like:
		// 		select o
		//		from Order o
		//		join fetch o.lineItems l
		//		join fetch l.order
		//
		//   - note : Order has a collection of LineItems which is "mapped by" LineItem#order
		//
		// the join-fetch for `l.order` ought point back to `o`.
		//
		// `o` -> Order(o)
		// `l` -> Order(o).lineItems(l).{element}
		// `l.order` -> Order(o).lineItems(l).{element}.order
		//
		// both `Order(o)` and `Order(o).lineItems(l).order` have the same identifying columns, so we know
		// they are circular.  So how do we resolve the columns? ...
		//
		// see `org.hibernate.loader.JoinWalker.isDuplicateAssociation(java.lang.String, java.lang.String[], org.hibernate.type.AssociationType)` in
		// previous versions of Hibernate
		//
		// For `l.order` we are in SingularAssociationAttributeMapping as the Fetchable, so we have access to the FK descriptor.
		// For `o` (the potential circular target reference) we need to locate the
		//
		//
		// where `owner` is the "owner" (in the mapped-by sense) of the association.  In other words it is a
		// bi-directional mapping.
		//
		// This call is trying to generate a fetch for the NavigablePath `Person(p).address`.
		// What we need to determine is whether owner is the same as address's container.  This might include
		// multiple parent-paths which we need to walk up to find the container (an entity of collection)


		final NavigablePath pathToParent = fetchParent.getNavigablePath();
		final NavigablePath pathToParentParent = pathToParent.getParent();

		// pathToParent : org.hibernate.orm.test.annotations.embedded.EmbeddedCircularFetchTests$RootEntity(r).intermediateComponent.leaves.{element}
		// pathToParentParent : org.hibernate.orm.test.annotations.embedded.EmbeddedCircularFetchTests$RootEntity(r).intermediateComponent.leaves

		// attributeName : rootEntity
		// referencedPropertyName : null

		if ( pathToParentParent == null ) {
			return null;
		}

		final TableGroup parentParentTableGroup = creationState.getSqlAstCreationState()
				.getFromClauseAccess()
				.findTableGroup( pathToParentParent );

		parentParentTableGroup.getModelPart().findContainingEntityMapping()

		final ModelPartContainer parentParentPart = parentParentTableGroup.getModelPart();
		final ModelPart parentPart = parentParentPart.findSubPart( pathToParent.getLocalName(), null );

		if ( ! parentPart.equals( fetchParent.getReferencedModePart() ) ) {
			throw new AssertionError(  );
		}



		final EntityMappingType containingEntityMapping = findContainingEntityMapping();

		// find the key-columns for the `parentParentTableGroup` and see if they match the fk-target
		switch ( cardinality ) {
			case ONE_TO_ONE:
			case LOGICAL_ONE_TO_ONE: {
				if ( ! EntityValuedModelPart.class.isInstance( parentPart ) ) {
					throw new IllegalStateException(
							"Parent part [" + pathToParent + "] did not refer to a `EntityValuedModelPart` - " + parentPart
					);
				}
				final EntityValuedModelPart entityValuedParentPart = (EntityValuedModelPart) parentPart;

				throw new NotYetImplementedFor6Exception( getClass() );
			}
			case MANY_TO_ONE: {

			}
			default: {
				throw new UnsupportedOperationException( "Unknown to-one singular attribute cardinality - " + cardinality.name() );
			}
		}
		if ( parentPart instanceof EntityCollectionPart ) {
			final EntityCollectionPart entityCollectionPart = (EntityCollectionPart) parentPart;
			final String mappedBy = entityCollectionPart.getMappedBy();
			if ( mappedBy.equals( getAttributeName() ) ) {
				return new BiDirectionalFetchImpl(
						FetchTiming.IMMEDIATE,
						fetchablePath,
						fetchParent,
						this,
						fetchParent.getNavigablePath().getParent()
				);
			}
		}
		else if ( parentPart instanceof EntityAssociationMapping ) {
			final EntityAssociationMapping entitySubPart = (EntityAssociationMapping) parentPart;

			final boolean condition1 = pathToParent.getLocalName().equals( referencedPropertyName )
					&& entitySubPart.getFetchableName().equals( referencedPropertyName );
			final boolean condition2 = entitySubPart.getKeyTargetMatchPart() != null
					&& entitySubPart.getKeyTargetMatchPart().getPartName().equals( getAttributeName() );

			if ( condition1 || condition2 ) {
				return new BiDirectionalFetchImpl(
						FetchTiming.IMMEDIATE,
						fetchablePath,
						fetchParent,
						this,
						fetchParent.getNavigablePath().getParent()
				);
			}
		}

		return null;
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

	@Override
	public String toString() {
		return "SingularAssociationAttributeMapping {" + navigableRole + "}";
	}
}

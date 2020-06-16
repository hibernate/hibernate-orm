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
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.ManyToOne;
import org.hibernate.mapping.OneToOne;
import org.hibernate.mapping.ToOne;
import org.hibernate.metamodel.mapping.AssociationKey;
import org.hibernate.metamodel.mapping.EntityAssociationMapping;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.StateArrayContributorMetadataAccess;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.spi.SqlAliasBase;
import org.hibernate.sql.ast.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.spi.SqlAliasStemHelper;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.from.StandardTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableGroupJoinProducer;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.embeddable.EmbeddableValuedFetchable;
import org.hibernate.sql.results.graph.entity.EntityFetch;
import org.hibernate.sql.results.graph.entity.EntityValuedFetchable;
import org.hibernate.sql.results.graph.entity.internal.EntityDelayedFetchImpl;
import org.hibernate.sql.results.graph.entity.internal.EntityFetchJoinedImpl;
import org.hibernate.sql.results.graph.entity.internal.EntityFetchSelectImpl;
import org.hibernate.sql.results.internal.domain.CircularFetchImpl;
import org.hibernate.sql.results.internal.domain.CircularBiDirectionalFetchImpl;
import org.hibernate.type.ForeignKeyDirection;

/**
 * @author Steve Ebersole
 */
public class ToOneAttributeMapping extends AbstractSingularAttributeMapping
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
	private final EntityMappingType entityMappingType;

	private final String referencedPropertyName;
	private final boolean referringPrimaryKey;

	private final Cardinality cardinality;
	private String mappedBy;

	private ForeignKeyDescriptor foreignKeyDescriptor;
	private ForeignKeyDirection foreignKeyDirection;
	private String identifyingColumnsTableExpression;

	public ToOneAttributeMapping(
			String name,
			NavigableRole navigableRole,
			int stateArrayPosition,
			ToOne bootValue,
			StateArrayContributorMetadataAccess attributeMetadataAccess,
			FetchStrategy mappedFetchStrategy,
			EntityMappingType entityMappingType,
			ManagedMappingType declaringType,
			PropertyAccess propertyAccess) {
		super(
				name,
				stateArrayPosition,
				attributeMetadataAccess,
				mappedFetchStrategy,
				declaringType,
				propertyAccess
		);
		this.sqlAliasStem = SqlAliasStemHelper.INSTANCE.generateStemFromAttributeName( name );
		this.isNullable = bootValue.isNullable();
		this.referencedPropertyName = bootValue.getReferencedPropertyName();
		this.referringPrimaryKey = bootValue.isReferenceToPrimaryKey();
		this.unwrapProxy = bootValue.isUnwrapProxy();
		this.entityMappingType = entityMappingType;

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
			String mappedByProperty = ( (OneToOne) bootValue ).getMappedByProperty();
			if ( mappedByProperty == null ) {
				mappedBy = StringHelper.nullIfEmpty( referencedPropertyName );
			}
			else {
				mappedBy = mappedByProperty;
			}
		}

		this.navigableRole = navigableRole;
	}

	public void setForeignKeyDescriptor(ForeignKeyDescriptor foreignKeyDescriptor) {
		this.foreignKeyDescriptor = foreignKeyDescriptor;
	}

	public void setIdentifyingColumnsTableExpression(String tableExpression) {
		identifyingColumnsTableExpression = tableExpression;
	}

	public void setForeignKeyDirection(ForeignKeyDirection direction) {
		foreignKeyDirection = direction;
	}

	public ForeignKeyDescriptor getForeignKeyDescriptor() {
		return this.foreignKeyDescriptor;
	}

	public String getReferencedPropertyName() {
		return referencedPropertyName;
	}

	@Override
	public EntityMappingType getMappedTypeDescriptor() {
		return getEntityMappingType();
	}

	@Override
	public EntityMappingType getEntityMappingType() {
		return entityMappingType;
	}

	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Override
	public Fetch resolveCircularFetch(
			NavigablePath fetchablePath,
			FetchParent fetchParent,
			DomainResultCreationState creationState) {
		final AssociationKey associationKey = foreignKeyDescriptor.getAssociationKey();

		if ( creationState.isAssociationKeyVisited( associationKey ) ) {
			NavigablePath parent = fetchablePath.getParent();
			ModelPart modelPart = creationState.resolveModelPart( parent );
			if ( modelPart instanceof EmbeddedIdentifierMappingImpl ) {
				while ( parent.getFullPath().endsWith( EntityIdentifierMapping.ROLE_LOCAL_NAME ) ) {
					parent = parent.getParent();
				}
			}
			while ( modelPart instanceof EmbeddableValuedFetchable ) {
				parent = parent.getParent();
				modelPart = creationState.resolveModelPart( parent );
			}

			if ( this.mappedBy != null && parent.getFullPath().endsWith( this.mappedBy )  ) {
				/*
					class Child {
						@OneToOne(mappedBy = "biologicalChild")
						private Mother mother;
					}

					class Mother {
						@OneToOne
						private Child biologicalChild;
					}

					fetchablePath= Mother.biologicalChild.mother
					this.mappedBy = "biologicalChild"
					parent.getFullPath() = "Mother.biologicalChild"
				 */
				return createCircularBiDirectionalFetch(
						fetchablePath,
						fetchParent,
						parent.getParent(),
						LockMode.READ
				);
			}

			/*
				check if mappedBy is on the other side of the association
			 */
			final String otherSideMappedBy = getOtherSideMappedBy( modelPart, parent.getParent(), creationState );
			if ( otherSideMappedBy != null ) {
					/*
						class Child {
							@OneToOne(mappedBy = "biologicalChild")
							private Mother mother;
						}

						class Mother {
							@OneToOne
							private Child biologicalChild;
						}

						fetchablePath = "Child.mother.biologicalChild"
						otherSideAssociationModelPart = ToOneAttributeMapping("Child.mother")
						otherSideMappedBy = "biologicalChild"

					 */

				if ( fetchablePath.getFullPath().endsWith( otherSideMappedBy ) ) {
					return createCircularBiDirectionalFetch(
							fetchablePath,
							fetchParent,
							parent.getParent(),
							LockMode.READ
					);
				}
			}
			/*
						class Child {
							@OneToOne
							private Mother mother;
						}

						class Mother {
							@OneToOne
							private Child stepMother;
						}

				We have a cirularity but it is not bidirectional
			 */
			if ( referringPrimaryKey ) {
				final TableGroup parentTableGroup = creationState
						.getSqlAstCreationState()
						.getFromClauseAccess()
						.getTableGroup( fetchParent.getNavigablePath() );
				return new CircularFetchImpl(
						getEntityMappingType(),
						getTiming(),
						fetchablePath,
						fetchParent,
						this,
						fetchablePath,
						foreignKeyDescriptor.createDomainResult( fetchablePath, parentTableGroup, creationState )
				);
			}
		}
		return null;
	}

	private String getOtherSideMappedBy(
			ModelPart modelPart,
			NavigablePath parentOfParent,
			DomainResultCreationState creationState) {
		if ( modelPart instanceof ToOneAttributeMapping ) {
			return ( (ToOneAttributeMapping) modelPart ).getMappedBy();
		}

		if ( modelPart instanceof EntityCollectionPart ) {
			return ( (PluralAttributeMapping) creationState.resolveModelPart( parentOfParent ) ).getMappedBy();
		}

		return null;
	}

	public String getMappedBy(){
		return mappedBy;
	}

	private Fetch createCircularBiDirectionalFetch(
			NavigablePath fetchablePath,
			FetchParent fetchParent,
			NavigablePath referencedNavigablePath,
			LockMode lockMode) {
		return new CircularBiDirectionalFetchImpl(
				FetchTiming.IMMEDIATE,
				fetchablePath,
				fetchParent,
				this,
				lockMode,
				referencedNavigablePath
		);
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
		final FromClauseAccess fromClauseAccess = sqlAstCreationState.getFromClauseAccess();

		final TableGroup parentTableGroup = fromClauseAccess.getTableGroup(
				fetchParent.getNavigablePath()
		);

		if ( fetchTiming == FetchTiming.IMMEDIATE && selected ) {
			fromClauseAccess.resolveTableGroup(
					fetchablePath,
					np -> {
						final SqlAstJoinType sqlAstJoinType;
						if ( isNullable ) {
							sqlAstJoinType = SqlAstJoinType.LEFT;
						}
						else {
							sqlAstJoinType = SqlAstJoinType.INNER;
						}

						final TableGroupJoin tableGroupJoin = createTableGroupJoin(
								fetchablePath,
								parentTableGroup,
								null,
								sqlAstJoinType,
								lockMode,
								creationState.getSqlAstCreationState()
						);

						return tableGroupJoin.getJoinedGroup();
					}
			);

			creationState.registerVisitedAssociationKey( foreignKeyDescriptor.getAssociationKey() );
			return new EntityFetchJoinedImpl(
					fetchParent,
					this,
					lockMode,
					true,
					fetchablePath,
					creationState
			);
		}

		//noinspection rawtypes
		final DomainResult keyResult;

		/*
			1. No JoinTable
				Model:
					EntityA{
						@ManyToOne
						EntityB b
					}

					EntityB{
						@ManyToOne
						EntityA a
					}

				Relational:
					ENTITY_A( id )
					ENTITY_B( id, entity_a_id)

				1.1 EntityA -> EntityB : as keyResult we need ENTITY_B.id
				1.2 EntityB -> EntityA : as keyResult we need ENTITY_B.entity_a_id (FK referring column)

			2. JoinTable

		 */

		boolean selectByUniqueKey;
		if ( referringPrimaryKey ) {
			// case 1.2
			keyResult = foreignKeyDescriptor.createDomainResult( fetchablePath, parentTableGroup, creationState );
			selectByUniqueKey = false;
		}
		else {
			keyResult = ((EntityPersister) getDeclaringType()).getIdentifierMapping()
					.createDomainResult( fetchablePath, parentTableGroup, null, creationState );
			// case 1.1
			selectByUniqueKey = true;
		}

		assert !selected;
		if ( fetchTiming == FetchTiming.IMMEDIATE ) {
			return new EntityFetchSelectImpl(
					fetchParent,
					this,
					isNullable,
					fetchablePath,
					keyResult,
					selectByUniqueKey,
					creationState
			);
		}

		return new EntityDelayedFetchImpl(
				fetchParent,
				this,
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
				(tableExpression) -> getEntityMappingType().containsTableReference( tableExpression ),
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

		final TableReference lhsTableReference = lhs.resolveTableReference( identifyingColumnsTableExpression );

		final TableGroupJoin tableGroupJoin = new TableGroupJoin(
				navigablePath,
				sqlAstJoinType,
				tableGroup,
				foreignKeyDescriptor.generateJoinPredicate(
						lhsTableReference,
						primaryTableReference,
						sqlAstJoinType,
						sqlExpressionResolver,
						creationContext
				)
		);

		lhs.addTableGroupJoin( tableGroupJoin );

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

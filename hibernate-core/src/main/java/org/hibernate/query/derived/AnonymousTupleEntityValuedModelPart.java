/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.derived;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.Incubating;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.ast.spi.MultiNaturalIdLoader;
import org.hibernate.loader.ast.spi.NaturalIdLoader;
import org.hibernate.mapping.IndexedConsumer;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.CompositeIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityAssociationMapping;
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityRowIdMapping;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.EntityVersionMapping;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.NaturalIdMapping;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.internal.SingleAttributeIdentifierMapping;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.metamodel.model.domain.DomainType;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.persister.entity.AttributeMappingsMap;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.persister.entity.AttributeMappingsList;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.spi.SqlAliasBase;
import org.hibernate.sql.ast.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.from.LazyTableGroup;
import org.hibernate.sql.ast.tree.from.StandardTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableGroupJoinProducer;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.predicate.ComparisonPredicate;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.type.descriptor.java.JavaType;

import static java.util.Objects.requireNonNullElse;

/**
 * @author Christian Beikov
 */
@Incubating
public class AnonymousTupleEntityValuedModelPart implements EntityValuedModelPart, EntityMappingType,
		TableGroupJoinProducer {

	private final EntityIdentifierMapping identifierMapping;
	private final DomainType<?> domainType;
	private final String componentName;
	private final EntityValuedModelPart delegate;
	private final Set<String> targetKeyPropertyNames;

	public AnonymousTupleEntityValuedModelPart(
			EntityIdentifierMapping identifierMapping,
			DomainType<?> domainType,
			String componentName,
			EntityValuedModelPart delegate) {
		this.identifierMapping = identifierMapping;
		this.domainType = domainType;
		this.componentName = componentName;
		this.delegate = delegate;
		final EntityPersister persister = ((EntityMappingType) delegate.getPartMappingType())
				.getEntityPersister();
		final Set<String> targetKeyPropertyNames = new HashSet<>();
		targetKeyPropertyNames.add( EntityIdentifierMapping.ROLE_LOCAL_NAME );
		ToOneAttributeMapping.addPrefixedPropertyNames(
				targetKeyPropertyNames,
				persister.getIdentifierPropertyName(),
				persister.getIdentifierType(),
				persister.getFactory()
		);
		this.targetKeyPropertyNames = targetKeyPropertyNames;
	}

	@Override
	public ModelPart findSubPart(String name, EntityMappingType treatTargetType) {
		if ( identifierMapping instanceof SingleAttributeIdentifierMapping ) {
			if ( ( (SingleAttributeIdentifierMapping) identifierMapping ).getAttributeName().equals( name ) ) {
				return identifierMapping;
			}
		}
		else {
			final ModelPart subPart = ( (CompositeIdentifierMapping) identifierMapping ).getPartMappingType().findSubPart(
					name,
					treatTargetType
			);
			if ( subPart != null ) {
				return subPart;
			}
		}
		return delegate.findSubPart( name, treatTargetType );
	}

	@Override
	public void visitSubParts(Consumer<ModelPart> consumer, EntityMappingType treatTargetType) {
		delegate.visitSubParts( consumer, treatTargetType );
	}

	@Override
	public MappingType getPartMappingType() {
		return this;
	}

	@Override
	public JavaType<?> getJavaType() {
		return domainType.getExpressibleJavaType();
	}

	@Override
	public String getPartName() {
		return componentName;
	}

	@Override
	public int getJdbcTypeCount() {
		return delegate.getJdbcTypeCount();
	}

	@Override
	public int getNumberOfAttributeMappings() {
		return delegate.getEntityMappingType().getNumberOfAttributeMappings();
	}

	@Override
	public AttributeMapping getAttributeMapping(int position) {
		return delegate.getEntityMappingType().getAttributeMapping( position );
	}

	@Override
	public AttributeMappingsList getAttributeMappings() {
		return delegate.getEntityMappingType().getAttributeMappings();
	}

	@Override
	public void visitAttributeMappings(Consumer<? super AttributeMapping> action) {
		delegate.getEntityMappingType().visitAttributeMappings( action );
	}

	@Override
	public Object[] getValues(Object instance) {
		return delegate.getEntityMappingType().getValues( instance );
	}

	@Override
	public Object getValue(Object instance, int position) {
		return delegate.getEntityMappingType()
				.getAttributeMapping( position )
				.getValue( instance );
	}

	@Override
	public void setValues(Object instance, Object[] resolvedValues) {
		delegate.getEntityMappingType().setValues( instance, resolvedValues );
	}

	@Override
	public void setValue(Object instance, int position, Object value) {
		delegate.getEntityMappingType()
				.getAttributeMapping( position )
				.setValue( instance, value );
	}

	@Override
	public List<JdbcMapping> getJdbcMappings() {
		final List<JdbcMapping> results = new ArrayList<>();
		forEachSelectable( (index, selection) -> results.add( selection.getJdbcMapping() ) );
		return results;
	}

	@Override
	public int forEachSelectable(SelectableConsumer consumer) {
		return forEachSelectable( 0, consumer );
	}

	@Override
	public int forEachSelectable(int offset, SelectableConsumer consumer) {
		return identifierMapping.forEachSelectable( offset, consumer );
	}

	@Override
	public JavaType<?> getMappedJavaType() {
		return delegate.getJavaType();
	}

	@Override
	public TableGroupJoin createTableGroupJoin(
			NavigablePath navigablePath,
			TableGroup lhs,
			String explicitSourceAlias,
			SqlAstJoinType requestedJoinType,
			boolean fetched,
			boolean addsPredicate,
			SqlAliasBaseGenerator aliasBaseGenerator,
			SqlExpressionResolver sqlExpressionResolver,
			FromClauseAccess fromClauseAccess,
			SqlAstCreationContext creationContext) {
		final SessionFactoryImplementor sessionFactory = creationContext.getSessionFactory();
		final SqlAstJoinType joinType = requireNonNullElse( requestedJoinType, SqlAstJoinType.INNER );

		final SqlAliasBase sqlAliasBase = aliasBaseGenerator.createSqlAliasBase( getSqlAliasStem() );
		final boolean canUseInnerJoin = joinType == SqlAstJoinType.INNER || lhs.canUseInnerJoins();
		final EntityPersister entityPersister = delegate.getEntityMappingType().getEntityPersister();
		final LazyTableGroup lazyTableGroup = new LazyTableGroup(
				canUseInnerJoin,
				navigablePath,
				fetched,
				() -> createTableGroupInternal(
						canUseInnerJoin,
						navigablePath,
						fetched,
						null,
						sqlAliasBase,
						sqlExpressionResolver,
						creationContext
				),
				(np, tableExpression) -> {
					if ( !tableExpression.isEmpty() && !entityPersister.containsTableReference( tableExpression ) ) {
						return false;
					}
					if ( navigablePath.equals( np.getParent() ) ) {
						return targetKeyPropertyNames.contains( np.getLocalName() );
					}

					final String relativePath = np.relativize( navigablePath );
					if ( relativePath == null ) {
						return false;
					}

					// Empty relative path means the navigable paths are equal,
					// in which case we allow resolving the parent table group
					return relativePath.isEmpty() || targetKeyPropertyNames.contains( relativePath );
				},
				this,
				explicitSourceAlias,
				sqlAliasBase,
				creationContext.getSessionFactory(),
				lhs
		);
		final TableGroupJoin tableGroupJoin = new TableGroupJoin(
				lazyTableGroup.getNavigablePath(),
				joinType,
				lazyTableGroup,
				null
		);

		// -----------------
		// Collect the selectable mappings for the FK key side and target side
		// As we will "resolve" the derived column references for these mappings
		// --------------

		final EntityAssociationMapping associationMapping = (EntityAssociationMapping) delegate;
		final List<SelectableMapping> keyMappings;
		final List<SelectableMapping> targetMappings;
		if ( associationMapping.isReferenceToPrimaryKey() && associationMapping.getSideNature() == ForeignKeyDescriptor.Nature.KEY ) {
			final ModelPart targetJoinModelPart = associationMapping.getForeignKeyDescriptor()
					.getPart( associationMapping.getSideNature().inverse() );
			targetMappings = new ArrayList<>( targetJoinModelPart.getJdbcTypeCount() );
			targetJoinModelPart.forEachSelectable(
					0,
					(i, selectableMapping) -> targetMappings.add( selectableMapping )
			);
			keyMappings = new ArrayList<>( targetJoinModelPart.getJdbcTypeCount() );
			associationMapping.getForeignKeyDescriptor()
					.getPart( associationMapping.getSideNature() )
					.forEachSelectable(
							0,
							(i, selectableMapping) -> keyMappings.add( selectableMapping )
					);
		}
		else {
			final ModelPart targetJoinModelPart = delegate.getEntityMappingType().getIdentifierMapping();
			targetMappings = new ArrayList<>( targetJoinModelPart.getJdbcTypeCount() );
			targetJoinModelPart.forEachSelectable(
					0,
					(i, selectableMapping) -> targetMappings.add( selectableMapping )
			);
			keyMappings = targetMappings;
		}
		final TableReference tableReference = lhs.getPrimaryTableReference();
		final List<ColumnReference> keyColumnReferences = new ArrayList<>( this.identifierMapping.getJdbcTypeCount() );
		this.identifierMapping.forEachSelectable(
				(i, selectableMapping) -> {
					// It is important to resolve the sql expression here,
					// as this selectableMapping is the "derived" one.
					// We want to register the expression under the key of the original mapping
					// which leads to this expression being used for a possible domain result
					keyColumnReferences.add(
							(ColumnReference) sqlExpressionResolver.resolveSqlExpression(
									SqlExpressionResolver.createColumnReferenceKey(
											tableReference,
											keyMappings.get( i ).getSelectionExpression()
									),
									state -> new ColumnReference(
											tableReference,
											selectableMapping,
											sessionFactory
									)
							)
					);
				}
		);
		if ( keyMappings != targetMappings ) {
			this.identifierMapping.forEachSelectable(
					(i, selectableMapping) -> {
						// It is important to resolve the sql expression here,
						// as this selectableMapping is the "derived" one.
						// We want to register the expression under the key of the original mapping
						// which leads to this expression being used for a possible domain result
						sqlExpressionResolver.resolveSqlExpression(
								SqlExpressionResolver.createColumnReferenceKey(
										tableReference,
										targetMappings.get( i ).getSelectionExpression()
								),
								state -> new ColumnReference(
										tableReference,
										selectableMapping,
										sessionFactory
								)
						);
					}
			);
		}
		lazyTableGroup.setTableGroupInitializerCallback(
				tg -> {
					this.identifierMapping.forEachSelectable(
							(i, selectableMapping) -> {
								final SelectableMapping targetMapping = targetMappings.get( i );
								final TableReference targetTableReference = tg.resolveTableReference(
										null,
										targetMapping.getContainingTableExpression(),
										false
								);
								tableGroupJoin.applyPredicate(
										new ComparisonPredicate(
												keyColumnReferences.get( i ),
												ComparisonOperator.EQUAL,
												new ColumnReference(
														targetTableReference,
														targetMapping,
														sessionFactory
												)
										)
								);
							}
					);
				}
		);
		return tableGroupJoin;
	}

	public TableGroup createTableGroupInternal(
			boolean canUseInnerJoins,
			NavigablePath navigablePath,
			boolean fetched,
			String sourceAlias,
			final SqlAliasBase sqlAliasBase,
			SqlExpressionResolver sqlExpressionResolver,
			SqlAstCreationContext creationContext) {
		final EntityMappingType entityMappingType = delegate.getEntityMappingType();
		final TableReference primaryTableReference = entityMappingType.createPrimaryTableReference(
				sqlAliasBase,
				sqlExpressionResolver,
				creationContext
		);

		return new StandardTableGroup(
				canUseInnerJoins,
				navigablePath,
				this,
				fetched,
				sourceAlias,
				primaryTableReference,
				true,
				sqlAliasBase,
				entityMappingType::containsTableReference,
				(tableExpression, tg) -> entityMappingType.createTableReferenceJoin(
						tableExpression,
						sqlAliasBase,
						primaryTableReference,
						sqlExpressionResolver,
						creationContext
				),
				creationContext.getSessionFactory()
		);
	}

	@Override
	public TableGroup createRootTableGroupJoin(
			NavigablePath navigablePath,
			TableGroup lhs,
			String explicitSourceAlias,
			SqlAstJoinType sqlAstJoinType,
			boolean fetched,
			Consumer<Predicate> predicateConsumer,
			SqlAliasBaseGenerator aliasBaseGenerator,
			SqlExpressionResolver sqlExpressionResolver,
			FromClauseAccess fromClauseAccess,
			SqlAstCreationContext creationContext) {
		return ( (TableGroupJoinProducer) delegate ).createRootTableGroupJoin(
				navigablePath,
				lhs,
				explicitSourceAlias,
				sqlAstJoinType,
				fetched,
				predicateConsumer,
				aliasBaseGenerator,
				sqlExpressionResolver,
				fromClauseAccess,
				creationContext
		);
	}

	@Override
	public String getSqlAliasStem() {
		return getPartName();
	}

	@Override
	public int getNumberOfFetchables() {
		return delegate.getNumberOfFetchables();
	}

	@Override
	public NavigableRole getNavigableRole() {
		return delegate.getNavigableRole();
	}

	@Override
	public EntityMappingType findContainingEntityMapping() {
		return this;
	}

	@Override
	public <T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
		return delegate.createDomainResult( navigablePath, tableGroup, resultVariable, creationState );
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState) {
		identifierMapping.applySqlSelections( navigablePath, tableGroup, creationState );
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState,
			BiConsumer<SqlSelection, JdbcMapping> selectionConsumer) {
		identifierMapping.applySqlSelections( navigablePath, tableGroup, creationState, selectionConsumer );
	}

	@Override
	public void breakDownJdbcValues(
			Object domainValue,
			JdbcValueConsumer valueConsumer,
			SharedSessionContractImplementor session) {
		delegate.breakDownJdbcValues( domainValue, valueConsumer, session );
	}

	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		return delegate.disassemble( value, session );
	}

	@Override
	public int forEachDisassembledJdbcValue(
			Object value,
			Clause clause,
			int offset,
			JdbcValuesConsumer valuesConsumer,
			SharedSessionContractImplementor session) {
		return delegate.forEachDisassembledJdbcValue( value, clause, offset, valuesConsumer, session );
	}

	@Override
	public int forEachJdbcValue(
			Object value,
			Clause clause,
			int offset,
			JdbcValuesConsumer consumer,
			SharedSessionContractImplementor session) {
		return delegate.forEachJdbcValue( value, clause, offset, consumer, session );
	}

	@Override
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		return delegate.forEachJdbcType( offset, action );
	}

	@Override
	public EntityPersister getEntityPersister() {
		return delegate.getEntityMappingType().getEntityPersister();
	}

	@Override
	public String getEntityName() {
		return delegate.getEntityMappingType().getEntityName();
	}

	@Override
	public void visitQuerySpaces(Consumer<String> querySpaceConsumer) {
		delegate.getEntityMappingType().visitQuerySpaces( querySpaceConsumer );
	}

	@Override
	public AttributeMapping findDeclaredAttributeMapping(String name) {
		return delegate.getEntityMappingType().findDeclaredAttributeMapping( name );
	}

	@Override
	public AttributeMappingsMap getDeclaredAttributeMappings() {
		return delegate.getEntityMappingType().getDeclaredAttributeMappings();
	}

	@Override
	public void visitDeclaredAttributeMappings(Consumer<? super AttributeMapping> action) {
		delegate.getEntityMappingType().visitDeclaredAttributeMappings( action );
	}

	@Override
	public EntityIdentifierMapping getIdentifierMapping() {
		return delegate.getEntityMappingType().getIdentifierMapping();
	}

	@Override
	public EntityDiscriminatorMapping getDiscriminatorMapping() {
		return delegate.getEntityMappingType().getDiscriminatorMapping();
	}

	@Override
	public Object getDiscriminatorValue() {
		return delegate.getEntityMappingType().getDiscriminatorValue();
	}

	@Override
	public String getSubclassForDiscriminatorValue(Object value) {
		return delegate.getEntityMappingType().getSubclassForDiscriminatorValue( value );
	}

	@Override
	public EntityVersionMapping getVersionMapping() {
		return delegate.getEntityMappingType().getVersionMapping();
	}

	@Override
	public NaturalIdMapping getNaturalIdMapping() {
		return delegate.getEntityMappingType().getNaturalIdMapping();
	}

	@Override
	public EntityRowIdMapping getRowIdMapping() {
		return delegate.getEntityMappingType().getRowIdMapping();
	}

	@Override
	public void visitConstraintOrderedTables(ConstraintOrderedTableConsumer consumer) {
		delegate.getEntityMappingType().visitConstraintOrderedTables( consumer );
	}

	@Override
	public NaturalIdLoader<?> getNaturalIdLoader() {
		return delegate.getEntityMappingType().getNaturalIdLoader();
	}

	@Override
	public MultiNaturalIdLoader<?> getMultiNaturalIdLoader() {
		return delegate.getEntityMappingType().getMultiNaturalIdLoader();
	}

	@Override
	public EntityMappingType getEntityMappingType() {
		return this;
	}

	@Override
	public SqlAstJoinType getDefaultSqlAstJoinType(TableGroup parentTableGroup) {
		return delegate instanceof TableGroupJoinProducer
				? ( (TableGroupJoinProducer) delegate ).getDefaultSqlAstJoinType( parentTableGroup )
				: null;
	}

	@Override
	public boolean isSimpleJoinPredicate(Predicate predicate) {
		return delegate instanceof TableGroupJoinProducer
				? ( (TableGroupJoinProducer) delegate ).isSimpleJoinPredicate( predicate )
				: false;
	}
}

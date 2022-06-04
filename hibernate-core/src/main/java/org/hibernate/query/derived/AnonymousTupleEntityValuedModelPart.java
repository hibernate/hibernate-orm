/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.derived;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
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
import org.hibernate.metamodel.mapping.EntityDiscriminatorMapping;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.EntityRowIdMapping;
import org.hibernate.metamodel.mapping.EntityValuedModelPart;
import org.hibernate.metamodel.mapping.EntityVersionMapping;
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
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.sqm.ComparisonOperator;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
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

	public AnonymousTupleEntityValuedModelPart(
			EntityIdentifierMapping identifierMapping,
			DomainType<?> domainType,
			String componentName,
			EntityValuedModelPart delegate) {
		this.identifierMapping = identifierMapping;
		this.domainType = domainType;
		this.componentName = componentName;
		this.delegate = delegate;
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
	public List<AttributeMapping> getAttributeMappings() {
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
		// Need to create a root table group and join predicate separately instead of a table group join directly,
		// because the column names on the "key-side" have different names
		final TableGroup tableGroup = ( (TableGroupJoinProducer) delegate ).createRootTableGroupJoin(
				navigablePath,
				lhs,
				explicitSourceAlias,
				joinType,
				fetched,
				null,
				aliasBaseGenerator,
				sqlExpressionResolver,
				fromClauseAccess,
				creationContext
		);
		final TableGroupJoin tableGroupJoin = new TableGroupJoin(
				tableGroup.getNavigablePath(),
				joinType,
				tableGroup,
				null
		);

		final List<SelectableMapping> keyMappings;
		final List<SelectableMapping> targetMappings;
		if ( delegate instanceof ToOneAttributeMapping ) {
			final ToOneAttributeMapping toOneAttributeMapping = (ToOneAttributeMapping) this.delegate;
			final ModelPart targetJoinModelPart = toOneAttributeMapping.getForeignKeyDescriptor()
					.getPart( toOneAttributeMapping.getSideNature().inverse() );
			targetMappings = new ArrayList<>( targetJoinModelPart.getJdbcTypeCount() );
			targetJoinModelPart.forEachSelectable(
					0,
					(i, selectableMapping) -> targetMappings.add( selectableMapping )
			);
			keyMappings = new ArrayList<>( targetJoinModelPart.getJdbcTypeCount() );
			toOneAttributeMapping.getForeignKeyDescriptor()
					.getPart( toOneAttributeMapping.getSideNature() )
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
		this.identifierMapping.forEachSelectable(
				(i, selectableMapping) -> {
					final SelectableMapping targetMapping = targetMappings.get( i );
					final TableReference targetTableReference = tableGroup.resolveTableReference(
							null,
							targetMapping.getContainingTableExpression(),
							false
					);
					tableGroupJoin.applyPredicate(
							new ComparisonPredicate(
									// It is important to resolve the sql expression here,
									// as this selectableMapping is the "derived" one.
									// We want to register the expression under the key of the original mapping
									// which leads to this expression being used for a possible domain result
									sqlExpressionResolver.resolveSqlExpression(
											SqlExpressionResolver.createColumnReferenceKey(
													tableReference,
													keyMappings.get( i ).getSelectionExpression()
											),
											state -> new ColumnReference(
													tableReference,
													selectableMapping,
													sessionFactory
											)
									),
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
		return tableGroupJoin;
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
	public Collection<AttributeMapping> getDeclaredAttributeMappings() {
		return delegate.getEntityMappingType().getDeclaredAttributeMappings();
	}

	@Override
	public void visitDeclaredAttributeMappings(Consumer<? super AttributeMapping> action) {
		delegate.getEntityMappingType().visitDeclaredAttributeMappings( action );
	}

	@Override
	public EntityIdentifierMapping getIdentifierMapping() {
		return identifierMapping;
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

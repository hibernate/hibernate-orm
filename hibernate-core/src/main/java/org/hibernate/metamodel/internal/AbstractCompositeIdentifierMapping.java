/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.internal;

import java.util.List;
import java.util.function.Consumer;

import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.CollectionHelper;
import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMappings;
import org.hibernate.metamodel.mapping.CompositeIdentifierMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.SingularAttributeMapping;
import org.hibernate.metamodel.mapping.StateArrayContributorMetadataAccess;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.query.NavigablePath;
import org.hibernate.query.sqm.sql.SqmToSqlAstConverter;
import org.hibernate.sql.ast.Clause;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.tree.expression.ColumnReference;
import org.hibernate.sql.ast.tree.expression.Expression;
import org.hibernate.sql.ast.tree.expression.SqlTuple;
import org.hibernate.sql.ast.tree.from.CompositeTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.from.TableReference;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchOptions;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.embeddable.EmbeddableValuedFetchable;
import org.hibernate.sql.results.graph.embeddable.internal.EmbeddableFetchImpl;
import org.hibernate.sql.results.graph.embeddable.internal.EmbeddableResultImpl;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * Base implementation for composite identifier mappings
 *
 * @author Andrea Boriero
 */
public abstract class AbstractCompositeIdentifierMapping
		implements CompositeIdentifierMapping, EmbeddableValuedFetchable, FetchOptions {
	private final NavigableRole navigableRole;
	private final String tableExpression;

	private final StateArrayContributorMetadataAccess attributeMetadataAccess;

	private final EntityMappingType entityMapping;
	private final EmbeddableMappingType embeddableDescriptor;

	private final SessionFactoryImplementor sessionFactory;

	public AbstractCompositeIdentifierMapping(
			StateArrayContributorMetadataAccess attributeMetadataAccess,
			EmbeddableMappingType embeddableDescriptor,
			EntityMappingType entityMapping,
			String tableExpression,
			SessionFactoryImplementor sessionFactory) {
		this.attributeMetadataAccess = attributeMetadataAccess;
		this.embeddableDescriptor = embeddableDescriptor;
		this.entityMapping = entityMapping;
		this.tableExpression = tableExpression;
		this.sessionFactory = sessionFactory;

		this.navigableRole = entityMapping.getNavigableRole()
				.appendContainer( EntityIdentifierMapping.ROLE_LOCAL_NAME );
	}

	/**
	 * Does the identifier have a corresponding EmbeddableId or IdClass?
	 *
	 * @return false if there is not an IdCass or an EmbeddableId
	 */
	public boolean hasContainingClass(){
		return true;
	}

	@Override
	public EmbeddableMappingType getMappedType() {
		return embeddableDescriptor;
	}

	@Override
	public EmbeddableMappingType getPartMappingType() {
		return getEmbeddableTypeDescriptor();
	}

	@Override
	public JavaType<?> getJavaTypeDescriptor() {
		return getEmbeddableTypeDescriptor().getMappedJavaTypeDescriptor();
	}

	@Override
	public EmbeddableMappingType getEmbeddableTypeDescriptor() {
		return embeddableDescriptor;
	}

	@Override
	public String getContainingTableExpression() {
		return tableExpression;
	}

	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Override
	public int forEachSelectable(int offset, SelectableConsumer consumer) {
		int span = 0;
		final List<SingularAttributeMapping> attributes = getAttributes();
		for ( int i = 0; i < attributes.size(); i++ ) {
			final SingularAttributeMapping attribute = attributes.get( i );
			if ( attribute instanceof ToOneAttributeMapping ) {
				final ToOneAttributeMapping associationAttributeMapping = (ToOneAttributeMapping) attribute;
				span += associationAttributeMapping.getForeignKeyDescriptor().visitKeySelectables(
						span + offset,
						consumer
				);
			}
			else {
				span += attribute.forEachSelectable( span + offset, consumer );
			}
		}
		return span;
	}

	@Override
	public Fetch generateFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			FetchTiming fetchTiming,
			boolean selected,
			String resultVariable,
			DomainResultCreationState creationState) {
		return new EmbeddableFetchImpl(
				fetchablePath,
				this,
				fetchParent,
				fetchTiming,
				selected,
				attributeMetadataAccess.resolveAttributeMetadata( null ).isNullable(),
				creationState
		);
	}

	@Override
	public TableGroupJoin createTableGroupJoin(
			NavigablePath navigablePath,
			TableGroup lhs,
			String explicitSourceAlias,
			SqlAstJoinType sqlAstJoinType,
			boolean fetched,
			boolean nested,
			SqlAliasBaseGenerator aliasBaseGenerator,
			SqlExpressionResolver sqlExpressionResolver,
			SqlAstCreationContext creationContext) {
		final TableGroup tableGroup = createRootTableGroupJoin(
				navigablePath,
				lhs,
				explicitSourceAlias,
				sqlAstJoinType,
				fetched,
				null,
				aliasBaseGenerator,
				sqlExpressionResolver,
				creationContext
		);

		final TableGroupJoin join = new TableGroupJoin( navigablePath, SqlAstJoinType.LEFT, tableGroup, null );
		if ( nested ) {
			lhs.addNestedTableGroupJoin( join );
		}
		else {
			lhs.addTableGroupJoin( join );
		}

		return join;
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
			SqlAstCreationContext creationContext) {
		return new CompositeTableGroup( navigablePath, this, lhs, fetched );
	}

	@Override
	public ModelPart findSubPart(String name, EntityMappingType treatTargetType) {
		return embeddableDescriptor.findSubPart( name, treatTargetType );
	}

	@Override
	public void visitSubParts(
			Consumer<ModelPart> consumer,
			EntityMappingType treatTargetType) {
		embeddableDescriptor.visitSubParts( consumer, treatTargetType );
	}

	@Override
	public int forEachJdbcValue(
			Object value,
			Clause clause,
			int offset,
			JdbcValuesConsumer valuesConsumer,
			SharedSessionContractImplementor session) {
		int span = 0;
		final List<AttributeMapping> attributeMappings = getEmbeddableTypeDescriptor().getAttributeMappings();
		for ( int i = 0; i < attributeMappings.size(); i++ ) {
			final AttributeMapping attributeMapping = attributeMappings.get( i );
			final Object o = attributeMapping.getPropertyAccess().getGetter().get( value );
			if ( attributeMapping instanceof ToOneAttributeMapping ) {
				final ToOneAttributeMapping toOneAttributeMapping = (ToOneAttributeMapping) attributeMapping;
				final ForeignKeyDescriptor fkDescriptor = toOneAttributeMapping.getForeignKeyDescriptor();
				final Object identifier = fkDescriptor.getAssociationKeyFromSide(
						o,
						toOneAttributeMapping.getSideNature().inverse(),
						session
				);
				span += fkDescriptor.forEachJdbcValue(
						identifier,
						clause,
						span + offset,
						valuesConsumer,
						session
				);
			}
			else {
				span += attributeMapping.forEachJdbcValue( o, clause, span + offset, valuesConsumer, session );
			}
		}
		return span;
	}

	@Override
	public SqlTuple toSqlExpression(
			TableGroup tableGroup,
			Clause clause,
			SqmToSqlAstConverter walker,
			SqlAstCreationState sqlAstCreationState) {
		final SelectableMappings selectableMappings = getEmbeddableTypeDescriptor();
		final List<ColumnReference> columnReferences = CollectionHelper.arrayList( selectableMappings.getJdbcTypeCount() );
		final NavigablePath navigablePath = tableGroup.getNavigablePath().append( getNavigableRole().getNavigableName() );
		final TableReference defaultTableReference = tableGroup.resolveTableReference( navigablePath, getContainingTableExpression() );
		getEmbeddableTypeDescriptor().forEachSelectable(
				(columnIndex, selection) -> {
					final TableReference tableReference = selection.getContainingTableExpression().equals( defaultTableReference.getTableExpression() )
							? defaultTableReference
							: tableGroup.resolveTableReference( navigablePath, selection.getContainingTableExpression() );
					final Expression columnReference = sqlAstCreationState.getSqlExpressionResolver()
							.resolveSqlExpression(
									SqlExpressionResolver.createColumnReferenceKey(
											tableReference,
											selection.getSelectionExpression()
									),
									sqlAstProcessingState -> new ColumnReference(
											tableReference.getIdentificationVariable(),
											selection,
											sqlAstCreationState.getCreationContext().getSessionFactory()
									)
							);

					columnReferences.add( (ColumnReference) columnReference );
				}
		);

		return new SqlTuple( columnReferences, this );
	}

	@Override
	public <T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
		return new EmbeddableResultImpl<>(
				navigablePath,
				this,
				resultVariable,
				creationState
		);
	}

	@Override
	public Object instantiate() {
		return getEntityMapping().getRepresentationStrategy().getInstantiator().instantiate( sessionFactory );
	}

	@Override
	public EntityMappingType findContainingEntityMapping() {
		return entityMapping;
	}

	@Override
	public FetchOptions getMappedFetchOptions() {
		return this;
	}

	@Override
	public FetchStyle getStyle() {
		return FetchStyle.JOIN;
	}

	@Override
	public FetchTiming getTiming() {
		return FetchTiming.IMMEDIATE;
	}

	protected EntityMappingType getEntityMapping() {
		return entityMapping;
	}

}

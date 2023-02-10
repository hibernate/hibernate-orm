/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping.internal;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.IndexedConsumer;
import org.hibernate.mapping.Any;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.DiscriminatedAssociationModelPart;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.spi.FromClauseAccess;
import org.hibernate.sql.ast.spi.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.spi.SqlAstCreationContext;
import org.hibernate.sql.ast.spi.SqlExpressionResolver;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.from.StandardVirtualTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchOptions;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.type.AnyType;
import org.hibernate.type.descriptor.java.JavaType;

/**
 * @author Steve Ebersole
 */
public class DiscriminatedCollectionPart implements DiscriminatedAssociationModelPart, CollectionPart {
	private final Nature nature;

	private final NavigableRole partRole;
	private final CollectionPersister collectionDescriptor;
	private final DiscriminatedAssociationMapping discriminatorMapping;

	public DiscriminatedCollectionPart(
			Nature nature,
			CollectionPersister collectionDescriptor,
			JavaType<Object> baseAssociationJtd,
			Any bootValueMapping,
			AnyType anyType,
			MappingModelCreationProcess creationProcess) {
		this.nature = nature;
		this.partRole = collectionDescriptor.getNavigableRole().append( nature.getName() );
		this.collectionDescriptor = collectionDescriptor;

		this.discriminatorMapping = DiscriminatedAssociationMapping.from(
				partRole,
				baseAssociationJtd,
				this,
				anyType,
				bootValueMapping,
				creationProcess
		);
	}

	@Override
	public Nature getNature() {
		return nature;
	}

	@Override
	public BasicValuedModelPart getDiscriminatorPart() {
		return discriminatorMapping.getDiscriminatorPart();
	}

	@Override
	public BasicValuedModelPart getKeyPart() {
		return discriminatorMapping.getKeyPart();
	}

	@Override
	public EntityMappingType resolveDiscriminatorValue(Object discriminatorValue) {
		return discriminatorMapping.resolveDiscriminatorValueToEntityMapping( discriminatorValue );
	}

	@Override
	public Object resolveDiscriminatorForEntityType(EntityMappingType entityMappingType) {
		return discriminatorMapping.resolveDiscriminatorValueToEntityMapping( entityMappingType );
	}

	@Override
	public int forEachSelectable(int offset, SelectableConsumer consumer) {
		discriminatorMapping.getDiscriminatorPart().forEachSelectable( offset, consumer );
		discriminatorMapping.getKeyPart().forEachSelectable( offset + 1, consumer );

		return 2;
	}

	@Override
	public String getFetchableName() {
		return nature.getName();
	}

	@Override
	public int getFetchableKey() {
		return nature == Nature.INDEX || !collectionDescriptor.hasIndex() ? 0 : 1;
	}

	@Override
	public FetchOptions getMappedFetchOptions() {
		return discriminatorMapping;
	}

	@Override
	public boolean hasPartitionedSelectionMapping() {
		return discriminatorMapping.getDiscriminatorPart().isPartitioned()
				|| discriminatorMapping.getKeyPart().isPartitioned();
	}

	@Override
	public String toString() {
		return "DiscriminatedCollectionPart(" + getNavigableRole() + ")@" + System.identityHashCode( this );
	}

	@Override
	public Fetch generateFetch(
			FetchParent fetchParent,
			NavigablePath fetchablePath,
			FetchTiming fetchTiming,
			boolean selected,
			String resultVariable,
			DomainResultCreationState creationState) {
		return discriminatorMapping.generateFetch(
				fetchParent,
				fetchablePath,
				fetchTiming,
				selected,
				resultVariable,
				creationState
		);
	}

	@Override
	public <T> DomainResult<T> createDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
		return discriminatorMapping.createDomainResult(
				navigablePath,
				tableGroup,
				resultVariable,
				creationState
		);
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState) {
		discriminatorMapping.getDiscriminatorPart().applySqlSelections( navigablePath, tableGroup, creationState );
	}

	@Override
	public void applySqlSelections(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			DomainResultCreationState creationState,
			BiConsumer<SqlSelection, JdbcMapping> selectionConsumer) {
		discriminatorMapping.getDiscriminatorPart().applySqlSelections( navigablePath, tableGroup, creationState, selectionConsumer );
	}

	@Override
	public MappingType getPartMappingType() {
		return discriminatorMapping;
	}

	@Override
	public JavaType<?> getJavaType() {
		return discriminatorMapping.getJavaType();
	}

	@Override
	public MappingType getMappedType() {
		return getPartMappingType();
	}

	@Override
	public JavaType<?> getExpressibleJavaType() {
		return getJavaType();
	}

	@Override
	public NavigableRole getNavigableRole() {
		return partRole;
	}

	@Override
	public EntityMappingType findContainingEntityMapping() {
		return collectionDescriptor.getAttributeMapping().findContainingEntityMapping();
	}

	@Override
	public ModelPart findSubPart(String name, EntityMappingType treatTargetType) {
		return discriminatorMapping.findSubPart( name, treatTargetType );
	}

	@Override
	public void forEachSubPart(IndexedConsumer<ModelPart> consumer, EntityMappingType treatTarget) {
		consumer.accept( 0, getDiscriminatorPart() );
		consumer.accept( 1, getKeyPart() );
	}

	@Override
	public void visitSubParts(Consumer<ModelPart> consumer, EntityMappingType treatTargetType) {
		consumer.accept( getDiscriminatorPart() );
		consumer.accept( getKeyPart() );
	}

	@Override
	public int getNumberOfFetchables() {
		return 2;
	}

	@Override
	public Fetchable getFetchable(int position) {
		switch ( position ) {
			case 0:
				return getDiscriminatorPart();
			case 1:
				return getKeyPart();
		}
		throw new IndexOutOfBoundsException(position);
	}

	@Override
	public String getContainingTableExpression() {
		return getDiscriminatorPart().getContainingTableExpression();
	}

	@Override
	public int getJdbcTypeCount() {
		return getDiscriminatorPart().getJdbcTypeCount() + getKeyPart().getJdbcTypeCount();
	}

	@Override
	public SelectableMapping getSelectable(int columnIndex) {
		return getDiscriminatorPart().getSelectable( columnIndex );
	}

	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		return discriminatorMapping.getDiscriminatorPart().disassemble( value, session );
	}

	@Override
	public <X, Y> int forEachDisassembledJdbcValue(
			Object value,
			int offset,
			X x,
			Y y,
			JdbcValuesBiConsumer<X, Y> valuesConsumer,
			SharedSessionContractImplementor session) {
		return discriminatorMapping.getDiscriminatorPart().forEachDisassembledJdbcValue(
				value,
				offset,
				x, y, valuesConsumer,
				session
		);
	}

	@Override
	public void breakDownJdbcValues(Object domainValue, JdbcValueConsumer valueConsumer, SharedSessionContractImplementor session) {
		discriminatorMapping.breakDownJdbcValues( domainValue, valueConsumer, session );
	}

	@Override
	public void decompose(Object domainValue, JdbcValueConsumer valueConsumer, SharedSessionContractImplementor session) {
		discriminatorMapping.decompose( domainValue, valueConsumer, session );
	}

	@Override
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		int span = getDiscriminatorPart().forEachJdbcType( offset, action );
		return span + getKeyPart().forEachJdbcType( offset + span, action );
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
		final SqlAstJoinType joinType;
		if ( requestedJoinType == null ) {
			joinType = SqlAstJoinType.INNER;
		}
		else {
			joinType = requestedJoinType;
		}
		final TableGroup tableGroup = createRootTableGroupJoin(
				navigablePath,
				lhs,
				explicitSourceAlias,
				requestedJoinType,
				fetched,
				null,
				aliasBaseGenerator,
				sqlExpressionResolver,
				fromClauseAccess,
				creationContext
		);

		return new TableGroupJoin( navigablePath, joinType, tableGroup );
	}

	@Override
	public TableGroup createRootTableGroupJoin(
			NavigablePath navigablePath,
			TableGroup lhs,
			String explicitSourceAlias,
			SqlAstJoinType requestedJoinType,
			boolean fetched,
			Consumer<Predicate> predicateConsumer,
			SqlAliasBaseGenerator aliasBaseGenerator,
			SqlExpressionResolver sqlExpressionResolver,
			FromClauseAccess fromClauseAccess,
			SqlAstCreationContext creationContext) {
		return new StandardVirtualTableGroup(
				navigablePath,
				this,
				lhs,
				fetched
		);
	}

	@Override
	public SqlAstJoinType getDefaultSqlAstJoinType(TableGroup parentTableGroup) {
		return SqlAstJoinType.LEFT;
	}

	@Override
	public String getSqlAliasStem() {
		return collectionDescriptor.getAttributeMapping().getSqlAliasStem();
	}
}

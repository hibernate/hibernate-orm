package org.hibernate.metamodel.mapping.internal;

import jakarta.annotation.Nonnull;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.cache.MutableCacheKeyBuilder;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.spi.IndexedConsumer;
import org.hibernate.mapping.Any;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.CollectionPart;
import org.hibernate.metamodel.mapping.DiscriminatedAssociationModelPart;
import org.hibernate.metamodel.mapping.DiscriminatorMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.query.from.SqlAstJoinType;
import org.hibernate.sql.ast.spi.creation.SqlAliasBase;
import org.hibernate.sql.ast.spi.creation.SqlAstCreationState;
import org.hibernate.sql.ast.spi.query.select.SqlSelection;
import org.hibernate.sql.ast.spi.query.from.TableGroup;
import org.hibernate.sql.ast.spi.query.from.TableGroupJoin;
import org.hibernate.sql.ast.spi.query.predicate.Predicate;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchOptions;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.type.AnyType;
import org.hibernate.type.descriptor.java.JavaType;

import jakarta.annotation.Nullable;

import static java.util.Objects.requireNonNullElse;

/**
 * @author Steve Ebersole
 */
public class DiscriminatedCollectionPart implements DiscriminatedAssociationModelPart, CollectionPart {
	private final Nature nature;

	private final NavigableRole partRole;
	private final CollectionPersister collectionDescriptor;
	private final DiscriminatedAssociationMapping associationMapping;

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
		this.associationMapping = DiscriminatedAssociationMapping.from(
				partRole,
				baseAssociationJtd,
				this,
				anyType,
				bootValueMapping,
				creationProcess
		);
	}

	@Nonnull
	@Override
	public Nature getNature() {
		return nature;
	}

	@Nonnull
	@Override
	public PluralAttributeMapping getCollectionAttribute() {
		return collectionDescriptor.getAttributeMapping();
	}

	@Nonnull
	@Override
	public DiscriminatorMapping getDiscriminatorMapping() {
		return associationMapping.getDiscriminatorMapping();
	}

	@Override
	public void applyDiscriminator(@Nonnull Consumer<Predicate> predicateConsumer, @Nullable String alias, @Nonnull TableGroup tableGroup, @Nonnull SqlAstCreationState creationState) {
		throw new UnsupportedOperationException();
	}

	@Nonnull
	@Override
	public BasicValuedModelPart getKeyPart() {
		return associationMapping.getKeyPart();
	}

	@Nullable
	@Override
	public EntityMappingType resolveDiscriminatorValue(@Nullable Object discriminatorValue) {
		return associationMapping.resolveDiscriminatorValueToEntityMapping( discriminatorValue );
	}

	@Nullable
	@Override
	public Object resolveDiscriminatorForEntityType(@Nonnull EntityMappingType entityMappingType) {
		return associationMapping.resolveDiscriminatorValueToEntityMapping( entityMappingType );
	}

	@Override
	public int forEachSelectable(int offset, @Nonnull SelectableConsumer consumer) {
		associationMapping.getDiscriminatorMapping().forEachSelectable( offset, consumer );
		associationMapping.getKeyPart().forEachSelectable( offset + 1, consumer );
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
		return associationMapping;
	}

	@Override
	public boolean hasPartitionedSelectionMapping() {
		return associationMapping.getDiscriminatorMapping().isPartitioned()
			|| associationMapping.getKeyPart().isPartitioned();
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
		return associationMapping.generateFetch(
				fetchParent,
				fetchablePath,
				fetchTiming,
				selected,
				resultVariable,
				creationState
		);
	}

	@Nonnull
	@Override
	public <T> DomainResult<T> createDomainResult(
			@Nonnull NavigablePath navigablePath,
			@Nonnull TableGroup tableGroup,
			@Nullable String resultVariable,
			@Nonnull DomainResultCreationState creationState) {
		return associationMapping.createDomainResult(
				navigablePath,
				tableGroup,
				resultVariable,
				creationState
		);
	}

	@Override
	public void applySqlSelections(
			@Nonnull NavigablePath navigablePath,
			@Nonnull TableGroup tableGroup,
			@Nonnull DomainResultCreationState creationState) {
		associationMapping.getDiscriminatorMapping().applySqlSelections( navigablePath, tableGroup, creationState );
	}

	@Override
	public void applySqlSelections(
			@Nonnull NavigablePath navigablePath,
			@Nonnull TableGroup tableGroup,
			@Nonnull DomainResultCreationState creationState,
			@Nonnull BiConsumer<SqlSelection, JdbcMapping> selectionConsumer) {
		associationMapping.getDiscriminatorMapping().applySqlSelections( navigablePath, tableGroup, creationState, selectionConsumer );
	}

	@Nonnull
	@Override
	public MappingType getPartMappingType() {
		return associationMapping;
	}

	@Nonnull
	@Override
	public JavaType<?> getJavaType() {
		return associationMapping.getJavaType();
	}

	@Nonnull
	@Override
	public MappingType getMappedType() {
		return getPartMappingType();
	}

	@Nonnull
	@Override
	public JavaType<?> getExpressibleJavaType() {
		return getJavaType();
	}

	@Nonnull
	@Override
	public NavigableRole getNavigableRole() {
		return partRole;
	}

	@Nullable
	@Override
	public EntityMappingType findContainingEntityMapping() {
		return collectionDescriptor.getAttributeMapping().findContainingEntityMapping();
	}

	@Nullable
	@Override
	public ModelPart findSubPart(@Nonnull String name, @Nullable EntityMappingType treatTargetType) {
		return associationMapping.findSubPart( name, treatTargetType );
	}

	@Override
	public void forEachSubPart(@Nonnull IndexedConsumer<ModelPart> consumer, @Nullable EntityMappingType treatTarget) {
		consumer.accept( 0, getDiscriminatorMapping() );
		consumer.accept( 1, getKeyPart() );
	}

	@Override
	public void visitSubParts(@Nonnull Consumer<ModelPart> consumer, @Nullable EntityMappingType treatTargetType) {
		consumer.accept( getDiscriminatorMapping() );
		consumer.accept( getKeyPart() );
	}

	@Override
	public int getNumberOfFetchables() {
		return 2;
	}

	@Override
	public Fetchable getFetchable(int position) {
		return switch ( position ) {
			case 0 -> getDiscriminatorMapping();
			case 1 -> getKeyPart();
			default -> throw new IndexOutOfBoundsException( position );
		};
	}

	@Nonnull
	@Override
	public String getContainingTableExpression() {
		return getDiscriminatorMapping().getContainingTableExpression();
	}

	@Override
	public int getJdbcTypeCount() {
		return getDiscriminatorMapping().getJdbcTypeCount() + getKeyPart().getJdbcTypeCount();
	}

	@Nonnull
	@Override
	public JdbcMapping getJdbcMapping(final int index) {
		final int base = getDiscriminatorMapping().getJdbcTypeCount();
		return index >= base
				? getKeyPart().getJdbcMapping( index - base )
				: getDiscriminatorMapping().getJdbcMapping( index );
	}

	@Nonnull
	@Override
	public SelectableMapping getSelectable(int columnIndex) {
		return getDiscriminatorMapping().getSelectable( columnIndex );
	}

	@Nullable
	@Override
	public Object disassemble(@Nullable Object value, @Nullable SharedSessionContractImplementor session) {
		return associationMapping.getDiscriminatorMapping().disassemble( value, session );
	}

	@Override
	public void addToCacheKey(@Nonnull MutableCacheKeyBuilder cacheKey, @Nullable Object value, @Nullable SharedSessionContractImplementor session) {
		associationMapping.getDiscriminatorMapping().addToCacheKey( cacheKey, value, session );
	}

	@Override
	public <X, Y> int forEachDisassembledJdbcValue(
			@Nullable Object value,
			int offset,
			@Nullable X x,
			@Nullable Y y,
			@Nonnull JdbcValuesBiConsumer<X, Y> valuesConsumer,
			@Nullable SharedSessionContractImplementor session) {
		return associationMapping.getDiscriminatorMapping().forEachDisassembledJdbcValue(
				value,
				offset,
				x,
				y,
				valuesConsumer,
				session
		);
	}

	@Override
	public <X, Y> int breakDownJdbcValues(
			@Nullable Object domainValue,
			int offset,
			@Nullable X x,
			@Nullable Y y,
			@Nonnull JdbcValueBiConsumer<X, Y> valueConsumer,
			@Nullable SharedSessionContractImplementor session) {
		return associationMapping.breakDownJdbcValues( offset, x, y, domainValue, valueConsumer, session );
	}

	@Override
	public <X, Y> int decompose(
			@Nullable Object domainValue,
			int offset,
			@Nullable X x,
			@Nullable Y y,
			@Nonnull JdbcValueBiConsumer<X, Y> valueConsumer,
			@Nullable SharedSessionContractImplementor session) {
		return associationMapping.decompose( offset, x, y, domainValue, valueConsumer, session );
	}

	@Override
	public int forEachJdbcType(int offset, @Nonnull IndexedConsumer<JdbcMapping> action) {
		int span = getDiscriminatorMapping().forEachJdbcType( offset, action );
		return span + getKeyPart().forEachJdbcType( offset + span, action );
	}

	@Override
	public TableGroupJoin createTableGroupJoin(
			NavigablePath navigablePath,
			TableGroup lhs,
			@Nullable String explicitSourceAlias,
			@Nullable SqlAliasBase explicitSqlAliasBase,
			@Nullable SqlAstJoinType requestedJoinType,
			boolean fetched,
			boolean addsPredicate,
			SqlAstCreationState creationState) {
		final var joinType = requireNonNullElse( requestedJoinType, SqlAstJoinType.INNER );
		final var tableGroup = createRootTableGroupJoin(
				navigablePath,
				lhs,
				explicitSourceAlias,
				explicitSqlAliasBase,
				requestedJoinType,
				fetched,
				null,
				creationState
		);

		return new TableGroupJoin( navigablePath, joinType, tableGroup );
	}

	@Override
	public TableGroup createRootTableGroupJoin(
			NavigablePath navigablePath,
			TableGroup lhs,
			@Nullable String explicitSourceAlias,
			@Nullable SqlAliasBase explicitSqlAliasBase,
			@Nullable SqlAstJoinType sqlAstJoinType,
			boolean fetched,
			@Nullable Consumer<Predicate> predicateConsumer,
			SqlAstCreationState creationState) {
		return associationMapping.createRootTableGroupJoin(
				navigablePath,
				lhs,
				fetched,
				sqlAstJoinType,
				predicateConsumer,
				creationState
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

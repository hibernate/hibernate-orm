/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping.internal;

import java.io.Serializable;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.hibernate.SharedSessionContract;
import org.hibernate.cache.MutableCacheKeyBuilder;
import org.hibernate.engine.FetchStyle;
import org.hibernate.engine.FetchTiming;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.IndexedConsumer;
import org.hibernate.mapping.Any;
import org.hibernate.mapping.Property;
import org.hibernate.metamodel.mapping.AttributeMetadata;
import org.hibernate.metamodel.mapping.BasicValuedModelPart;
import org.hibernate.metamodel.mapping.DiscriminatedAssociationModelPart;
import org.hibernate.metamodel.mapping.DiscriminatorMapping;
import org.hibernate.metamodel.mapping.EntityIdentifierMapping;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.ManagedMappingType;
import org.hibernate.metamodel.mapping.MappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.SelectableConsumer;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.model.domain.NavigableRole;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.SqlAstJoinType;
import org.hibernate.sql.ast.spi.SqlAliasBase;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.spi.SqlSelection;
import org.hibernate.sql.ast.tree.from.StandardVirtualTableGroup;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoin;
import org.hibernate.sql.ast.tree.predicate.Predicate;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetch;
import org.hibernate.sql.results.graph.FetchParent;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.type.AnyType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.java.MutabilityPlan;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Singular, any-valued attribute
 *
 * @see org.hibernate.annotations.Any
 *
 * @author Steve Ebersole
 */
public class DiscriminatedAssociationAttributeMapping
		extends AbstractSingularAttributeMapping
		implements DiscriminatedAssociationModelPart {
	private final NavigableRole navigableRole;
	private final DiscriminatedAssociationMapping discriminatorMapping;
	private final SessionFactoryImplementor sessionFactory;

	public DiscriminatedAssociationAttributeMapping(
			NavigableRole attributeRole,
			JavaType<?> baseAssociationJtd,
			ManagedMappingType declaringType,
			int stateArrayPosition,
			int fetchableIndex,
			AttributeMetadata attributeMetadata,
			FetchTiming fetchTiming,
			PropertyAccess propertyAccess,
			Property bootProperty,
			AnyType anyType,
			Any bootValueMapping,
			MappingModelCreationProcess creationProcess) {
		super(
				bootProperty.getName(),
				stateArrayPosition,
				fetchableIndex,
				attributeMetadata,
				fetchTiming,
				FetchStyle.SELECT,
				declaringType,
				propertyAccess
		);
		this.navigableRole = attributeRole;

		this.discriminatorMapping = DiscriminatedAssociationMapping.from(
				attributeRole,
				baseAssociationJtd,
				this,
				anyType,
				bootValueMapping,
				creationProcess
		);
		this.sessionFactory = creationProcess.getCreationContext().getSessionFactory();
	}

	@Override
	public DiscriminatorMapping getDiscriminatorMapping() {
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
	public String toString() {
		return "DiscriminatedAssociationAttributeMapping(" + navigableRole + ")@" + System.identityHashCode( this );
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
	public void applySqlSelections(NavigablePath navigablePath, TableGroup tableGroup, DomainResultCreationState creationState) {
		discriminatorMapping.getDiscriminatorPart().applySqlSelections( navigablePath, tableGroup, creationState );
		discriminatorMapping.getKeyPart().applySqlSelections( navigablePath, tableGroup, creationState );
	}

	@Override
	public void applySqlSelections(NavigablePath navigablePath, TableGroup tableGroup, DomainResultCreationState creationState, BiConsumer<SqlSelection, JdbcMapping> selectionConsumer) {
		discriminatorMapping.getDiscriminatorPart().applySqlSelections( navigablePath, tableGroup, creationState, selectionConsumer );
		discriminatorMapping.getKeyPart().applySqlSelections( navigablePath, tableGroup, creationState, selectionConsumer );
	}

	@Override
	public NavigableRole getNavigableRole() {
		return navigableRole;
	}

	@Override
	public MappingType getMappedType() {
		return discriminatorMapping;
	}

	@Override
	public int getNumberOfFetchables() {
		return 2;
	}

	@Override
	public Fetchable getFetchable(int position) {
		switch ( position ) {
			case 0:
				assert getDiscriminatorPart().getFetchableKey() == 0;
				return getDiscriminatorPart();
			case 1:
				assert getKeyPart().getFetchableKey() == 1;
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
	public JdbcMapping getJdbcMapping(final int index) {
		switch (index) {
			case 0 : return discriminatorMapping.getDiscriminatorPart().getJdbcMapping();
			case 1 : return discriminatorMapping.getKeyPart().getJdbcMapping();
			default:
				throw new IndexOutOfBoundsException( index );
		}
	}

	@Override
	public SelectableMapping getSelectable(int columnIndex) {
		if ( columnIndex == 0 ) {
			return getDiscriminatorPart();
		}
		return getKeyPart();
	}

	@Override
	public Object disassemble(Object value, SharedSessionContractImplementor session) {
		if ( value == null ) {
			return null;
		}

		final EntityMappingType concreteMappingType = determineConcreteType( value, session );
		final EntityIdentifierMapping identifierMapping = concreteMappingType.getIdentifierMapping();

		final Object discriminator = discriminatorMapping
				.getModelPart()
				.resolveDiscriminatorForEntityType( concreteMappingType );
		final Object identifier = identifierMapping.getIdentifier( value );

		return new Object[] {
				discriminatorMapping.getDiscriminatorPart().disassemble( discriminator, session ),
				identifierMapping.disassemble( identifier, session )
		};
	}

	@Override
	public void addToCacheKey(MutableCacheKeyBuilder cacheKey, Object value, SharedSessionContractImplementor session) {
		if ( value == null ) {
			cacheKey.addValue( null );
			cacheKey.addHashCode( 0 );
		}
		else {
			final EntityMappingType concreteMappingType = determineConcreteType( value, session );

			final Object discriminator = discriminatorMapping
					.getModelPart()
					.resolveDiscriminatorForEntityType( concreteMappingType );
			discriminatorMapping.getDiscriminatorPart().addToCacheKey( cacheKey, discriminator, session );

			final EntityIdentifierMapping identifierMapping = concreteMappingType.getIdentifierMapping();
			identifierMapping.addToCacheKey( cacheKey, identifierMapping.getIdentifier( value ), session );
		}
	}

	private EntityMappingType determineConcreteType(Object entity, SharedSessionContractImplementor session) {
		final String entityName = session == null
				? sessionFactory.bestGuessEntityName( entity )
				: session.bestGuessEntityName( entity );
		return sessionFactory.getMappingMetamodel()
				.getEntityDescriptor( entityName );
	}

	@Override
	public int forEachSelectable(int offset, SelectableConsumer consumer) {
		discriminatorMapping.getDiscriminatorPart().forEachSelectable( offset, consumer );
		discriminatorMapping.getKeyPart().forEachSelectable( offset + 1, consumer );

		return 2;
	}

	@Override
	public int forEachJdbcType(IndexedConsumer<JdbcMapping> action) {
		action.accept( 0, discriminatorMapping.getDiscriminatorPart().getJdbcMapping() );
		action.accept( 1, discriminatorMapping.getKeyPart().getJdbcMapping() );
		return 2;
	}

	@Override
	public <X, Y> int forEachDisassembledJdbcValue(
			Object value,
			int offset,
			X x,
			Y y,
			JdbcValuesBiConsumer<X, Y> valuesConsumer,
			SharedSessionContractImplementor session) {
		if ( value == null ) {
			valuesConsumer.consume(
					offset,
					x,
					y,
					null,
					discriminatorMapping.getDiscriminatorPart().getJdbcMapping()
			);
			valuesConsumer.consume(
					offset + 1,
					x,
					y,
					null,
					discriminatorMapping.getKeyPart().getJdbcMapping()
			);
		}
		else {
			if ( value.getClass().isArray() ) {
				final Object[] values = (Object[]) value;
				valuesConsumer.consume(
						offset,
						x,
						y,
						values[0],
						discriminatorMapping.getDiscriminatorPart().getJdbcMapping()
				);
				valuesConsumer.consume(
						offset + 1,
						x,
						y,
						values[1],
						discriminatorMapping.getKeyPart().getJdbcMapping()
				);
			}
			else {
				final EntityMappingType concreteMappingType = determineConcreteType( value, session );

				final Object discriminator = discriminatorMapping
						.getModelPart()
						.resolveDiscriminatorForEntityType( concreteMappingType );
				final Object disassembledDiscriminator = discriminatorMapping.getDiscriminatorPart().disassemble( discriminator, session );
				valuesConsumer.consume(
						offset,
						x,
						y,
						disassembledDiscriminator,
						discriminatorMapping.getDiscriminatorPart().getJdbcMapping()
				);

				final EntityIdentifierMapping identifierMapping = concreteMappingType.getIdentifierMapping();
				final Object identifier = identifierMapping.getIdentifier( value );
				final Object disassembledKey = discriminatorMapping.getKeyPart().disassemble( identifier, session );
				valuesConsumer.consume(
						offset + 1,
						x,
						y,
						disassembledKey,
						discriminatorMapping.getKeyPart().getJdbcMapping()
				);
			}
		}

		return 2;
	}

	@Override
	public <X, Y> int breakDownJdbcValues(
			Object domainValue,
			int offset,
			X x,
			Y y,
			JdbcValueBiConsumer<X, Y> valueConsumer,
			SharedSessionContractImplementor session) {
		return discriminatorMapping.breakDownJdbcValues( offset, x, y, domainValue, valueConsumer, session );
	}

	@Override
	public <X, Y> int decompose(
			Object domainValue,
			int offset,
			X x,
			Y y,
			JdbcValueBiConsumer<X, Y> valueConsumer,
			SharedSessionContractImplementor session) {
		return discriminatorMapping.decompose( offset, x, y, domainValue, valueConsumer, session );
	}

	@Override
	public int forEachJdbcType(int offset, IndexedConsumer<JdbcMapping> action) {
		int span = getDiscriminatorPart().forEachJdbcType( offset, action );
		return span + getKeyPart().forEachJdbcType( offset + span, action );
	}

	@Override
	public void visitFetchables(Consumer<? super Fetchable> fetchableConsumer, EntityMappingType treatTargetType) {
		fetchableConsumer.accept( getDiscriminatorPart() );
		fetchableConsumer.accept( getKeyPart() );
	}

	@Override
	public void visitFetchables(IndexedConsumer<? super Fetchable> fetchableConsumer, EntityMappingType treatTargetType) {
		//noinspection unchecked,rawtypes
		forEachSubPart( (IndexedConsumer) fetchableConsumer, treatTargetType );
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
	public boolean hasPartitionedSelectionMapping() {
		return discriminatorMapping.getDiscriminatorPart().isPartitioned()
				|| discriminatorMapping.getKeyPart().isPartitioned();
	}

	public static class MutabilityPlanImpl implements MutabilityPlan<Object> {
		// for now use the AnyType for consistency with write-operations
		private final AnyType anyType;

		public MutabilityPlanImpl(AnyType anyType) {
			this.anyType = anyType;
		}

		@Override
		public boolean isMutable() {
			return anyType.isMutable();
		}

		@Override
		public Object deepCopy(Object value) {
			return value;
		}

		@Override
		public Serializable disassemble(Object value, SharedSessionContract session) {
//			if ( value == null ) {
//				return null;
//			}
//			else {
//				return new AnyType.ObjectTypeCacheEntry(
//						persistenceContext.bestGuessEntityName( value ),
//						ForeignKeys.getEntityIdentifierIfNotUnsaved(
//								persistenceContext.bestGuessEntityName( value ),
//								value,
//								persistenceContext
//						)
//				);
//			}

			// this ^^ is what we want eventually, but for the time-being to ensure compatibility with
			// writing just reuse the AnyType

			final SharedSessionContractImplementor persistenceContext = (SharedSessionContractImplementor) session;
			return anyType.disassemble( value, persistenceContext, null );
		}

		@Override
		public Object assemble(Serializable cached, SharedSessionContract session) {
//			final AnyType.ObjectTypeCacheEntry e = (AnyType.ObjectTypeCacheEntry) cached;
//			return e == null ? null : session.internalLoad( e.entityName, e.id, eager, false );

			// again, what we want eventually ^^ versus what we should do now vv

			final SharedSessionContractImplementor persistenceContext = (SharedSessionContractImplementor) session;
			return anyType.assemble( cached, persistenceContext, null );
		}
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
		final SqlAstJoinType joinType = Objects.requireNonNullElse( requestedJoinType, SqlAstJoinType.INNER );
		final TableGroup tableGroup = createRootTableGroupJoin(
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
		return new StandardVirtualTableGroup( navigablePath, this, lhs, fetched );
	}

	@Override
	public SqlAstJoinType getDefaultSqlAstJoinType(TableGroup parentTableGroup) {
		return SqlAstJoinType.LEFT;
	}

	@Override
	public String getSqlAliasStem() {
		return getAttributeName();
	}

	@Override
	public void applyDiscriminator(Consumer<Predicate> predicateConsumer, String alias, TableGroup tableGroup, SqlAstCreationState creationState) {
		throw new UnsupportedOperationException();
	}
}

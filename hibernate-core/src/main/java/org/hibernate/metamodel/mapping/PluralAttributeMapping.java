/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.metamodel.model.domain.NavigableRole;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.hibernate.Filter;
import org.hibernate.Incubating;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.spi.IndexedConsumer;
import org.hibernate.loader.ast.spi.Loadable;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.metamodel.mapping.ordering.spi.OrderByFragment;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.creation.SqlAstCreationState;
import org.hibernate.sql.ast.spi.creation.SqlAliasBaseGenerator;
import org.hibernate.sql.ast.spi.query.from.TableGroup;
import org.hibernate.sql.ast.spi.query.from.TableGroupJoinProducer;
import org.hibernate.sql.ast.spi.query.predicate.Predicate;
import org.hibernate.sql.results.graph.DomainResult;
import org.hibernate.sql.results.graph.DomainResultCreationState;
import org.hibernate.sql.results.graph.Fetchable;
import org.hibernate.sql.results.graph.FetchableContainer;
import org.hibernate.sql.results.graph.basic.BasicResult;

/**
 * Mapping of a plural (collection-valued) attribute
 *
 * @author Steve Ebersole
 */
public interface PluralAttributeMapping
		extends AttributeMapping, TableGroupJoinProducer, FetchableContainer, Loadable, Restrictable, SoftDeletableModelPart {
	@Nonnull
	@Override
	NavigableRole getNavigableRole();


	@Nonnull
	CollectionPersister getCollectionDescriptor();

	@Nonnull
	ForeignKeyDescriptor getKeyDescriptor();

	@Nullable
	CollectionPart getIndexDescriptor();

	@Nonnull
	@Override
	CollectionMappingType<?> getMappedType();

	@org.hibernate.SPI({ org.hibernate.SPI.Role.USE, org.hibernate.SPI.Role.IMPLEMENT })
	@FunctionalInterface
	interface PredicateConsumer {
		void applyPredicate(@Nonnull Predicate predicate);
	}

	/**
	 * Apply auxiliary restrictions (soft delete, temporal, audit) in a single pass.
	 */
	void applyAuxiliaryRestrictions(
			@Nonnull TableGroup tableGroup,
			@Nonnull PredicateConsumer predicateConsumer,
			@Nonnull LoadQueryInfluencers influencers,
			@Nonnull SqlAliasBaseGenerator sqlAliasBaseGenerator);

	interface IndexMetadata {
		@Nullable
		CollectionPart getIndexDescriptor();
		int getListIndexBase();
		@Nullable
		String getIndexPropertyName();
	}

	@Nullable
	IndexMetadata getIndexMetadata();

	@Nonnull
	CollectionPart getElementDescriptor();

	@Nullable
	CollectionIdentifierDescriptor getIdentifierDescriptor();

	/**
	 * Mapping for soft-delete support, or {@code null} if soft-delete not defined
	 */
	@Nullable
	@Incubating(since = "5.4")
	default SoftDeleteMapping getSoftDeleteMapping() {
		return null;
	}

	/**
	 * Mapping for temporal support, or {@code null} if temporal not defined
	 */
	@Nullable
	@Incubating(since = "5.4")
	default TemporalMapping getTemporalMapping() {
		return null;
	}

	/**
	 * Mapping for audit support, or {@code null} if audit not defined
	 */
	@Nullable
	@Incubating(since = "5.4")
	default AuditMapping getAuditMapping() {
		return null;
	}

	@Nullable
	OrderByFragment getOrderByFragment();
	@Nullable
	OrderByFragment getManyToManyOrderByFragment();

	@Override
	default void visitKeyFetchables(@Nonnull Consumer<? super Fetchable> fetchableConsumer, @Nullable EntityMappingType treatTargetType) {
		final CollectionPart indexDescriptor = getIndexDescriptor();
		if ( indexDescriptor != null ) {
			fetchableConsumer.accept( indexDescriptor );
		}
	}

	@Override
	default int getNumberOfKeyFetchables() {
		return getIndexDescriptor() == null ? 0 : 1;
	}

	@Nonnull
	@Override
	default Fetchable getKeyFetchable(int position) {
		final CollectionPart indexDescriptor = getIndexDescriptor();
		if ( indexDescriptor != null && position == 0 ) {
			return indexDescriptor;
		}
		throw new IndexOutOfBoundsException( position );
	}

	@Override
	default void visitKeyFetchables(@Nonnull IndexedConsumer<? super Fetchable> fetchableConsumer, @Nullable EntityMappingType treatTargetType) {
		final CollectionPart indexDescriptor = getIndexDescriptor();
		if ( indexDescriptor != null ) {
			fetchableConsumer.accept( 0, indexDescriptor );
		}
	}

	@Override
	default void visitFetchables(@Nonnull Consumer<? super Fetchable> fetchableConsumer, @Nullable EntityMappingType treatTargetType) {
		fetchableConsumer.accept( getElementDescriptor() );
	}

	@Override
	default int getNumberOfFetchables() {
		return 1;
	}

	@Override
	default int getNumberOfFetchableKeys() {
		return getNumberOfKeyFetchables() + getNumberOfFetchables();
	}

	@Override
	default void visitFetchables(@Nonnull IndexedConsumer<? super Fetchable> fetchableConsumer, @Nullable EntityMappingType treatTargetType) {
		fetchableConsumer.accept( 0, getElementDescriptor() );
	}

	@Nonnull
	@Override
	default Fetchable getFetchable(int position) {
		if ( position == 0 ) {
			return getElementDescriptor();
		}
		throw new IndexOutOfBoundsException( position );
	}

	@Nonnull
	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	@org.hibernate.SPI(org.hibernate.SPI.Role.SUPPLY)
	default <T> DomainResult<T> createSnapshotDomainResult(
			@Nonnull NavigablePath navigablePath,
			@Nonnull TableGroup parentTableGroup,
			@Nullable String resultVariable,
			@Nonnull DomainResultCreationState creationState) {
		return new BasicResult( 0, null, getJavaType(), null, null, false, false );
	}

	@Nullable
	String getSeparateCollectionTable();

	@org.hibernate.Internal
	boolean isBidirectionalAttributeName(@Nonnull NavigablePath fetchablePath, @Nonnull ToOneAttributeMapping modelPart);

	@Override
	default boolean incrementFetchDepth(){
		return true;
	}

	@Override
	default void applyFilterRestrictions(
			@Nonnull Consumer<Predicate> predicateConsumer,
			@Nonnull TableGroup tableGroup,
			boolean useQualifier,
			@Nonnull Map<String, Filter> enabledFilters,
			boolean onlyApplyLoadByKeyFilters,
			@Nullable SqlAstCreationState creationState) {
		getCollectionDescriptor().applyFilterRestrictions(
				predicateConsumer,
				tableGroup,
				useQualifier,
				enabledFilters,
				onlyApplyLoadByKeyFilters,
				creationState
		);
	}

	@Override
	default void applyBaseRestrictions(
			@Nonnull Consumer<Predicate> predicateConsumer,
			@Nonnull TableGroup tableGroup,
			boolean useQualifier,
			@Nonnull Map<String, Filter> enabledFilters,
			boolean onlyApplyLoadByKeyFilters,
			@Nullable Set<String> treatAsDeclarations,
			@Nullable SqlAstCreationState creationState) {
		getCollectionDescriptor().applyBaseRestrictions(
				predicateConsumer,
				tableGroup,
				useQualifier,
				enabledFilters,
				onlyApplyLoadByKeyFilters,
				treatAsDeclarations,
				creationState
		);
	}

	default void applyBaseManyToManyRestrictions(
			@Nonnull Consumer<Predicate> predicateConsumer,
			@Nonnull TableGroup tableGroup,
			boolean useQualifier,
			@Nonnull Map<String, Filter> enabledFilters,
			@Nullable Set<String> treatAsDeclarations,
			@Nullable SqlAstCreationState creationState) {
		getCollectionDescriptor().applyBaseManyToManyRestrictions( predicateConsumer, tableGroup, useQualifier, enabledFilters, treatAsDeclarations, creationState );
	}

	@Override
	default boolean hasWhereRestrictions() {
		return getCollectionDescriptor().hasWhereRestrictions();
	}

	@Override
	default void applyWhereRestrictions(
			@Nonnull Consumer<Predicate> predicateConsumer,
			@Nonnull TableGroup tableGroup,
			boolean useQualifier,
			@Nullable SqlAstCreationState creationState) {
		getCollectionDescriptor().applyWhereRestrictions( predicateConsumer, tableGroup, useQualifier, creationState );
	}

	@Nonnull
	@Override
	default PluralAttributeMapping asPluralAttributeMapping() {
		return this;
	}

	@Override
	default boolean isPluralAttributeMapping() {
		return true;
	}

}

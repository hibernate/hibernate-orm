/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.hibernate.Filter;
import org.hibernate.internal.util.IndexedConsumer;
import org.hibernate.loader.ast.spi.Loadable;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.metamodel.mapping.ordering.OrderByFragment;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.ast.spi.SqlAstCreationState;
import org.hibernate.sql.ast.tree.from.TableGroup;
import org.hibernate.sql.ast.tree.from.TableGroupJoinProducer;
import org.hibernate.sql.ast.tree.predicate.Predicate;
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

	CollectionPersister getCollectionDescriptor();

	ForeignKeyDescriptor getKeyDescriptor();

	CollectionPart getIndexDescriptor();

	@Override
	CollectionMappingType<?> getMappedType();

	@FunctionalInterface
	interface PredicateConsumer {
		void applyPredicate(Predicate predicate);
	}

	void applySoftDeleteRestrictions(TableGroup tableGroup, PredicateConsumer predicateConsumer);

	interface IndexMetadata {
		CollectionPart getIndexDescriptor();
		int getListIndexBase();
		String getIndexPropertyName();
	}

	IndexMetadata getIndexMetadata();

	CollectionPart getElementDescriptor();

	CollectionIdentifierDescriptor getIdentifierDescriptor();

	/**
	 * Mapping for soft-delete support, or {@code null} if soft-delete not defined
	 */
	default SoftDeleteMapping getSoftDeleteMapping() {
		return null;
	}

	OrderByFragment getOrderByFragment();
	OrderByFragment getManyToManyOrderByFragment();

	@Override
	default void visitKeyFetchables(Consumer<? super Fetchable> fetchableConsumer, EntityMappingType treatTargetType) {
		final CollectionPart indexDescriptor = getIndexDescriptor();
		if ( indexDescriptor != null ) {
			fetchableConsumer.accept( indexDescriptor );
		}
	}

	@Override
	default int getNumberOfKeyFetchables() {
		return getIndexDescriptor() == null ? 0 : 1;
	}

	@Override
	default Fetchable getKeyFetchable(int position) {
		final CollectionPart indexDescriptor = getIndexDescriptor();
		if ( indexDescriptor != null && position == 0 ) {
			return indexDescriptor;
		}
		throw new IndexOutOfBoundsException( position );
	}

	@Override
	default void visitKeyFetchables(IndexedConsumer<? super Fetchable> fetchableConsumer, EntityMappingType treatTargetType) {
		final CollectionPart indexDescriptor = getIndexDescriptor();
		if ( indexDescriptor != null ) {
			fetchableConsumer.accept( 0, indexDescriptor );
		}
	}

	@Override
	default void visitFetchables(Consumer<? super Fetchable> fetchableConsumer, EntityMappingType treatTargetType) {
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
	default void visitFetchables(IndexedConsumer<? super Fetchable> fetchableConsumer, EntityMappingType treatTargetType) {
		fetchableConsumer.accept( 0, getElementDescriptor() );
	}

	@Override
	default Fetchable getFetchable(int position) {
		if ( position == 0 ) {
			return getElementDescriptor();
		}
		throw new IndexOutOfBoundsException( position );
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	default <T> DomainResult<T> createSnapshotDomainResult(
			NavigablePath navigablePath,
			TableGroup parentTableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
		return new BasicResult( 0, null, getJavaType(), null, null, false, false );
	}

	String getSeparateCollectionTable();

	boolean isBidirectionalAttributeName(NavigablePath fetchablePath, ToOneAttributeMapping modelPart);

	@Override
	default boolean incrementFetchDepth(){
		return true;
	}

	@Override
	default void applyFilterRestrictions(
			Consumer<Predicate> predicateConsumer,
			TableGroup tableGroup,
			boolean useQualifier,
			Map<String, Filter> enabledFilters,
			boolean onlyApplyLoadByKeyFilters,
			SqlAstCreationState creationState) {
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
			Consumer<Predicate> predicateConsumer,
			TableGroup tableGroup,
			boolean useQualifier,
			Map<String, Filter> enabledFilters,
			boolean onlyApplyLoadByKeyFilters,
			Set<String> treatAsDeclarations,
			SqlAstCreationState creationState) {
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
			Consumer<Predicate> predicateConsumer,
			TableGroup tableGroup,
			boolean useQualifier,
			Map<String, Filter> enabledFilters,
			Set<String> treatAsDeclarations,
			SqlAstCreationState creationState) {
		getCollectionDescriptor().applyBaseManyToManyRestrictions( predicateConsumer, tableGroup, useQualifier, enabledFilters, treatAsDeclarations, creationState );
	}

	@Override
	default boolean hasWhereRestrictions() {
		return getCollectionDescriptor().hasWhereRestrictions();
	}

	@Override
	default void applyWhereRestrictions(
			Consumer<Predicate> predicateConsumer,
			TableGroup tableGroup,
			boolean useQualifier,
			SqlAstCreationState creationState) {
		getCollectionDescriptor().applyWhereRestrictions( predicateConsumer, tableGroup, useQualifier, creationState );
	}

	@Override
	default PluralAttributeMapping asPluralAttributeMapping() {
		return this;
	}

	@Override
	default boolean isPluralAttributeMapping() {
		return true;
	}

	@Override
	default boolean isReadOnly() {
		return getCollectionDescriptor().getMappedByProperty() != null
				|| getKeyDescriptor().getKeyPart().isReadOnly();
	}

}

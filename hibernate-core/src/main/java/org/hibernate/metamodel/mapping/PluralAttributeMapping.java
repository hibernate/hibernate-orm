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
import org.hibernate.loader.ast.spi.Loadable;
import org.hibernate.metamodel.mapping.internal.ToOneAttributeMapping;
import org.hibernate.metamodel.mapping.ordering.OrderByFragment;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.query.spi.NavigablePath;
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
		extends AttributeMapping, TableGroupJoinProducer, FetchableContainer, Loadable, Restrictable {

	CollectionPersister getCollectionDescriptor();

	ForeignKeyDescriptor getKeyDescriptor();

	CollectionPart getIndexDescriptor();

	@Override
	CollectionMappingType<?> getMappedType();

	interface IndexMetadata {
		CollectionPart getIndexDescriptor();
		int getListIndexBase();
		String getIndexPropertyName();
	}

	IndexMetadata getIndexMetadata();

	CollectionPart getElementDescriptor();

	CollectionIdentifierDescriptor getIdentifierDescriptor();

	OrderByFragment getOrderByFragment();
	OrderByFragment getManyToManyOrderByFragment();

	@Override
	default void visitKeyFetchables(Consumer<Fetchable> fetchableConsumer, EntityMappingType treatTargetType) {
		final CollectionPart indexDescriptor = getIndexDescriptor();
		if ( indexDescriptor != null ) {
			fetchableConsumer.accept( indexDescriptor );
		}
	}

	@Override
	default void visitFetchables(Consumer<Fetchable> fetchableConsumer, EntityMappingType treatTargetType) {
		fetchableConsumer.accept( getElementDescriptor() );
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	default <T> DomainResult<T> createSnapshotDomainResult(
			NavigablePath navigablePath,
			TableGroup tableGroup,
			String resultVariable,
			DomainResultCreationState creationState) {
		return new BasicResult( 0, null, getJavaType() );
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
			SqlAstCreationState creationState) {
		getCollectionDescriptor().applyFilterRestrictions( predicateConsumer, tableGroup, useQualifier, enabledFilters, creationState );
	}

	@Override
	default void applyBaseRestrictions(
			Consumer<Predicate> predicateConsumer,
			TableGroup tableGroup,
			boolean useQualifier,
			Map<String, Filter> enabledFilters,
			Set<String> treatAsDeclarations,
			SqlAstCreationState creationState) {
		getCollectionDescriptor().applyBaseRestrictions( predicateConsumer, tableGroup, useQualifier, enabledFilters, treatAsDeclarations, creationState );
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
	default void applyWhereRestrictions(
			Consumer<Predicate> predicateConsumer,
			TableGroup tableGroup,
			boolean useQualifier,
			SqlAstCreationState creationState) {
		getCollectionDescriptor().applyWhereRestrictions( predicateConsumer, tableGroup, useQualifier, creationState );
	}
}

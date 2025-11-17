/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.internal.util.IndexedConsumer;
import org.hibernate.internal.util.MutableInteger;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ModelPartContainer;

/**
 * Container of {@link Fetchable} references
 *
 * @author Steve Ebersole
 */
public interface FetchableContainer extends ModelPartContainer {

	/**
	 * The number of key fetchables in the container
	 */
	default int getNumberOfKeyFetchables() {
		return 0;
	}

	/**
	 * The number of fetchables in the container
	 */
	int getNumberOfFetchables();

	/**
	 * The number of fetchables in the container
	 */
	default int getNumberOfFetchableKeys() {
		return getNumberOfFetchables();
	}

	default Fetchable getKeyFetchable(int position) {
		List<Fetchable> fetchables = new ArrayList<>( getNumberOfKeyFetchables() );
		visitKeyFetchables( fetchable -> fetchables.add( fetchable ), null );
		return fetchables.get( position );
	}

	default Fetchable getFetchable(int position) {
		List<Fetchable> fetchables = new ArrayList<>( getNumberOfFetchables() );
		visitFetchables( fetchable -> fetchables.add( fetchable ), null );
		return fetchables.get( position );
	}

	default void visitKeyFetchables(
			Consumer<? super Fetchable> fetchableConsumer,
			EntityMappingType treatTargetType) {
		// by default, nothing to do
	}

	default void visitKeyFetchables(
			IndexedConsumer<? super Fetchable> fetchableConsumer,
			EntityMappingType treatTargetType) {
		visitKeyFetchables( 0, fetchableConsumer, treatTargetType );
	}

	default void visitKeyFetchables(
			int offset,
			IndexedConsumer<? super Fetchable> fetchableConsumer,
			EntityMappingType treatTargetType) {
		// by default, nothing to do
	}

	default void visitFetchables(
			Consumer<? super Fetchable> fetchableConsumer,
			EntityMappingType treatTargetType) {
		//noinspection unchecked
		visitSubParts( (Consumer) fetchableConsumer, treatTargetType );
	}

	default void visitFetchables(
			IndexedConsumer<? super Fetchable> fetchableConsumer,
			EntityMappingType treatTargetType) {
		visitFetchables( 0, fetchableConsumer, treatTargetType );
	}

	default void visitFetchables(
			int offset,
			IndexedConsumer<? super Fetchable> fetchableConsumer,
			EntityMappingType treatTargetType) {
		final MutableInteger index = new MutableInteger( offset );
		visitSubParts(
				modelPart -> fetchableConsumer.accept( index.getAndIncrement(), (Fetchable) modelPart ),
				treatTargetType
		);
	}

	default int getSelectableIndex(String selectableName) {
		final MutableInteger position = new MutableInteger( -1 );
		forEachSelectable(
				(selectionIndex, selectableMapping) -> {
					if ( selectableMapping.getSelectableName().equals( selectableName ) ) {
						position.set( selectionIndex );
					}
				}
		);
		return position.get();
	}
}

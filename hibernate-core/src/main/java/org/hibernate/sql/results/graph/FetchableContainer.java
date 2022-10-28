/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.internal.util.MutableInteger;
import org.hibernate.mapping.IndexedConsumer;
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
			Consumer<Fetchable> fetchableConsumer,
			EntityMappingType treatTargetType) {
		// by default, nothing to do
	}

	default void visitKeyFetchables(
			IndexedConsumer<Fetchable> fetchableConsumer,
			EntityMappingType treatTargetType) {
		visitKeyFetchables( 0, fetchableConsumer, treatTargetType );
	}

	default void visitKeyFetchables(
			int offset,
			IndexedConsumer<Fetchable> fetchableConsumer,
			EntityMappingType treatTargetType) {
		// by default, nothing to do
	}

	default void visitFetchables(
			Consumer<Fetchable> fetchableConsumer,
			EntityMappingType treatTargetType) {
		//noinspection unchecked
		visitSubParts( (Consumer) fetchableConsumer, treatTargetType );
	}

	default void visitFetchables(
			IndexedConsumer<Fetchable> fetchableConsumer,
			EntityMappingType treatTargetType) {
		visitFetchables( 0, fetchableConsumer, treatTargetType );
	}

	default void visitFetchables(
			int offset,
			IndexedConsumer<Fetchable> fetchableConsumer,
			EntityMappingType treatTargetType) {
		final MutableInteger index = new MutableInteger( offset );
		visitSubParts(
				modelPart -> fetchableConsumer.accept( index.getAndIncrement(), (Fetchable) modelPart ),
				treatTargetType
		);
	}
}

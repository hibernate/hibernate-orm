/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph;

import java.util.function.Consumer;

import org.hibernate.metamodel.mapping.AttributeMapping;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.collection.internal.AbstractImmediateCollectionInitializer;
import org.hibernate.sql.results.graph.embeddable.EmbeddableInitializer;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Provides access to information about the owner/parent of a fetch
 * in relation to the current "row" being processed.
 *
 * @author Steve Ebersole
 */
public interface FetchParentAccess extends InitializerParent {
	/**
	 * Find the first entity access up the fetch parent graph
	 * @deprecated use {@link #findOwningEntityInitializer()} instead
	 */
	@Deprecated(forRemoval = true)
	default @Nullable EntityInitializer findFirstEntityDescriptorAccess() {
		return findOwningEntityInitializer();
	}

	default @Nullable EntityInitializer findFirstEntityInitializer() {
		// Keep this method around for binary backwards compatibility
		return InitializerParent.super.findFirstEntityInitializer();
	}

	/**
	 * @deprecated Use {@link EntityInitializer#getEntityIdentifier()} on {@link #findFirstEntityInitializer()} instead.
	 */
	@Deprecated(forRemoval = true)
	default @Nullable Object getParentKey() {
		EntityInitializer entityInitializer = asEntityInitializer();
		return entityInitializer == null || ( entityInitializer = findOwningEntityInitializer() ) == null
				? null
				: entityInitializer.getEntityIdentifier();
	}

	NavigablePath getNavigablePath();

	/**
	 * Register a listener to be notified when the parent is "resolved"
	 *
	 * @apiNote If already resolved, the callback is triggered immediately
	 * @deprecated Not used anymore
	 */
	@Deprecated(forRemoval = true)
	default void registerResolutionListener(Consumer<Object> resolvedParentConsumer) {
		throw new UnsupportedOperationException( "Don't use this method. It will be removed." );
	}

	/**
	 * @deprecated Use {@link #getParent()} instead
	 */
	@Deprecated(forRemoval = true)
	default @Nullable FetchParentAccess getFetchParentAccess() {
		return null;
	}

	@Override
	default @Nullable InitializerParent getParent() {
		return getFetchParentAccess();
	}

	/**
	 * @deprecated Not needed anymore.
	 */
	@Deprecated(forRemoval = true)
	default boolean shouldSkipInitializer(RowProcessingState rowProcessingState) {
		return false;
	}
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph;

import org.hibernate.Incubating;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.spi.EntityIdentifierNavigablePath;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.exec.spi.ExecutionContext;
import org.hibernate.sql.results.graph.collection.CollectionInitializer;
import org.hibernate.sql.results.graph.embeddable.EmbeddableInitializer;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Defines a multi-step process for initializing entity, collection and
 * composite state.  Each step is performed on each initializer
 * before starting the next step.
 *
 * @author Steve Ebersole
 */
@Incubating
public interface Initializer {

	Initializer[] EMPTY_ARRAY = new Initializer[0];

	/**
	 * Returns the parent {@link Initializer} or {@code null} if this is a result initializer.
	 */
	default @Nullable InitializerParent getParent() {
		return null;
	}

	/**
	 * Find the first entity access up the fetch parent graph
	 * @deprecated use {@link #findOwningEntityInitializer()} instead
	 */
	@Deprecated(forRemoval = true)
	default @Nullable EntityInitializer findFirstEntityDescriptorAccess() {
		return findOwningEntityInitializer();
	}

	/**
	 * Find the entity initializer that owns this initializer
	 * by traversing up {@link #getParent()}.
	 */
	default @Nullable EntityInitializer findOwningEntityInitializer() {
		return Initializer.findOwningEntityInitializer( getParent() );
	}
	/**
	 * Find the entity initializer that owns this initializer
	 * by traversing up {@link #getParent()}.
	 */
	static @Nullable EntityInitializer findOwningEntityInitializer(@Nullable Initializer parent) {
		if ( parent == null || parent.isCollectionInitializer() ) {
			return null;
		}
		final EntityInitializer entityInitializer = parent.asEntityInitializer();
		if ( entityInitializer != null ) {
			return entityInitializer;
		}
		return findOwningEntityInitializer( parent.getParent() );
	}

	/**
	 * Find the first {@link EntityInitializer},
	 * returning {@code this} if {@link #isEntityInitializer()} returns {@code true}.
	 * @deprecated Use {@link #findOwningEntityInitializer()} instead, optionally in combination with
	 * {@link #asEntityInitializer()} if the type of the {@code this} {@link Initializer} is unknown.
	 */
	@Deprecated(forRemoval = true)
	default @Nullable EntityInitializer findFirstEntityInitializer() {
		final EntityInitializer entityInitializer = this.asEntityInitializer();
		if ( entityInitializer != null ) {
			return entityInitializer;
		}
		return findOwningEntityInitializer();
	}

	NavigablePath getNavigablePath();

	ModelPart getInitializedPart();

	Object getInitializedInstance();

	/**
	 * The current state of this initializer.
	 */
	State getState();

	/**
	 * Step 0 - Callback for initializers before the first row is read.
	 * It is the responsibility of this initializer to recurse to the sub-initializers.
	 *
	 * This is useful for e.g. preparing initializers in case of a cache hit.
	 */
	void startLoading(RowProcessingState rowProcessingState);
		// by default - nothing to do

	/**
	 * Step 1 - Resolve the key value for this initializer for the current
	 * row and then recurse to the sub-initializers.
	 *
	 * After this point, the initializer knows whether further processing is necessary
	 * for the current row i.e. if the object is missing.
	 */
	void resolveKey();

	/**
	 * Step 2.1 - Using the key resolved in {@link #resolveKey}, resolve the
	 * instance (of the thing initialized) to use for the current row.
	 *
	 * After this point, the initializer knows the entity/collection/component
	 * instance for the current row based on the resolved key.
	 * If the resolving was successful, {@link #getInitializedInstance()} will return that instance.
	 */
	void resolveInstance();

	/**
	 * Step 2.2 - Use the given instance as resolved instance for this initializer.
	 * Initializers are supposed to recursively call this method for sub-initializers.
	 *
	 * This alternative initialization protocol is used when a parent instance was already part of the persistence context.
	 */
	default void resolveInstance(@Nullable Object instance) {
		resolveKey();
	}

	/**
	 * Step 3 - Initialize the state of the instance resolved in
	 * {@link #resolveInstance} from the current row values.
	 *
	 * All resolved state for the current row is injected into the resolved
	 * instance
	 */
	void initializeInstance();

	/**
	 * Step 3.1 - Initialize the state of the instance as extracted from the given parentInstance.
	 * Extraction can be done with the {@link #getInitializedPart()}.
	 * Initializers are supposed to recursively call this method for sub-initializers.
	 *
	 * This alternative initialization protocol is used for shallow query cache hits,
	 * in which case there is no data available in the {@link org.hibernate.sql.results.jdbc.internal.JdbcValuesCacheHit}
	 * to initialize potentially lazy associations.
	 */
	default void initializeInstanceFromParent(Object parentInstance) {
	}

	/**
	 * Lifecycle method called at the end of the current row processing.
	 * Provides ability to complete processing from the current row and
	 * prepare for the next row.
	 */
	void finishUpRow();

	/**
	 * Lifecycle method called at the very end of the result values processing
	 */
	default void endLoading(ExecutionContext executionContext) {
		// by default - nothing to do
	}

	/**
	 * Indicates whether this initializer is part of a key i.e. entity identifier, foreign key or collection key.
	 */
	boolean isPartOfKey();

	/**
	 * @deprecated Use {@link #isPartOfKey(NavigablePath, InitializerParent)} instead.
	 */
	@Deprecated(forRemoval = true)
	static boolean isPartOfKey(NavigablePath navigablePath, FetchParentAccess parentAccess) {
		return isPartOfKey( navigablePath, (InitializerParent) parentAccess );
	}

	static boolean isPartOfKey(NavigablePath navigablePath, InitializerParent parent) {
		return parent != null && parent.isEmbeddableInitializer() && parent.isPartOfKey()
				|| navigablePath instanceof EntityIdentifierNavigablePath
				|| ForeignKeyDescriptor.PART_NAME.equals( navigablePath.getLocalName() )
				|| ForeignKeyDescriptor.TARGET_PART_NAME.equals( navigablePath.getLocalName() );
	}

	/**
	 * Indicates if this is a result or fetch initializer.
	 */
	boolean isResultInitializer();

	default boolean isEmbeddableInitializer() {
		return false;
	}

	default boolean isEntityInitializer() {
		return false;
	}

	default boolean isCollectionInitializer() {
		return false;
	}

	/**
	 * A utility method to avoid casting explicitly to EntityInitializer
	 *
	 * @return EntityInitializer if this is an instance of EntityInitializer otherwise {@code null}
	 */
	default @Nullable EntityInitializer asEntityInitializer() {
		return null;
	}

	/**
	 * A utility method to avoid casting explicitly to EmbeddableInitializer
	 *
	 * @return EmbeddableInitializer if this is an instance of EmbeddableInitializer otherwise {@code null}
	 */
	default @Nullable EmbeddableInitializer asEmbeddableInitializer() {
		return null;
	}

	/**
	 * A utility method to avoid casting explicitly to CollectionInitializer
	 *
	 * @return CollectionInitializer if this is an instance of CollectionInitializer otherwise {@code null}
	 */
	default @Nullable CollectionInitializer asCollectionInitializer() {
		return null;
	}

	enum State {
		UNINITIALIZED,
		MISSING,
		KEY_RESOLVED,
		RESOLVED,
		INITIALIZED;
	}
}

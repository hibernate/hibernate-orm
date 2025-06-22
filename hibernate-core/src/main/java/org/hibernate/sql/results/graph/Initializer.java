/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.sql.results.graph;

import org.hibernate.Incubating;
import org.hibernate.metamodel.mapping.ForeignKeyDescriptor;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.spi.EntityIdentifierNavigablePath;
import org.hibernate.spi.NavigablePath;
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
public interface Initializer<Data extends InitializerData> {

	Initializer<?>[] EMPTY_ARRAY = new Initializer<?>[0];

	/**
	 * Returns the parent {@link Initializer} or {@code null} if this is a result initializer.
	 */
	default @Nullable InitializerParent<?> getParent() {
		return null;
	}

	/**
	 * Find the entity initializer that owns this initializer
	 * by traversing up {@link #getParent()}.
	 */
	default @Nullable EntityInitializer<?> findOwningEntityInitializer() {
		return findOwningEntityInitializer( getParent() );
	}
	/**
	 * Find the entity initializer that owns this initializer
	 * by traversing up {@link #getParent()}.
	 */
	static @Nullable EntityInitializer<?> findOwningEntityInitializer(@Nullable Initializer<?> parent) {
		if ( parent == null || parent.isCollectionInitializer() ) {
			return null;
		}
		else {
			final EntityInitializer<?> initializer = parent.asEntityInitializer();
			return initializer != null ? initializer : findOwningEntityInitializer( parent.getParent() );
		}
	}

	NavigablePath getNavigablePath();

	ModelPart getInitializedPart();

	default Object getResolvedInstance(Data data) {
		assert switch ( data.getState() ) {
			case UNINITIALIZED, KEY_RESOLVED -> false;
			case INITIALIZED, RESOLVED -> true;
			case MISSING -> data.getInstance() == null;
		};
		return data.getInstance();
	}
	default Object getResolvedInstance(RowProcessingState rowProcessingState) {
		return getResolvedInstance( getData( rowProcessingState ) );
	}

	/**
	 * The current data of this initializer.
	 */
	Data getData(RowProcessingState rowProcessingState);

	/**
	 * Step 0 - Callback for initializers before the first row is read.
	 * It is the responsibility of this initializer to recurse to the sub-initializers
	 * and register {@link InitializerData} for the initializer id via
	 * {@link RowProcessingState#setInitializerData(int, InitializerData)}.
	 * <p>
	 * This is useful for e.g. preparing initializers in case of a cache hit.
	 */
	void startLoading(RowProcessingState rowProcessingState);
		// by default - nothing to do

	/**
	 * Step 1.1 - Resolve the key value for this initializer for the current
	 * row and then recurse to the sub-initializers.
	 * <p>
	 * After this point, the initializer knows whether further processing is necessary
	 * for the current row i.e. if the object is missing.
	 */
	void resolveKey(Data data);

	default void resolveKey(RowProcessingState rowProcessingState) {
		resolveKey( getData( rowProcessingState ) );
	}

	/**
	 * Step 1.2 - Special variant of {@link #resolveKey(InitializerData)} that allows
	 * the reuse of key value and instance value from the previous row.
	 *
	 * @implSpec Defaults to simply delegating to {@link #resolveKey(InitializerData)}.
	 */
	default void resolveFromPreviousRow(Data data) {
		resolveKey( data );
	}

	default void resolveFromPreviousRow(RowProcessingState rowProcessingState) {
		resolveFromPreviousRow( getData( rowProcessingState ) );
	}

	/**
	 * Step 2.1 - Using the key resolved in {@link #resolveKey}, resolve the
	 * instance (of the thing initialized) to use for the current row.
	 * <p>
	 * After this point, the initializer knows the entity/collection/component
	 * instance for the current row based on the resolved key. If the resolving
	 * was successful, {@link #getResolvedInstance(RowProcessingState)} will
	 * return that instance.
	 */
	void resolveInstance(Data data);

	default void resolveInstance(RowProcessingState rowProcessingState) {
		resolveInstance( getData( rowProcessingState ) );
	}

	void resolveState(Data data);

	default void resolveState(RowProcessingState rowProcessingState) {
		resolveState( getData( rowProcessingState ) );
	}

	/**
	 * Step 2.2 - Use the given instance as resolved instance for this initializer.
	 * Initializers are supposed to recursively call this method for sub-initializers.
	 * <p>
	 * This alternative initialization protocol is used when a parent instance was
	 * already part of the persistence context.
	 */
	default void resolveInstance(@Nullable Object instance, Data data) {
		resolveKey( data );
	}

	default void resolveInstance(@Nullable Object instance, RowProcessingState rowProcessingState) {
		resolveInstance( instance, getData( rowProcessingState ) );
	}

	/**
	 * Step 3 - Initialize the state of the instance resolved in
	 * {@link #resolveInstance} from the current row values.
	 * <p>
	 * All resolved state for the current row is injected into the resolved
	 * instance
	 */
	void initializeInstance(Data data);

	default void initializeInstance(RowProcessingState rowProcessingState) {
		initializeInstance( getData( rowProcessingState ) );
	}

	/**
	 * Step 3.1 - Initialize the state of the instance as extracted from the given
	 * {@code parentInstance}. Extraction can be done with the {@link #getInitializedPart()}.
	 * Initializers are supposed to recursively call this method for sub-initializers.
	 * <p>
	 * This alternative initialization protocol is used for shallow query cache hits,
	 * in which case there is no data available in the
	 * {@link org.hibernate.sql.results.jdbc.internal.JdbcValuesCacheHit} to initialize
	 * potentially lazy associations.
	 */
	default void initializeInstanceFromParent(Object parentInstance, Data data) {
	}

	default void initializeInstanceFromParent(Object parentInstance, RowProcessingState rowProcessingState) {
		initializeInstanceFromParent( parentInstance, getData( rowProcessingState ) );
	}

	/**
	 * Lifecycle method called at the end of the current row processing.
	 * Provides ability to complete processing from the current row and
	 * prepare for the next row.
	 */
	void finishUpRow(Data data);

//	default void finishUpRow(RowProcessingState rowProcessingState) {
//		finishUpRow( getData( rowProcessingState ) );
//	}

	/**
	 * Lifecycle method called at the very end of the result values processing
	 */
	default void endLoading(Data data) {
		// by default - nothing to do
	}

//	default void endLoading(RowProcessingState rowProcessingState) {
//		final Data data = getData( rowProcessingState );
//		if ( data != null ) {
//			endLoading( data );
//		}
//	}

	/**
	 * Indicates whether this initializer is part of a key i.e. entity identifier,
	 * foreign key or collection key.
	 */
	boolean isPartOfKey();

	static boolean isPartOfKey(NavigablePath navigablePath, InitializerParent<?> parent) {
		return parent != null && parent.isEmbeddableInitializer() && parent.isPartOfKey()
			|| navigablePath instanceof EntityIdentifierNavigablePath
			|| ForeignKeyDescriptor.PART_NAME.equals( navigablePath.getLocalName() )
			|| ForeignKeyDescriptor.TARGET_PART_NAME.equals( navigablePath.getLocalName() );
	}

	/**
	 * Indicates whether calling resolve is needed when the object for this initializer is initialized already.
	 */
	boolean isEager();

	/**
	 * Indicates whether this initializer or one of its sub-parts could be made lazy.
	 */
	default boolean isLazyCapable() {
		// Usually, every model part for which an initializer exists is lazy capable
		// except for embeddable initializers with no sub-initializers
		return true;
	}

	/**
	 * Indicates whether this initializer has sub-initializers which are lazy.
	 */
	boolean hasLazySubInitializers();

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
	default @Nullable EntityInitializer<?> asEntityInitializer() {
		return null;
	}

	/**
	 * A utility method to avoid casting explicitly to EmbeddableInitializer
	 *
	 * @return EmbeddableInitializer if this is an instance of EmbeddableInitializer otherwise {@code null}
	 */
	default @Nullable EmbeddableInitializer<?> asEmbeddableInitializer() {
		return null;
	}

	/**
	 * A utility method to avoid casting explicitly to CollectionInitializer
	 *
	 * @return CollectionInitializer if this is an instance of CollectionInitializer otherwise {@code null}
	 */
	default @Nullable CollectionInitializer<?> asCollectionInitializer() {
		return null;
	}

	enum State {
		UNINITIALIZED,
		MISSING,
		KEY_RESOLVED,
		RESOLVED,
		INITIALIZED
	}
}

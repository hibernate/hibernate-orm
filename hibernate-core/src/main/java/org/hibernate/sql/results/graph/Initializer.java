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
	NavigablePath getNavigablePath();

	ModelPart getInitializedPart();

	Object getInitializedInstance();

	default void startLoading(RowProcessingState rowProcessingState) {
	}

	default void markShallowCached() {
	}

	/**
	 * Step 1 - Resolve the key value for this initializer for the current
	 * row.
	 *
	 * After this point, the initializer knows the entity/collection/component
	 * key for the current row
	 */
	void resolveKey(RowProcessingState rowProcessingState);

	/**
	 * Step 2 - Using the key resolved in {@link #resolveKey}, resolve the
	 * instance (of the thing initialized) to use for the current row.
	 *
	 * After this point, the initializer knows the entity/collection/component
	 * instance for the current row based on the resolved key
	 */
	void resolveInstance(RowProcessingState rowProcessingState);

	/**
	 * Step 3 - Initialize the state of the instance resolved in
	 * {@link #resolveInstance} from the current row values.
	 *
	 * All resolved state for the current row is injected into the resolved
	 * instance
	 */
	void initializeInstance(RowProcessingState rowProcessingState);

	/**
	 * Step 3.1 - Initialize the state of the instance as extracted from the given parentInstance.
	 * Extraction can be done with the {@link #getInitializedPart()}.
	 * Initializers are supposed to recursively call this method for sub-initializers.
	 *
	 * This alternative initialization protocol is used for shallow query cache hits,
	 * in which case there is no data available in the {@link org.hibernate.sql.results.jdbc.internal.JdbcValuesCacheHit}
	 * to initialize potentially lazy associations.
	 */
	default void initializeInstanceFromParent(Object parentInstance, RowProcessingState rowProcessingState) {
	}

	/**
	 * Lifecycle method called at the end of the current row processing.
	 * Provides ability to complete processing from the current row and
	 * prepare for the next row.
	 */
	void finishUpRow(RowProcessingState rowProcessingState);

	/**
	 * Lifecycle method called at the very end of the result values processing
	 */
	default void endLoading(ExecutionContext executionContext) {
		// by default - nothing to do
	}

	boolean isPartOfKey();

	static boolean isPartOfKey(NavigablePath navigablePath, FetchParentAccess parentAccess) {
		return parentAccess != null && parentAccess.isEmbeddableInitializer() && parentAccess.isPartOfKey()
				|| navigablePath instanceof EntityIdentifierNavigablePath
				|| ForeignKeyDescriptor.PART_NAME.equals( navigablePath.getLocalName() )
				|| ForeignKeyDescriptor.TARGET_PART_NAME.equals( navigablePath.getLocalName() );
	}

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

}

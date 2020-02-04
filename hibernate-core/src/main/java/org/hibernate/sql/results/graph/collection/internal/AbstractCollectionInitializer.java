/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.collection.internal;

import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.internal.log.LoggingHelper;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.sql.results.graph.collection.CollectionLoadingLogger;
import org.hibernate.sql.results.jdbc.spi.JdbcValuesSourceProcessingState;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.graph.collection.CollectionInitializer;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

/**
 * Base support for CollectionInitializer implementations
 *
 * @author Steve Ebersole
 */
public abstract class AbstractCollectionInitializer implements CollectionInitializer {
	private final NavigablePath collectionPath;
	private final PluralAttributeMapping collectionAttributeMapping;

	private final FetchParentAccess parentAccess;

	private final boolean selected;

	/**
	 * refers to the collection's container value - which collection-key?
	 */
	private final DomainResultAssembler keyContainerAssembler;

	/**
	 * refers to the rows entry in the collection.  null indicates that the collection is empty
	 */
	private final DomainResultAssembler keyCollectionAssembler;

	// per-row state
	private Object keyContainerValue;
	private Object keyCollectionValue;

	private CollectionKey collectionKey;


	@SuppressWarnings("WeakerAccess")
	protected AbstractCollectionInitializer(
			NavigablePath collectionPath,
			PluralAttributeMapping collectionAttributeMapping,
			FetchParentAccess parentAccess,
			boolean selected,
			DomainResultAssembler keyContainerAssembler,
			DomainResultAssembler keyCollectionAssembler) {
		this.collectionPath = collectionPath;
		this.collectionAttributeMapping = collectionAttributeMapping;
		this.parentAccess = parentAccess;
		this.selected = selected;
		this.keyContainerAssembler = keyContainerAssembler;
		this.keyCollectionAssembler = keyCollectionAssembler;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return collectionPath;
	}

	public PluralAttributeMapping getCollectionAttributeMapping() {
		return collectionAttributeMapping;
	}

	@Override
	public PluralAttributeMapping getInitializedPart() {
		return getCollectionAttributeMapping();
	}

	/**
	 * Are the values for performing this initialization present in the current
	 * {@link JdbcValuesSourceProcessingState}?
	 * Or should a separate/subsequent select be performed
	 *
	 * todo (6.0) : opportunity for performance gain by batching these selects triggered at the end of processing the JdbcValuesSource
	 */
	protected boolean isSelected() {
		return selected;
	}

	protected FetchParentAccess getParentAccess() {
		return parentAccess;
	}

	/**
	 * The value of the container/owner side of the collection key (FK).  Identifies the
	 * owner of the collection
	 */
	@SuppressWarnings("WeakerAccess")
	protected Object getKeyContainerValue() {
		return keyContainerValue;
	}

	/**
	 * The value of the collection side of the collection key (FK).  Identifies
	 * inclusion in the collection.  Can be null to indicate that the current row
	 * does not contain any collection values
	 */
	@SuppressWarnings("WeakerAccess")
	protected Object getKeyCollectionValue() {
		return keyCollectionValue;
	}

	@Override
	public CollectionKey resolveCollectionKey(RowProcessingState rowProcessingState) {
		resolveKey( rowProcessingState );
		return collectionKey;
	}

	@Override
	public void resolveKey(RowProcessingState rowProcessingState) {
		if ( collectionKey != null ) {
			// already resolved
			return;
		}

		final CollectionKey loadingKey = rowProcessingState.getCollectionKey();
		if ( loadingKey != null ) {
			collectionKey = loadingKey;
			return;
		}

		keyContainerValue = keyContainerAssembler.assemble(
				rowProcessingState,
				rowProcessingState.getJdbcValuesSourceProcessingState().getProcessingOptions()
		);

		if ( keyCollectionAssembler == null || keyContainerAssembler == keyCollectionAssembler ) {
			keyCollectionValue = keyContainerValue;
		}
		else {
			keyCollectionValue = keyCollectionAssembler.assemble(
					rowProcessingState,
					rowProcessingState.getJdbcValuesSourceProcessingState().getProcessingOptions()
			);
		}

		Object keyContainerValue = getKeyContainerValue();
		if ( keyContainerValue != null ) {
			this.collectionKey = new CollectionKey(
					collectionAttributeMapping.getCollectionDescriptor(),
					keyContainerValue
			);

			if ( CollectionLoadingLogger.DEBUG_ENABLED ) {
				CollectionLoadingLogger.INSTANCE.debugf(
						"(%s) Current row collection key : %s",
						StringHelper.collapse( this.getClass().getName() ),
						LoggingHelper.toLoggableString( getNavigablePath(), this.collectionKey.getKey() )
				);
			}
		}
	}

	@Override
	public void finishUpRow(RowProcessingState rowProcessingState) {
		keyContainerValue = null;
		keyCollectionValue = null;

		collectionKey = null;
	}
}

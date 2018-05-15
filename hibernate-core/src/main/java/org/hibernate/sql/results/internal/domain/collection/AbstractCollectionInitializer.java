/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.internal.domain.collection;

import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.domain.spi.PluralPersistentAttribute;
import org.hibernate.query.NavigablePath;
import org.hibernate.internal.log.LoggingHelper;
import org.hibernate.sql.results.spi.CollectionInitializer;
import org.hibernate.sql.results.spi.DomainResultAssembler;
import org.hibernate.sql.results.spi.FetchParentAccess;
import org.hibernate.sql.results.spi.RowProcessingState;

/**
 * Base support for CollectionInitializer implementations
 *
 * @author Steve Ebersole
 */
public abstract class AbstractCollectionInitializer implements CollectionInitializer {
	private final PersistentCollectionDescriptor collectionDescriptor;
	private final FetchParentAccess parentAccess;
	private final NavigablePath navigablePath;
	private final boolean selected;

	private final DomainResultAssembler keyTargetAssembler;
	private final DomainResultAssembler keyCollectionAssembler;

	// per-row state
	private Object keyContainerValue;
	private Object keyCollectionValue;

	private CollectionKey collectionKey;


	@SuppressWarnings("WeakerAccess")
	protected AbstractCollectionInitializer(
			PersistentCollectionDescriptor collectionDescriptor,
			FetchParentAccess parentAccess,
			NavigablePath navigablePath,
			boolean selected,
			DomainResultAssembler keyContainerAssembler,
			DomainResultAssembler keyCollectionAssembler) {
		this.collectionDescriptor = collectionDescriptor;
		this.parentAccess = parentAccess;
		this.navigablePath = navigablePath;
		this.selected = selected;
		this.keyTargetAssembler = keyContainerAssembler;
		this.keyCollectionAssembler = keyCollectionAssembler;
	}

	protected PersistentCollectionDescriptor getCollectionDescriptor() {
		return collectionDescriptor;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	/**
	 * Are the values for performing this initialization present in the current
	 * {@link org.hibernate.sql.results.spi.JdbcValuesSourceProcessingState}?
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
	 * owner of the collection and used to create the {@link CollectionKey} in
	 * {@link #resolveCollectionKey}
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
	public PluralPersistentAttribute getFetchedAttribute() {
		return getCollectionDescriptor().getDescribedAttribute();
	}

	@Override
	public void resolveKey(RowProcessingState rowProcessingState) {
		keyContainerValue = keyTargetAssembler.assemble(
				rowProcessingState,
				rowProcessingState.getJdbcValuesSourceProcessingState().getProcessingOptions()
		);

		if ( keyCollectionAssembler == null ) {
			keyCollectionValue = keyContainerValue;
		}
		else {
			keyCollectionValue = keyCollectionAssembler.assemble(
					rowProcessingState,
					rowProcessingState.getJdbcValuesSourceProcessingState().getProcessingOptions()
			);
		}

		collectionKey = new CollectionKey(
				getFetchedAttribute().getPersistentCollectionDescriptor(),
				getKeyContainerValue()
		);

		if ( CollectionLoadingLogger.DEBUG_ENABLED ) {
			CollectionLoadingLogger.INSTANCE.debugf(
					"(%s) Current row collection key : %s",
					StringHelper.collapse( this.getClass().getName() ),
					LoggingHelper.toLoggableString( getNavigablePath(), collectionKey.getKey() )
			);
		}
	}

	@SuppressWarnings("WeakerAccess")
	protected CollectionKey resolveCollectionKey(@SuppressWarnings("unused") RowProcessingState rowProcessingState) {
		return collectionKey;
	}

	@Override
	public void finishUpRow(RowProcessingState rowProcessingState) {
		keyContainerValue = null;
		keyCollectionValue = null;

		collectionKey = null;
	}
}

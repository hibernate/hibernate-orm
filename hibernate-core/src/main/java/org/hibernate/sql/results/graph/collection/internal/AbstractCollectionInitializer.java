/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.collection.internal;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.internal.log.LoggingHelper;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.spi.NavigablePath;
import org.hibernate.sql.results.graph.DomainResultAssembler;
import org.hibernate.sql.results.graph.FetchParentAccess;
import org.hibernate.sql.results.graph.collection.CollectionInitializer;
import org.hibernate.sql.results.graph.collection.CollectionLoadingLogger;
import org.hibernate.sql.results.graph.entity.EntityInitializer;
import org.hibernate.sql.results.jdbc.spi.RowProcessingState;

/**
 * Base support for CollectionInitializer implementations
 *
 * @author Steve Ebersole
 */
public abstract class AbstractCollectionInitializer implements CollectionInitializer {
	private final NavigablePath collectionPath;
	protected final PluralAttributeMapping collectionAttributeMapping;

	protected final FetchParentAccess parentAccess;

	/**
	 * refers to the collection's container value - which collection-key?
	 */
	protected final DomainResultAssembler<?> collectionKeyResultAssembler;

	protected PersistentCollection<?> collectionInstance;
	protected CollectionKey collectionKey;

	protected AbstractCollectionInitializer(
			NavigablePath collectionPath,
			PluralAttributeMapping collectionAttributeMapping,
			FetchParentAccess parentAccess,
			DomainResultAssembler<?> collectionKeyResultAssembler) {
		this.collectionPath = collectionPath;
		this.collectionAttributeMapping = collectionAttributeMapping;
		this.parentAccess = parentAccess;
		this.collectionKeyResultAssembler = collectionKeyResultAssembler;
	}

	@Override
	public void resolveKey(RowProcessingState rowProcessingState) {
		if ( collectionKey != null ) {
			// already resolved
			return;
		}

		if ( !isAttributeAssignableToConcreteDescriptor() ) {
			return;
		}

		// A null collection key result assembler means that we can use the parent key
		final Object collectionKeyValue;
		if ( collectionKeyResultAssembler == null ) {
			collectionKeyValue = parentAccess.getParentKey();
		}
		else {
			collectionKeyValue = collectionKeyResultAssembler.assemble( rowProcessingState );
		}

		if ( collectionKeyValue != null ) {
			this.collectionKey = new CollectionKey(
					collectionAttributeMapping.getCollectionDescriptor(),
					collectionKeyValue
			);

			if ( CollectionLoadingLogger.DEBUG_ENABLED ) {
				CollectionLoadingLogger.COLL_LOAD_LOGGER.debugf(
						"(%s) Current row collection key : %s",
						DelayedCollectionInitializer.class.getSimpleName(),
						LoggingHelper.toLoggableString( getNavigablePath(), this.collectionKey.getKey() )
				);
			}
		}
	}

	protected boolean isAttributeAssignableToConcreteDescriptor() {
		if ( parentAccess instanceof EntityInitializer ) {
			final AbstractEntityPersister concreteDescriptor = (AbstractEntityPersister) ( (EntityInitializer) parentAccess ).getConcreteDescriptor();
			if ( concreteDescriptor.isPolymorphic() ) {
				final AbstractEntityPersister declaringType = (AbstractEntityPersister) collectionAttributeMapping.getDeclaringType();
				if ( concreteDescriptor != declaringType ) {
					if ( !declaringType.getSubclassEntityNames().contains( concreteDescriptor.getName() ) ) {
						return false;
					}
				}
			}
		}
		return true;
	}

	@Override
	public PersistentCollection<?> getCollectionInstance() {
		return collectionInstance;
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

	protected FetchParentAccess getParentAccess() {
		return parentAccess;
	}

	@Override
	public CollectionKey resolveCollectionKey(RowProcessingState rowProcessingState) {
		resolveKey( rowProcessingState );
		return collectionKey;
	}

	@Override
	public void finishUpRow(RowProcessingState rowProcessingState) {
		collectionKey = null;
	}
}

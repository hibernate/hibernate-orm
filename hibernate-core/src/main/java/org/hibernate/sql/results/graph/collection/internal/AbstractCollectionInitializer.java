/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.graph.collection.internal;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.entity.AbstractEntityPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.query.NavigablePath;
import org.hibernate.sql.results.graph.collection.CollectionInitializer;
import org.hibernate.sql.results.graph.FetchParentAccess;
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

	protected PersistentCollection collectionInstance;
	protected CollectionKey collectionKey;

	@SuppressWarnings("WeakerAccess")
	protected AbstractCollectionInitializer(
			NavigablePath collectionPath,
			PluralAttributeMapping collectionAttributeMapping,
			FetchParentAccess parentAccess) {
		this.collectionPath = collectionPath;
		this.collectionAttributeMapping = collectionAttributeMapping;
		this.parentAccess = parentAccess;
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

		final Object parentKey = parentAccess.getParentKey();
		if ( parentKey != null ) {
			collectionKey = new CollectionKey(
					collectionAttributeMapping.getCollectionDescriptor(),
					parentKey
			);
		}
	}

	protected boolean isAttributeAssignableToConcreteDescriptor() {
		if ( parentAccess instanceof EntityInitializer ) {
			final AbstractEntityPersister concreteDescriptor = (AbstractEntityPersister) ( (EntityInitializer) parentAccess ).getConcreteDescriptor();
			if ( concreteDescriptor.isPolymorphic() ) {
				final AbstractEntityPersister declaringType = (AbstractEntityPersister) collectionAttributeMapping.getDeclaringType();
				if ( concreteDescriptor != declaringType ) {
					if ( !declaringType.getEntityMetamodel().getSubclassEntityNames().contains( concreteDescriptor.getEntityMetamodel().getName() ) ) {
						return false;
					}
				}
			}
		}
		return true;
	}

	@Override
	public PersistentCollection getCollectionInstance() {
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

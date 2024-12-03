/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.bytecode.enhance.spi.interceptor;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.LockMode;
import org.hibernate.bytecode.enhance.spi.CollectionTracker;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SelfDirtinessTracker;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.engine.spi.Status;
import org.hibernate.persister.entity.EntityPersister;

import static org.hibernate.engine.internal.ManagedTypeHelper.asSelfDirtinessTracker;
import static org.hibernate.engine.internal.ManagedTypeHelper.isSelfDirtinessTracker;

/**
 * Interceptor that loads attributes lazily
 *
 * @author Luis Barreiro
 * @author Steve Ebersole
 */
public class LazyAttributeLoadingInterceptor extends AbstractLazyLoadInterceptor {
	private final Object identifier;

	//N.B. this Set needs to be treated as immutable
	private Set<String> lazyFields;
	private Set<String> initializedLazyFields;
	private Set<String> mutableLazyFields;

	public LazyAttributeLoadingInterceptor(
			String entityName,
			Object identifier,
			Set<String> lazyFields,
			SharedSessionContractImplementor session) {
		super( entityName, session );
		this.identifier = identifier;
		//Important optimisation to not actually do a Map lookup for entities which don't have any lazy fields at all:
		this.lazyFields = org.hibernate.internal.util.collections.CollectionHelper.toSmallSet( lazyFields );
	}

	@Override
	public Object getIdentifier() {
		return identifier;
	}

	@Override
	protected Object handleRead(Object target, String attributeName, Object value) {
		if ( !isAttributeLoaded( attributeName ) ) {
			Object loadedValue = fetchAttribute( target, attributeName );
			attributeInitialized( attributeName );
			return loadedValue;
		}
		return value;
	}

	@Override
	protected Object handleWrite(Object target, String attributeName, Object oldValue, Object newValue) {
		if ( !isAttributeLoaded( attributeName ) ) {
			attributeInitialized( attributeName );
		}
		return newValue;
	}

	/**
	 * Fetches the lazy attribute. The attribute does not get associated with the entity. (To be used by hibernate methods)
	 */
	public Object fetchAttribute(final Object target, final String attributeName) {
		return loadAttribute( target, attributeName );
	}

	protected Object loadAttribute(final Object target, final String attributeName) {
		return EnhancementHelper.performWork(
				this,
				(session, isTemporarySession) -> {
					final EntityPersister persister = session.getFactory()
							.getMappingMetamodel()
							.getEntityDescriptor( getEntityName() );

					if ( isTemporarySession ) {
						final Object id = persister.getIdentifier( target, session );

						// Add an entry for this entity in the PC of the temp Session
						// NOTE : a few arguments that would be nice to pass along here...
						//		1) loadedState if we know any
						final Object[] loadedState = null;
						//		2) does a row exist in the db for this entity?
						final boolean existsInDb = true;
						session.getPersistenceContextInternal().addEntity(
								target,
								Status.READ_ONLY,
								loadedState,
								session.generateEntityKey( id, persister ),
								persister.getVersion( target ),
								LockMode.NONE,
								existsInDb,
								persister,
								true
						);
					}

					final LazyPropertyInitializer initializer = (LazyPropertyInitializer) persister;
					final Object loadedValue = initializer.initializeLazyProperty(
							attributeName,
							target,
							session
					);

					takeCollectionSizeSnapshot( target, attributeName, loadedValue );
					return loadedValue;
				},
				getEntityName(),
				attributeName
		);
	}

	public boolean isAttributeLoaded(String fieldName) {
		return !isLazyAttribute( fieldName ) || isInitializedLazyField( fieldName );
	}

	private boolean isLazyAttribute(String fieldName) {
		return lazyFields.contains( fieldName );
	}

	private boolean isInitializedLazyField(String fieldName) {
		return initializedLazyFields != null && initializedLazyFields.contains( fieldName );
	}

	public boolean hasAnyUninitializedAttributes() {
		if ( lazyFields.isEmpty() ) {
			return false;
		}

		if ( initializedLazyFields == null ) {
			return true;
		}

		for ( String fieldName : lazyFields ) {
			if ( !initializedLazyFields.contains( fieldName ) ) {
				return true;
			}
		}

		return false;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "(entityName=" + getEntityName() + " ,lazyFields=" + lazyFields + ')';
	}

	private void takeCollectionSizeSnapshot(Object target, String fieldName, Object value) {
		if ( value instanceof Collection && isSelfDirtinessTracker( target ) ) {
			// This must be called first, so that we remember that there is a collection out there,
			// even if we don't know its size (see below).
			final SelfDirtinessTracker targetSDT = asSelfDirtinessTracker( target );
			CollectionTracker tracker = targetSDT.$$_hibernate_getCollectionTracker();
			if ( tracker == null ) {
				targetSDT.$$_hibernate_clearDirtyAttributes();
				tracker = targetSDT.$$_hibernate_getCollectionTracker();
			}

			if ( value instanceof PersistentCollection && !( (PersistentCollection<?>) value ).wasInitialized() ) {
				// Cannot take a snapshot of an un-initialized collection.
				return;
			}
			tracker.add( fieldName, ( (Collection<?>) value ).size() );
		}
	}

	@Override
	public void attributeInitialized(String name) {
		if ( !isLazyAttribute( name ) ) {
			return;
		}
		if ( initializedLazyFields == null ) {
			initializedLazyFields = new HashSet<>();
		}
		initializedLazyFields.add( name );
	}

	@Override
	public Set<String> getInitializedLazyAttributeNames() {
		return initializedLazyFields == null ? Collections.emptySet() : initializedLazyFields;
	}

	public void addLazyFieldByGraph(String fieldName) {
		if ( mutableLazyFields == null ) {
			mutableLazyFields = new HashSet<>( lazyFields );
			lazyFields = mutableLazyFields;
		}
		mutableLazyFields.add( fieldName );
	}

	public void clearInitializedLazyFields() {
		initializedLazyFields = null;
	}
}

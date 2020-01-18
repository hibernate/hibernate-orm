/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import java.io.Serializable;

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.bytecode.enhance.spi.LazyPropertyInitializer;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.CollectionType;
import org.hibernate.type.CompositeType;
import org.hibernate.type.Type;

/**
 * Wrap collections in a Hibernate collection wrapper.
 *
 * @author Gavin King
 */
@SuppressWarnings("WeakerAccess")
public class WrapVisitor extends ProxyVisitor {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( WrapVisitor.class );
	private Object entity;
	private Serializable id;

	private boolean substitute;

	public WrapVisitor(Object entity, Serializable id, EventSource session) {
		super( session );
		this.entity = entity;
		this.id = id;
	}

	public boolean isSubstitutionRequired() {
		return substitute;
	}

	public WrapVisitor(EventSource session) {
		super( session );
	}

	@Override
	Object processCollection(Object collection, CollectionType collectionType)
			throws HibernateException {

		if ( collection == null ) {
			return null;
		}

		if ( collection == LazyPropertyInitializer.UNFETCHED_PROPERTY ) {
			return null;
		}

		if ( collection instanceof PersistentCollection ) {
			final PersistentCollection coll = (PersistentCollection) collection;
			final SessionImplementor session = getSession();

			if ( coll.setCurrentSession( session ) ) {
				reattachCollection( coll, collectionType );
			}

			return null;
		}

		return processArrayOrNewCollection( collection, collectionType );
	}

	final Object processArrayOrNewCollection(Object collection, CollectionType collectionType)
			throws HibernateException {

		final SessionImplementor session = getSession();

		if ( collection == null ) {
			//do nothing
			return null;
		}
		else {
			CollectionPersister persister = session.getFactory().getCollectionPersister( collectionType.getRole() );

			final PersistenceContext persistenceContext = session.getPersistenceContextInternal();
			//TODO: move into collection type, so we can use polymorphism!
			if ( collectionType.hasHolder() ) {

				if ( collection == CollectionType.UNFETCHED_COLLECTION ) {
					return null;
				}

				PersistentCollection ah = persistenceContext.getCollectionHolder( collection );
				if ( ah == null ) {
					ah = collectionType.wrap( session, collection );
					persistenceContext.addNewCollection( persister, ah );
					persistenceContext.addCollectionHolder( ah );
				}
				return null;
			}
			else {

				PersistentCollection persistentCollection = collectionType.wrap( session, collection );
				persistenceContext.addNewCollection( persister, persistentCollection );

				if ( LOG.isTraceEnabled() ) {
					LOG.tracev( "Wrapped collection in role: {0}", collectionType.getRole() );
				}

				return persistentCollection; //Force a substitution!

			}

		}

	}

	@Override
	void processValue(int i, Object[] values, Type[] types) {
		Object result = processValue( values[i], types[i] );
		if ( result != null ) {
			substitute = true;
			values[i] = result;
		}
	}

	@Override
	Object processComponent(Object component, CompositeType componentType) throws HibernateException {
		if ( component != null ) {
			Object[] values = componentType.getPropertyValues( component, getSession() );
			Type[] types = componentType.getSubtypes();
			boolean substituteComponent = false;
			for ( int i = 0; i < types.length; i++ ) {
				Object result = processValue( values[i], types[i] );
				if ( result != null ) {
					values[i] = result;
					substituteComponent = true;
				}
			}
			if ( substituteComponent ) {
				componentType.setPropertyValues( component, values, EntityMode.POJO );
			}
		}

		return null;
	}

	@Override
	public void process(Object object, EntityPersister persister) throws HibernateException {
		final Object[] values = persister.getPropertyValues( object );
		final Type[] types = persister.getPropertyTypes();
		processEntityPropertyValues( values, types );
		if ( isSubstitutionRequired() ) {
			persister.setPropertyValues( object, values );
		}
	}
}

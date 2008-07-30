/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 *
 */
package org.hibernate.event.def;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.engine.PersistenceContext;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.event.EventSource;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.AbstractComponentType;
import org.hibernate.type.CollectionType;
import org.hibernate.type.Type;

/**
 * Wrap collections in a Hibernate collection
 * wrapper.
 * @author Gavin King
 */
public class WrapVisitor extends ProxyVisitor {

	private static final Logger log = LoggerFactory.getLogger(WrapVisitor.class);

	boolean substitute = false;

	boolean isSubstitutionRequired() {
		return substitute;
	}

	WrapVisitor(EventSource session) {
		super(session);
	}

	Object processCollection(Object collection, CollectionType collectionType)
	throws HibernateException {

		if ( collection!=null && (collection instanceof PersistentCollection) ) {

			final SessionImplementor session = getSession();
			PersistentCollection coll = (PersistentCollection) collection;
			if ( coll.setCurrentSession(session) ) {
				reattachCollection( coll, collectionType );
			}
			return null;

		}
		else {
			return processArrayOrNewCollection(collection, collectionType);
		}

	}

	final Object processArrayOrNewCollection(Object collection, CollectionType collectionType)
	throws HibernateException {

		final SessionImplementor session = getSession();

		if (collection==null) {
			//do nothing
			return null;
		}
		else {
			CollectionPersister persister = session.getFactory().getCollectionPersister( collectionType.getRole() );

			final PersistenceContext persistenceContext = session.getPersistenceContext();
			//TODO: move into collection type, so we can use polymorphism!
			if ( collectionType.hasHolder( session.getEntityMode() ) ) {
				
				if (collection==CollectionType.UNFETCHED_COLLECTION) return null;

				PersistentCollection ah = persistenceContext.getCollectionHolder(collection);
				if (ah==null) {
					ah = collectionType.wrap(session, collection);
					persistenceContext.addNewCollection( persister, ah );
					persistenceContext.addCollectionHolder(ah);
				}
				return null;
			}
			else {

				PersistentCollection persistentCollection = collectionType.wrap(session, collection);
				persistenceContext.addNewCollection( persister, persistentCollection );

				if ( log.isTraceEnabled() ) log.trace( "Wrapped collection in role: " + collectionType.getRole() );

				return persistentCollection; //Force a substitution!

			}

		}

	}

	void processValue(int i, Object[] values, Type[] types) {
		Object result = processValue( values[i], types[i] );
		if (result!=null) {
			substitute = true;
			values[i] = result;
		}
	}

	Object processComponent(Object component, AbstractComponentType componentType)
	throws HibernateException {

		if (component!=null) {
			Object[] values = componentType.getPropertyValues( component, getSession() );
			Type[] types = componentType.getSubtypes();
			boolean substituteComponent = false;
			for ( int i=0; i<types.length; i++ ) {
				Object result = processValue( values[i], types[i] );
				if (result!=null) {
					values[i] = result;
					substituteComponent = true;
				}
			}
			if (substituteComponent) {
				componentType.setPropertyValues( component, values, getSession().getEntityMode() );
			}
		}

		return null;
	}

	void process(Object object, EntityPersister persister) throws HibernateException {
		EntityMode entityMode = getSession().getEntityMode();
		Object[] values = persister.getPropertyValues( object, entityMode );
		Type[] types = persister.getPropertyTypes();
		processEntityPropertyValues(values, types);
		if ( isSubstitutionRequired() ) {
			persister.setPropertyValues( object, values, entityMode );
		}
	}

}

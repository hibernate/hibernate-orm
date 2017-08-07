/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import org.hibernate.EntityMode;
import org.hibernate.HibernateException;
import org.hibernate.collection.spi.CollectionClassification;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.type.Type;

/**
 * Wrap collections in a Hibernate collection wrapper.
 *
 * @author Gavin King
 */
public class WrapVisitor extends ProxyVisitor {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( WrapVisitor.class );

	boolean substitute;

	boolean isSubstitutionRequired() {
		return substitute;
	}

	WrapVisitor(EventSource session) {
		super( session );
	}

	@Override
	Object processCollection(Object collection, PersistentCollectionDescriptor descriptor)
			throws HibernateException {

		if ( collection != null && ( collection instanceof PersistentCollection ) ) {

			final SessionImplementor session = getSession();
			PersistentCollection coll = (PersistentCollection) collection;
			if ( coll.setCurrentSession( session ) ) {
				reattachCollection( coll, descriptor.getNavigableRole() );
			}
			return null;

		}
		else {
			return processArrayOrNewCollection( collection, descriptor );
		}

	}

	final Object processArrayOrNewCollection(Object collection, PersistentCollectionDescriptor descriptor)
			throws HibernateException {

		final SessionImplementor session = getSession();

		if ( collection == null ) {
			//do nothing
			return null;
		}
		else {
			PersistentCollectionDescriptor persister = session.getFactory().getTypeConfiguration().findCollectionPersister( descriptor.getRole() );

			final PersistenceContext persistenceContext = session.getPersistenceContext();
			//TODO: move into collection type, so we can use polymorphism!
			if ( descriptor.getCollectionClassification() == CollectionClassification.ARRAY ) {

				if ( collection == PersistentCollectionDescriptor.UNFETCHED_COLLECTION ) {
					return null;
				}

				PersistentCollection ah = persistenceContext.getCollectionHolder( collection );
				if ( ah == null ) {
					ah = descriptor.getTuplizer().wrap( session, collection );
					persistenceContext.addNewCollection( persister, ah );
					persistenceContext.addCollectionHolder( ah );
				}
				return null;
			}
			else {

				PersistentCollection persistentCollection = descriptor.getTuplizer().wrap( session, collection );
				persistenceContext.addNewCollection( persister, persistentCollection );

				if ( LOG.isTraceEnabled() ) {
					LOG.tracev( "Wrapped collection in role: {0}", descriptor.getNavigableRole().getFullPath() );
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
	Object processComponent(Object component, EmbeddedType componentType) throws HibernateException {
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
	void process(Object object, EntityDescriptor persister) throws HibernateException {
		final Object[] values = persister.getPropertyValues( object );
		final Type[] types = persister.getPropertyTypes();
		processEntityPropertyValues( values, types );
		if ( isSubstitutionRequired() ) {
			persister.setPropertyValues( object, values );
		}
	}
}

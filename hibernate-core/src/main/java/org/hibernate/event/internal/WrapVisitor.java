/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.event.internal;

import java.util.List;

import org.hibernate.HibernateException;
import org.hibernate.collection.spi.CollectionClassification;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.PersistenceContext;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.event.spi.EventSource;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.metamodel.model.domain.spi.EmbeddedTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;
import org.hibernate.metamodel.model.domain.spi.PersistentAttributeDescriptor;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.domain.spi.PluralPersistentAttribute;

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
	Object processCollection(Object collection, PluralPersistentAttribute collectionAttribute)
			throws HibernateException {

		if ( collection != null && ( collection instanceof PersistentCollection ) ) {

			final SessionImplementor session = getSession();
			PersistentCollection coll = (PersistentCollection) collection;
			if ( coll.setCurrentSession( session ) ) {
				reattachCollection( coll, collectionAttribute.getNavigableRole() );
			}
			return null;

		}
		else {
			return processArrayOrNewCollection( collection, collectionAttribute.getCollectionDescriptor() );
		}

	}

	final Object processArrayOrNewCollection(Object collection, PersistentCollectionDescriptor collectionDescriptor)
			throws HibernateException {

		final SessionImplementor session = getSession();

		if ( collection == null ) {
			//do nothing
			return null;
		}
		else {
			final PersistenceContext persistenceContext = session.getPersistenceContext();
			//TODO: move into collection type, so we can use polymorphism!
			if ( collectionDescriptor.getCollectionClassification() == CollectionClassification.ARRAY ) {

				if ( collection == PersistentCollectionDescriptor.UNFETCHED_COLLECTION ) {
					return null;
				}

				PersistentCollection ah = persistenceContext.getCollectionHolder( collection );
				if ( ah == null ) {
					ah = collectionDescriptor.wrap( session, collection );
					persistenceContext.addNewCollection( collectionDescriptor, ah );
					persistenceContext.addCollectionHolder( ah );
				}
				return null;
			}
			else {
				PersistentCollection persistentCollection = collectionDescriptor.wrap( session, collection );
				persistenceContext.addNewCollection( collectionDescriptor, persistentCollection );

				if ( LOG.isTraceEnabled() ) {
					LOG.tracev( "Wrapped collection in role: {0}", collectionDescriptor.getNavigableRole().getFullPath() );
				}

				return persistentCollection; //Force a substitution!

			}

		}

	}

	@Override
	void processValue(int i, Object[] values, PersistentAttributeDescriptor attribute) throws HibernateException{
		Object result = processValue( values[i], attribute );
		if ( result != null ) {
			substitute = true;
			values[i] = result;
		}
	}

	@Override
	Object processComponent(Object component, EmbeddedTypeDescriptor descriptor) throws HibernateException {
		if ( component != null ) {
			Object[] values = descriptor.getPropertyValues( component );
			final List<PersistentAttributeDescriptor> persistentAttributes = descriptor.getPersistentAttributes();
			boolean substituteComponent = false;
			int i = 0;
			for( PersistentAttributeDescriptor attribute : persistentAttributes){
				Object result = processValue( values[i], attribute );
				if ( result != null ) {
					values[i] = result;
					substituteComponent = true;
				}
				i++;
			}
			if ( substituteComponent ) {
				descriptor.setPropertyValues( component, values );
			}
		}

		return null;
	}

	@Override
	void process(Object object, EntityTypeDescriptor descriptor) throws HibernateException {
		final Object[] values = descriptor.getPropertyValues( object );
		final List<PersistentAttributeDescriptor> persistentAttributes = descriptor.getPersistentAttributes();
		processEntityPropertyValues( values, persistentAttributes );
		if ( isSubstitutionRequired() ) {
			descriptor.setPropertyValues( object, values );
		}
	}
}

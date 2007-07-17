//$Id: CustomCollectionType.java 11496 2007-05-09 03:54:06Z steve.ebersole@jboss.com $
package org.hibernate.type;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.engine.SessionFactoryImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.usertype.UserCollectionType;
import org.hibernate.usertype.LoggableUserType;

/**
 * A custom type for mapping user-written classes that implement <tt>PersistentCollection</tt>
 * 
 * @see org.hibernate.collection.PersistentCollection
 * @see org.hibernate.usertype.UserCollectionType
 * @author Gavin King
 */
public class CustomCollectionType extends CollectionType {

	private final UserCollectionType userType;
	private final boolean customLogging;

	public CustomCollectionType(Class userTypeClass, String role, String foreignKeyPropertyName, boolean isEmbeddedInXML) {
		super(role, foreignKeyPropertyName, isEmbeddedInXML);

		if ( !UserCollectionType.class.isAssignableFrom( userTypeClass ) ) {
			throw new MappingException( "Custom type does not implement UserCollectionType: " + userTypeClass.getName() );
		}

		try {
			userType = ( UserCollectionType ) userTypeClass.newInstance();
		}
		catch ( InstantiationException ie ) {
			throw new MappingException( "Cannot instantiate custom type: " + userTypeClass.getName() );
		}
		catch ( IllegalAccessException iae ) {
			throw new MappingException( "IllegalAccessException trying to instantiate custom type: " + userTypeClass.getName() );
		}

		customLogging = LoggableUserType.class.isAssignableFrom( userTypeClass );
	}

	public PersistentCollection instantiate(SessionImplementor session, CollectionPersister persister, Serializable key)
	throws HibernateException {
		return userType.instantiate(session, persister);
	}

	public PersistentCollection wrap(SessionImplementor session, Object collection) {
		return userType.wrap(session, collection);
	}

	public Class getReturnedClass() {
		return userType.instantiate( -1 ).getClass();
	}

	public Object instantiate(int anticipatedType) {
		return userType.instantiate( anticipatedType );
	}

	public Iterator getElementsIterator(Object collection) {
		return userType.getElementsIterator(collection);
	}
	public boolean contains(Object collection, Object entity, SessionImplementor session) {
		return userType.contains(collection, entity);
	}
	public Object indexOf(Object collection, Object entity) {
		return userType.indexOf(collection, entity);
	}

	public Object replaceElements(Object original, Object target, Object owner, Map copyCache, SessionImplementor session)
	throws HibernateException {
		CollectionPersister cp = session.getFactory().getCollectionPersister( getRole() );
		return userType.replaceElements(original, target, cp, owner, copyCache, session);
	}

	protected String renderLoggableString(Object value, SessionFactoryImplementor factory) throws HibernateException {
		if ( customLogging ) {
			return ( ( LoggableUserType ) userType ).toLoggableString( value, factory );
		}
		else {
			return super.renderLoggableString( value, factory );
		}
	}

	public UserCollectionType getUserType() {
		return userType;
	}
}

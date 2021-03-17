/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.type.spi.TypeConfiguration;
import org.hibernate.usertype.LoggableUserType;
import org.hibernate.usertype.UserCollectionType;

/**
 * A custom type for mapping user-written classes that implement <tt>PersistentCollection</tt>
 *
 * @see org.hibernate.collection.spi.PersistentCollection
 * @see org.hibernate.usertype.UserCollectionType
 * @author Gavin King
 */
public class CustomCollectionType extends CollectionType {

	private final UserCollectionType userType;
	private final boolean customLogging;

	/**
	 * @deprecated Use the other constructor
	 */
	@Deprecated
	public CustomCollectionType(
			TypeFactory.TypeScope typeScope,
			Class userTypeClass,
			String role,
			String foreignKeyPropertyName) {
		this( userTypeClass, role, foreignKeyPropertyName );
	}

	public CustomCollectionType(
			Class userTypeClass,
			String role,
			String foreignKeyPropertyName) {
		super( role, foreignKeyPropertyName );
		userType = createUserCollectionType( userTypeClass );
		customLogging = LoggableUserType.class.isAssignableFrom( userTypeClass );
	}

	private static UserCollectionType createUserCollectionType(Class userTypeClass) {
		if ( !UserCollectionType.class.isAssignableFrom( userTypeClass ) ) {
			throw new MappingException( "Custom type does not implement UserCollectionType: " + userTypeClass.getName() );
		}

		try {
			return ( UserCollectionType ) userTypeClass.newInstance();
		}
		catch ( InstantiationException ie ) {
			throw new MappingException( "Cannot instantiate custom type: " + userTypeClass.getName() );
		}
		catch ( IllegalAccessException iae ) {
			throw new MappingException( "IllegalAccessException trying to instantiate custom type: " + userTypeClass.getName() );
		}
	}

	@Override
	public PersistentCollection instantiate(SharedSessionContractImplementor session, CollectionPersister persister, Serializable key)
	throws HibernateException {
		return userType.instantiate( session, persister );
	}

	@Override
	public PersistentCollection wrap(SharedSessionContractImplementor session, Object collection) {
		return userType.wrap( session, collection );
	}

	@Override
	public Class getReturnedClass() {
		return userType.instantiate( -1 ).getClass();
	}

	@Override
	public Object instantiate(int anticipatedType) {
		return userType.instantiate( anticipatedType );
	}

	@Override
	public Iterator getElementsIterator(Object collection) {
		return userType.getElementsIterator(collection);
	}

	@Override
	public boolean contains(Object collection, Object entity, SharedSessionContractImplementor session) {
		return userType.contains(collection, entity);
	}

	@Override
	public Object indexOf(Object collection, Object entity) {
		return userType.indexOf(collection, entity);
	}

	@Override
	public Object replaceElements(Object original, Object target, Object owner, Map copyCache, SharedSessionContractImplementor session)
	throws HibernateException {
		CollectionPersister cp = session.getFactory().getMetamodel().collectionPersister( getRole() );
		return userType.replaceElements(original, target, cp, owner, copyCache, session);
	}

	@Override
	protected String renderLoggableString(Object value, SessionFactoryImplementor factory) throws HibernateException {
		if ( customLogging ) {
			return ( (LoggableUserType) userType ).toLoggableString( value, factory );
		}
		else {
			return super.renderLoggableString( value, factory );
		}
	}

	public UserCollectionType getUserType() {
		return userType;
	}
}

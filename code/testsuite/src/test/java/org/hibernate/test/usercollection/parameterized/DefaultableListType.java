package org.hibernate.test.usercollection.parameterized;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.List;

import org.hibernate.usertype.UserCollectionType;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.collection.PersistentCollection;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.EntityMode;

/**
 * Our Hibernate type-system extension for defining our specialized collection
 * contract.
 *
 * @author Holger Brands
 * @author Steve Ebersole
 */
public class DefaultableListType implements UserCollectionType, ParameterizedType {
    private String defaultValue;

	public Object instantiate(int anticipatedSize) {
		DefaultableListImpl list = anticipatedSize < 0 ? new DefaultableListImpl() : new DefaultableListImpl( anticipatedSize );
		list.setDefaultValue( defaultValue );
		return list;
	}

	public PersistentCollection instantiate(
			SessionImplementor session,
			CollectionPersister persister) {
		return new PersistentDefaultableList( session );
	}

	public PersistentCollection wrap(SessionImplementor session, Object collection) {
		if ( session.getEntityMode() == EntityMode.DOM4J ) {
			throw new IllegalStateException( "dom4j not supported" );
		}
		else {
			return new PersistentDefaultableList( session, ( List ) collection );
		}
	}

	public Iterator getElementsIterator(Object collection) {
		return ( ( DefaultableList ) collection ).iterator();
	}

	public boolean contains(Object collection, Object entity) {
		return ( ( DefaultableList ) collection ).contains( entity );
	}

	public Object indexOf(Object collection, Object entity) {
		int index = ( ( DefaultableList ) collection ).indexOf( entity );
		return index >= 0 ? new Integer( index ) : null;
	}

	public Object replaceElements(
			Object original,
			Object target,
			CollectionPersister persister,
			Object owner,
			Map copyCache,
			SessionImplementor session) {
		DefaultableList result = ( DefaultableList ) target;
		result.clear();
		result.addAll( ( DefaultableList ) original );
		return result;
	}

	public void setParameterValues(Properties parameters) {
        defaultValue = parameters.getProperty( "default" );
	}
}

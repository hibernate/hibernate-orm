/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.collection.custom.parameterized;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserCollectionType;

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

	@Override
	public PersistentCollection instantiate(
			SharedSessionContractImplementor session,
			CollectionPersister persister) {
		return new PersistentDefaultableList( session );
	}

	@Override
	public PersistentCollection wrap(SharedSessionContractImplementor session, Object collection) {
		return new PersistentDefaultableList( session, ( List ) collection );
	}

	@Override
	public Iterator getElementsIterator(Object collection) {
		return ( ( DefaultableList ) collection ).iterator();
	}

	@Override
	public boolean contains(Object collection, Object entity) {
		return ( ( DefaultableList ) collection ).contains( entity );
	}

	@Override
	public Object indexOf(Object collection, Object entity) {
		int index = ( ( DefaultableList ) collection ).indexOf( entity );
		return index >= 0 ? new Integer( index ) : null;
	}

	@Override
	public Object replaceElements(
			Object original,
			Object target,
			CollectionPersister persister,
			Object owner,
			Map copyCache,
			SharedSessionContractImplementor session) {
		DefaultableList result = ( DefaultableList ) target;
		result.clear();
		result.addAll( ( DefaultableList ) original );
		return result;
	}

	@Override
	public void setParameterValues(Properties parameters) {
        defaultValue = parameters.getProperty( "default" );
	}
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type.internal;

import java.io.Serializable;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.hibernate.collection.internal.PersistentMap;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionMetadata;

/**
 * @author Andrea Boriero
 */
public class MapType extends AbstractCollectionType {
	public MapType(String roleName) {
		super( roleName );
	}

	public MapType(String roleName, Comparator comparator) {
		super( roleName, comparator );
	}

	@Override
	public PersistentCollection instantiate(
			SharedSessionContractImplementor session, PersistentCollectionMetadata persister, Serializable key) {
		return new PersistentMap( session );
	}

	@Override
	public Object instantiate(int anticipatedSize) {
		return anticipatedSize <= 0
				? new HashMap()
				: new HashMap( anticipatedSize + (int) ( anticipatedSize * .75f ), .75f );
	}

	@Override
	public PersistentCollection wrap(SharedSessionContractImplementor session, Object collection) {
		return new PersistentMap( session, (java.util.Map) collection );
	}

	@Override
	public Iterator getElementsIterator(Object collection) {
		return ( (java.util.Map) collection ).values().iterator();
	}

	@Override
	protected Object replaceElements(
			Object original, Object target, Object owner, Map copyCache, SharedSessionContractImplementor session) {
		final PersistentCollectionMetadata cp = getCollectionPersister();

		java.util.Map result = (java.util.Map) target;
		result.clear();

		for ( Object o : ( (Map) original ).entrySet() ) {
			Map.Entry me = (Map.Entry) o;
			Object key = cp.getIndexType().replace( me.getKey(), null, session, owner, copyCache );
			Object value = cp.getElementReference().getOrmType().replace(
					me.getValue(),
					null,
					session,
					owner,
					copyCache
			);
			result.put( key, value );
		}

		return result;
	}

	@Override
	public Object indexOf(Object collection, Object element) {
		for ( Object o : ( (Map) collection ).entrySet() ) {
			Map.Entry me = (Map.Entry) o;
			//TODO: proxies!
			if ( me.getValue() == element ) {
				return me.getKey();
			}
		}
		return null;
	}

	@Override
	public Class getReturnedClass() {
		return Map.class;
	}
}

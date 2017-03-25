/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.collections.type;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.hibernate.HibernateException;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.usertype.UserCollectionType;

//tag::collections-custom-collection-mapping-example[]
public class QueueType implements UserCollectionType {

    @Override
    public PersistentCollection instantiate(
            SharedSessionContractImplementor session,
            CollectionPersister persister) throws HibernateException {
        return new PersistentQueue( session );
    }

    @Override
    public PersistentCollection wrap(
            SharedSessionContractImplementor session,
            Object collection) {
        return new PersistentQueue( session, (List) collection );
    }

    @Override
    public Iterator getElementsIterator(Object collection) {
        return ( (Queue) collection ).iterator();
    }

    @Override
    public boolean contains(Object collection, Object entity) {
        return ( (Queue) collection ).contains( entity );
    }

    @Override
    public Object indexOf(Object collection, Object entity) {
        int i = ( (List) collection ).indexOf( entity );
        return ( i < 0 ) ? null : i;
    }

    @Override
    public Object replaceElements(
            Object original,
            Object target,
            CollectionPersister persister,
            Object owner,
            Map copyCache,
            SharedSessionContractImplementor session)
            throws HibernateException {
        Queue result = (Queue) target;
        result.clear();
        result.addAll( (Queue) original );
        return result;
    }

    @Override
    public Object instantiate(int anticipatedSize) {
        return new LinkedList<>();
    }

}
//end::collections-custom-collection-mapping-example[]

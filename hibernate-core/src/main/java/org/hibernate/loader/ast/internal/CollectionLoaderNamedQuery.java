/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.ast.internal;

import org.hibernate.FlushMode;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.CollectionKey;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.ast.spi.CollectionLoader;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.query.QueryTypeMismatchException;
import org.hibernate.query.named.NamedQueryMemento;
import org.hibernate.query.spi.QueryImplementor;

import jakarta.persistence.Parameter;

import java.util.List;

/**
 * @author Steve Ebersole
 */
public class CollectionLoaderNamedQuery implements CollectionLoader {
	private final CollectionPersister persister;
	private final NamedQueryMemento namedQueryMemento;

	public CollectionLoaderNamedQuery(CollectionPersister persister, NamedQueryMemento namedQueryMemento) {
		this.persister = persister;
		this.namedQueryMemento = namedQueryMemento;
	}

	@Override
	public PluralAttributeMapping getLoadable() {
		return persister.getAttributeMapping();
	}

	@Override
	public PersistentCollection<?> load(Object key, SharedSessionContractImplementor session) {
		final QueryImplementor<?> query = namedQueryMemento.toQuery( session );
		//noinspection unchecked
		query.setParameter( (Parameter<Object>) query.getParameters().iterator().next(), key );
		query.setHibernateFlushMode( FlushMode.MANUAL );
		final List<?> resultList = query.getResultList();
		// TODO: we need a good way to inspect the query itself to see what it returns
		if ( !resultList.isEmpty() && resultList.get(0) instanceof PersistentCollection<?> ) {
			// in hbm.xml files we have the <load-collection/> element
			return (PersistentCollection<?>) resultList.get(0);
		}
		else {
			// using annotations we have no way to specify a @CollectionResult
			final CollectionKey collectionKey = new CollectionKey( persister, key );
			final PersistentCollection<?> collection =
					session.getPersistenceContextInternal()
							.getCollection( collectionKey );
			for ( Object element : resultList ) {
				if ( element != null
						&& !persister.getElementType().getReturnedClass().isInstance( element ) ) {
					throw new QueryTypeMismatchException( "Collection loader for '" + persister.getRole()
							+ "' returned an instance of '" + element.getClass().getName() + "'" );
				}
			}
			collection.beforeInitialize( persister, resultList.size() );
			collection.injectLoadedState( getLoadable(), resultList );
			collection.afterInitialize();
			session.getPersistenceContextInternal()
					.getCollectionEntry( collection )
					.postInitialize( collection, session );
			return collection;
		}
	}
}

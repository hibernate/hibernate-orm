/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.ast.internal;

import org.hibernate.FlushMode;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.ast.spi.CollectionLoader;
import org.hibernate.metamodel.mapping.PluralAttributeMapping;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.query.named.NamedQueryMemento;
import org.hibernate.query.spi.QueryImplementor;

import jakarta.persistence.Parameter;

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
		final QueryImplementor<PersistentCollection<?>> query = namedQueryMemento.toQuery( session );
		//noinspection unchecked
		query.setParameter( (Parameter<Object>) query.getParameters().iterator().next(), key );
		query.setHibernateFlushMode( FlushMode.MANUAL );
		return query.getResultList().get( 0 );
	}
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.internal;

import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.spi.SingleIdEntityLoader;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.query.named.NamedQueryProducer;
import org.hibernate.query.named.NamedQueryRepository;
import org.hibernate.query.spi.QueryImplementor;
import org.hibernate.query.sql.spi.NamedNativeQueryMemento;

/**
 * Implementation of SingleIdEntityLoader for cases where the application has
 * provided the select load query
 *
 * @author Steve Ebersole
 */
public class SingleIdEntityLoaderProvidedQueryImpl<T> implements SingleIdEntityLoader<T> {
	private final EntityMappingType entityDescriptor;
	private final NamedQueryProducer namedQueryMemento;

	public SingleIdEntityLoaderProvidedQueryImpl(
			EntityMappingType entityDescriptor,
			String loadQueryName,
			SessionFactoryImplementor sessionFactory) {
		this.entityDescriptor = entityDescriptor;

		this.namedQueryMemento = resolveNamedQuery( loadQueryName, sessionFactory );
		if ( namedQueryMemento == null ) {
			throw new IllegalArgumentException( "Could not resolve named load-query [" + entityDescriptor.getEntityName() + "] : " + loadQueryName );
		}
	}

	private static NamedQueryProducer resolveNamedQuery(
			String queryName,
			SessionFactoryImplementor sf) {
		final NamedQueryRepository namedQueryRepository = sf.getQueryEngine().getNamedQueryRepository();

		final NamedNativeQueryMemento nativeQueryMemento = namedQueryRepository.getNativeQueryMemento( queryName );
		if ( nativeQueryMemento != null ) {
			return nativeQueryMemento;
		}

		return namedQueryRepository.getHqlQueryMemento( queryName );
	}

	@Override
	public EntityMappingType getLoadable() {
		return entityDescriptor;
	}

	@Override
	public T load(Object pkValue, LockOptions lockOptions, SharedSessionContractImplementor session) {
		//noinspection unchecked
		final QueryImplementor<T> query = namedQueryMemento.toQuery(
				session,
				entityDescriptor.getMappedJavaTypeDescriptor().getJavaType()
		);

		query.setParameter( 0, pkValue );

		return query.uniqueResult();
	}

	@Override
	public Object[] loadDatabaseSnapshot(Object id, SharedSessionContractImplementor session) {
		return new Object[0];
	}
}

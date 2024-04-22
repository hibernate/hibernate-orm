/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.ast.internal;

import org.hibernate.FlushMode;
import org.hibernate.LockOptions;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.loader.ast.spi.SingleIdEntityLoader;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.query.named.NamedQueryMemento;
import org.hibernate.query.spi.QueryImplementor;

import jakarta.persistence.Parameter;

/**
 * Implementation of SingleIdEntityLoader for cases where the application has
 * provided the select load query
 *
 * @author Steve Ebersole
 */
public class SingleIdEntityLoaderProvidedQueryImpl<T> implements SingleIdEntityLoader<T> {
	private final EntityMappingType entityDescriptor;
	private final NamedQueryMemento<?> namedQueryMemento;

	public SingleIdEntityLoaderProvidedQueryImpl(
			EntityMappingType entityDescriptor,
			NamedQueryMemento<?> namedQueryMemento) {
		this.entityDescriptor = entityDescriptor;
		this.namedQueryMemento = namedQueryMemento;
	}

	@Override
	public EntityMappingType getLoadable() {
		return entityDescriptor;
	}

	@Override
	public T load(Object pkValue, LockOptions lockOptions, Boolean readOnly, SharedSessionContractImplementor session) {
		//noinspection unchecked
		final QueryImplementor<T> query = namedQueryMemento.toQuery(
				session,
				(Class<T>) entityDescriptor.getMappedJavaType().getJavaTypeClass()
		);

		//noinspection unchecked
		query.setParameter( (Parameter<Object>) query.getParameters().iterator().next(), pkValue );
		query.setHibernateFlushMode( FlushMode.MANUAL );

		return query.uniqueResult();
	}

	@Override
	public T load(
			Object pkValue,
			Object entityInstance,
			LockOptions lockOptions,
			Boolean readOnly,
			SharedSessionContractImplementor session) {
		if ( entityInstance != null ) {
			throw new UnsupportedOperationException("null entity instance");
		}
		return load( pkValue, lockOptions, readOnly, session );
	}

	@Override
	public Object[] loadDatabaseSnapshot(Object id, SharedSessionContractImplementor session) {
		return ArrayHelper.EMPTY_OBJECT_ARRAY;
	}
}

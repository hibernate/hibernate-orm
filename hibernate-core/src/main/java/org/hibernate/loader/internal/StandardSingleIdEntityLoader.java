/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.loader.internal;

import java.io.Serializable;

import org.hibernate.LockOptions;
import org.hibernate.engine.spi.LoadQueryInfluencers;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.loader.spi.SingleIdEntityLoader;
import org.hibernate.metamodel.model.domain.spi.EntityDescriptor;
import org.hibernate.NotYetImplementedFor6Exception;

/**
 * @author Steve Ebersole
 */
public class StandardSingleIdEntityLoader<T> implements SingleIdEntityLoader<T> {
	private final EntityDescriptor<T> entityDescriptor;

	public StandardSingleIdEntityLoader(
			EntityDescriptor<T> entityDescriptor,
			LockOptions lockOptions,
			LoadQueryInfluencers loadQueryInfluencers) {
		this.entityDescriptor = entityDescriptor;
	}

	@Override
	public T load(Serializable id, LockOptions lockOptions, SharedSessionContractImplementor session) {
		throw new NotYetImplementedFor6Exception(  );
	}
}

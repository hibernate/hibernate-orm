/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal.collection;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;

/**
 * @author Steve Ebersole
 */
public class OneToManyRemovalExecutor implements CollectionRemovalExecutor {
	public OneToManyRemovalExecutor(
			PersistentCollectionDescriptor collectionDescriptor,
			SessionFactoryImplementor sessionFactory) {
		throw new NotYetImplementedFor6Exception();
	}

	@Override
	public void remove(Object key, SharedSessionContractImplementor session) {

	}
}

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.metamodel.model.domain.internal.collection;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.collection.spi.PersistentCollection;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;
import org.hibernate.metamodel.model.relational.spi.Table;

/**
 * @author Chris Cranford
 */
public class OneToManyRowsDeletionExecutor implements CollectionRowsDeletionExecutor {
	public OneToManyRowsDeletionExecutor(
			PersistentCollectionDescriptor collectionDescriptor,
			SessionFactoryImplementor sessionFactory,
			Table dmlTargetTable,
			boolean deleteByIndex) {
	}

	@Override
	public void execute(
			PersistentCollection collection, Object key, SharedSessionContractImplementor session) {
		throw new NotYetImplementedFor6Exception( getClass() );
	}
}

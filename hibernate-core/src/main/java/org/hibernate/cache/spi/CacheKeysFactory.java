/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.spi;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.spi.EntityHierarchy;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionDescriptor;

/**
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
public interface CacheKeysFactory {
	Object createEntityKey(
			Object id,
			EntityHierarchy entityHierarchy,
			SessionFactoryImplementor factory,
			String tenantIdentifier);

	Object createNaturalIdKey(
			Object[] naturalIdValues,
			EntityHierarchy entityHierarchy,
			SharedSessionContractImplementor session);

	Object createCollectionKey(
			Object id,
			PersistentCollectionDescriptor descriptor,
			SessionFactoryImplementor factory,
			String tenantIdentifier);

	Object getEntityId(Object cacheKey);

	Object[] getNaturalIdValues(Object cacheKey);

	Object getCollectionId(Object cacheKey);
}

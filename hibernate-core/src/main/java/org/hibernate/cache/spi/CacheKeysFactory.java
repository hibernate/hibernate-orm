/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.cache.spi;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.metamodel.model.domain.spi.PersistentCollectionMetadata;
import org.hibernate.metamodel.model.domain.spi.EntityTypeImplementor;

/**
 * @author Radim Vansa &lt;rvansa@redhat.com&gt;
 */
public interface CacheKeysFactory {
	Object createCollectionKey(Object id, PersistentCollectionMetadata persister, SessionFactoryImplementor factory, String tenantIdentifier);

	Object createEntityKey(Object id, EntityTypeImplementor persister, SessionFactoryImplementor factory, String tenantIdentifier);

	Object createNaturalIdKey(Object[] naturalIdValues, EntityTypeImplementor persister, SharedSessionContractImplementor session);

	Object getEntityId(Object cacheKey);

	Object getCollectionId(Object cacheKey);

	Object[] getNaturalIdValues(Object cacheKey);
}

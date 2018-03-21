/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.persister;

import org.hibernate.HibernateException;
import org.hibernate.cache.spi.access.EntityDataAccess;
import org.hibernate.cache.spi.access.NaturalIdDataAccess;
import org.hibernate.mapping.PersistentClass;
import org.hibernate.persister.entity.SingleTableEntityPersister;
import org.hibernate.persister.spi.PersisterCreationContext;

/**
 * @author Shawn Clowater
 */
//tag::entity-persister-mapping[]

public class EntityPersister
    extends SingleTableEntityPersister {

    public EntityPersister(
            PersistentClass persistentClass,
            EntityDataAccess cache,
            NaturalIdDataAccess naturalIdRegionAccessStrategy,
            PersisterCreationContext creationContext)
            throws HibernateException {
        super( persistentClass, cache, naturalIdRegionAccessStrategy, creationContext );
    }
}
//end::entity-persister-mapping[]


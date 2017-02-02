/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.spi;

import org.hibernate.persister.entity.EntityPersister;

/**
 * Models a QuerySpace specific to an entity (EntityPersister).
 * <p/>
 * It's {@link #getDisposition()} result will be {@link Disposition#ENTITY}
 *
 * @author Steve Ebersole
 */
public interface EntityQuerySpace extends QuerySpace {
	 /**
	 * Retrieve the EntityPersister that this QuerySpace refers to.
	  *
	 * @return The entity persister
	 */
	public EntityPersister getEntityPersister();
}

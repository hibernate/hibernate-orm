/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.queryable.spi;

import org.hibernate.persister.common.spi.NavigableSource;
import org.hibernate.persister.entity.spi.EntityPersister;

/**
 * Common contract for any Navigable whose type is an entity.
 *
 * @author Steve Ebersole
 */
public interface EntityValuedExpressableType<T> extends ExpressableType<T>, NavigableSource<T> {

	// todo (6.0) : should this extend NavigableSource rather than just Navigable?
	// 		or should this just specialize ExpressableType?

	EntityPersister<T> getEntityPersister();

	String getTypeName();
	String getEntityName();
	String getJpaEntityName();

	default PersistenceType getPersistenceType() {
		return PersistenceType.ENTITY;
	}
}

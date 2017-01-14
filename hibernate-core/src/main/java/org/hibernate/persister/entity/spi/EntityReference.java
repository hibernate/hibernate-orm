/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.entity.spi;

import org.hibernate.persister.common.spi.Navigable;
import org.hibernate.sqm.domain.SqmExpressableTypeEntity;

/**
 * @author Steve Ebersole
 */
public interface EntityReference<T> extends Navigable<T>, SqmExpressableTypeEntity {
	EntityPersister<T> getEntityPersister();

	String getTypeName();
	String getEntityName();
	String getJpaEntityName();
}

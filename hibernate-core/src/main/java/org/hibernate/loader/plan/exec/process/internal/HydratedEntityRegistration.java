/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.exec.process.internal;

import org.hibernate.engine.spi.EntityKey;
import org.hibernate.loader.plan.spi.EntityReference;

/**
 * @author Steve Ebersole
 */
public class HydratedEntityRegistration {
	private final EntityReference entityReference;
	private final EntityKey key;
	private Object instance;

	HydratedEntityRegistration(EntityReference entityReference, EntityKey key, Object instance) {
		this.entityReference = entityReference;
		this.key = key;
		this.instance = instance;
	}

	public EntityReference getEntityReference() {
		return entityReference;
	}

	public EntityKey getKey() {
		return key;
	}

	public Object getInstance() {
		return instance;
	}
}

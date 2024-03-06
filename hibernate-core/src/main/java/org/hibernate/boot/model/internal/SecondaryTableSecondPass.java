/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.model.internal;

import java.util.Map;

import org.hibernate.MappingException;
import org.hibernate.boot.spi.SecondPass;
import org.hibernate.mapping.PersistentClass;

/**
 * @author Emmanuel Bernard
 */
public class SecondaryTableSecondPass implements SecondPass {
	private final EntityBinder entityBinder;
	private final PropertyHolder propertyHolder;

	public SecondaryTableSecondPass(EntityBinder entityBinder, PropertyHolder propertyHolder) {
		this.entityBinder = entityBinder;
		this.propertyHolder = propertyHolder;
	}

	@Override
	public void doSecondPass(Map<String, PersistentClass> persistentClasses) throws MappingException {
		entityBinder.finalSecondaryTableBinding( propertyHolder );
	}
}

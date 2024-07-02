/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.jaxb.hbm.transform;

import java.util.Map;

import org.hibernate.mapping.PersistentClass;
import org.hibernate.mapping.Table;

/**
 * @author Steve Ebersole
 */
public class EntityTypeInfo extends ManagedTypeInfo {
	private final PersistentClass persistentClass;

	public EntityTypeInfo(
			Table table,
			PersistentClass persistentClass) {
		super( table );
		this.persistentClass = persistentClass;
	}

	public PersistentClass getPersistentClass() {
		return persistentClass;
	}
}

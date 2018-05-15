/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.results.spi;

import java.util.Locale;

import org.hibernate.engine.spi.EntityKey;
import org.hibernate.metamodel.model.domain.spi.EntityTypeDescriptor;

/**
 * Representation of an entity being loaded, containing its state
 *
 * @author Steve Ebersole
 */
public class LoadingEntityEntry {
	private final EntityInitializer entityInitializer;
	private final EntityKey entityKey;
	private final EntityTypeDescriptor descriptor;
	private final Object entityInstance;

	public LoadingEntityEntry(
			EntityInitializer entityInitializer,
			EntityKey entityKey,
			EntityTypeDescriptor descriptor,
			Object entityInstance) {
		this.entityInitializer = entityInitializer;
		this.entityKey = entityKey;
		this.descriptor = descriptor;
		this.entityInstance = entityInstance;
	}

	public EntityInitializer getEntityInitializer() {
		return entityInitializer;
	}

	public EntityKey getEntityKey() {
		return entityKey;
	}

	public EntityTypeDescriptor getDescriptor() {
		return descriptor;
	}

	public Object getEntityInstance() {
		return entityInstance;
	}

	@Override
	public String toString() {
		return String.format(
				Locale.ROOT,
				"LoadingEntityEntry(type=%s, id=%s)@%s",
				getDescriptor().getEntityName(),
				getEntityKey().getIdentifier(),
				System.identityHashCode( this )
		);
	}
}

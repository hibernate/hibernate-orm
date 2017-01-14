/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.persister.entity.spi;

import org.hibernate.mapping.PersistentClass;
import org.hibernate.orm.persister.common.spi.ManagedTypeImplementor;
import org.hibernate.orm.persister.spi.PersisterCreationContext;

/**
 * @author Steve Ebersole
 */
public interface IdentifiableTypeImplementor<T> extends ManagedTypeImplementor<T>, javax.persistence.metamodel.IdentifiableType<T> {
	@Override
	default IdentifiableTypeImplementor<? super T> getSupertype() {
		return getSuperType();
	}

	@Override
	IdentifiableTypeImplementor<? super T> getSuperType();

	EntityHierarchy getHierarchy();

	/**
	 * Called after all EntityPersister instance have been created and (initially) initialized.
	 *
	 * @param superType The entity's super's EntityPersister
	 * @param mappingDescriptor Should be  the same PersistentClass instance originally passed to the
	 * 		ctor, but we want to not have to store that around as EntityPersister instance state -
	 * 		so we pass it in again
	 * @param creationContext Access to the database model
	 */
	void finishInitialization(
			EntityHierarchy entityHierarchy,
			IdentifiableTypeImplementor<? super T> superType,
			PersistentClass mappingDescriptor,
			PersisterCreationContext creationContext);
}

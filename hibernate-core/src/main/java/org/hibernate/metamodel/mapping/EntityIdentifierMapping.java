/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.engine.spi.IdentifierValue;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.persister.entity.EntityPersister;


/**
 * Describes the mapping of an entity's identifier.
 *
 * @see jakarta.persistence.Id
 * @see jakarta.persistence.EmbeddedId
 */
public interface EntityIdentifierMapping extends ValueMapping, ModelPart {
	String ROLE_LOCAL_NAME = "{id}";

	@Override
	default String getPartName() {
		return ROLE_LOCAL_NAME;
	}

	/**
	 * The strategy for distinguishing between detached and transient
	 * state based on the identifier mapping
	 */
	IdentifierValue getUnsavedStrategy();

	/**
	 *
	 *
	 * @return the entity identifier value
	 *
	 * @deprecated Use {@link #getIdentifier(Object)}
	 */
	@Deprecated
	Object getIdentifier(Object entity, SharedSessionContractImplementor session);

	Object getIdentifier(Object entity);

	/**
	 * @deprecated use {@link #setIdentifier(Object, Object, EntityPersister, SharedSessionContractImplementor)} instead.
	 */
	@Deprecated
	void setIdentifier(Object entity, Object id, SharedSessionContractImplementor session);

	default void setIdentifier(Object entity, Object id, EntityPersister entityDescriptor, SharedSessionContractImplementor session){
		setIdentifier( entity, id, session );
	}

	Object instantiate();
}

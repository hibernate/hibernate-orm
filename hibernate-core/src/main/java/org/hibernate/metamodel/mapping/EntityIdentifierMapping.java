/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import java.io.Serializable;

import org.hibernate.TransientObjectException;
import org.hibernate.engine.internal.ForeignKeys;
import org.hibernate.engine.spi.IdentifierValue;
import org.hibernate.engine.spi.SharedSessionContractImplementor;


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
	 * Return the identifier of the persistent or transient object, or throw
	 * an exception if the instance is "unsaved"
	 * <p/>
	 * Used by OneToOneType and ManyToOneType to determine what id value should
	 * be used for an object that may or may not be associated with the session.
	 * This does a "best guess" using any/all info available to use (not just the
	 * EntityEntry).
	 *
	 * @param entity The entity instance
	 * @param session The session
	 *
	 * @return The identifier
	 *
	 * @throws TransientObjectException if the entity is transient (does not yet have an identifier)
	 * @see org.hibernate.engine.internal.ForeignKeys#getEntityIdentifierIfNotUnsaved(String, Object, SharedSessionContractImplementor)
	 * @since 6.1.1
	 */
	default Object getIdentifierIfNotUnsaved(Object entity, SharedSessionContractImplementor session) {
		if ( entity == null ) {
			return null;
		}
		else if ( session == null ) {
			// If we have no session available, just return the identifier
			return getIdentifier( entity );
		}
		Object id = session.getContextEntityIdentifier( entity );
		if ( id == null ) {
			// context-entity-identifier returns null explicitly if the entity
			// is not associated with the persistence context; so make some
			// deeper checks...
			final String entityName = findContainingEntityMapping().getEntityName();
			if ( ForeignKeys.isTransient( entityName, entity, Boolean.FALSE, session ) ) {
				throw new TransientObjectException(
						"object references an unsaved transient instance - save the transient instance before flushing: " +
								(entityName == null ? session.guessEntityName( entity ) : entityName)
				);
			}
			id = getIdentifier( entity );
		}
		return id;
	}

	void setIdentifier(Object entity, Object id, SharedSessionContractImplementor session);

	Object instantiate();
}

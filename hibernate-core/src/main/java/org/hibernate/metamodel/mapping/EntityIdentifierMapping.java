/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.TransientObjectException;
import org.hibernate.engine.internal.ForeignKeys;
import org.hibernate.engine.spi.IdentifierValue;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.spi.MergeContext;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;


/**
 * Describes the mapping of an entity's identifier.
 *
 * @see Id
 * @see EmbeddedId
 * @see Nature
 */
public interface EntityIdentifierMapping extends ValuedModelPart {

	String ID_ROLE_NAME = "{id}";
	String LEGACY_ID_NAME = "id";

	static boolean matchesRoleName(String name) {
		return LEGACY_ID_NAME.equalsIgnoreCase( name )
			|| ID_ROLE_NAME.equals( name );
	}

	@Override
	default String getPartName() {
		return ID_ROLE_NAME;
	}

	/**
	 * @see Nature
	 */
	Nature getNature();

	/**
	 * The name of the attribute defining the id, if one
	 */
	String getAttributeName();

	/**
	 * The strategy for distinguishing between detached and transient
	 * state based on the identifier mapping
	 *
	 * @see EntityVersionMapping#getUnsavedStrategy()
	 */
	IdentifierValue getUnsavedStrategy();

	/**
	 * Instantiate an instance of the identifier.
	 *
	 * @apiNote This is really only valid on {@linkplain CompositeIdentifierMapping composite identifiers}
	 */
	Object instantiate();

	/**
	 * Extract the identifier from an instance of the entity
	 */
	Object getIdentifier(Object entity);

	/**
	 * Extract the identifier from an instance of the entity
	 *
	 * It's supposed to be use during the merging process
	 */
	default Object getIdentifier(Object entity, MergeContext mergeContext){
		return getIdentifier( entity );
	}

	/**
	 * Return the identifier of the persistent or transient object, or throw
	 * an exception if the instance is "unsaved"
	 * <p>
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

	/**
	 * Inject an identifier value into an instance of the entity
	 */
	void setIdentifier(Object entity, Object id, SharedSessionContractImplementor session);

	/**
	 * The style of identifier used.
	 */
	enum Nature {
		/**
		 * Simple, single-column identifier.
		 *
		 * @see Id
		 * @see BasicEntityIdentifierMapping
		 */
		SIMPLE,

		/**
		 * An "aggregated" composite identifier, which is another way to say that the
		 * identifier is represented as an {@linkplain EmbeddedId embeddable}.
		 *
		 * @see EmbeddedId
		 * @see AggregatedIdentifierMapping
		 */
		COMPOSITE,

		/**
		 * Composite identifier defined with multiple {@link Id}
		 * mappings.  Often used in conjunction with an {@link IdClass}
		 *
		 * @see Id
		 * @see IdClass
		 * @see NonAggregatedIdentifierMapping
		 */
		VIRTUAL
	}

	@Override
	default boolean isEntityIdentifierMapping() {
		return true;
	}
}

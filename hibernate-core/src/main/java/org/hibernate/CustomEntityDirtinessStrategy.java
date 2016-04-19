/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate;

import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.type.Type;

/**
 * During a flush cycle, Hibernate needs to determine which of the entities associated with a {@link Session}.
 * Dirty entities are the ones that get {@literal UPDATE}ed to the database.
 * <p/>
 * In some circumstances, that process of determining whether an entity is dirty can take a significant time as
 * by default Hibernate must check each of the entity's attribute values one-by-one.  Oftentimes applications
 * already have knowledge of an entity's dirtiness and using that information instead would be more performant.
 * The purpose of this contract then is to allow applications such a plug-in point.
 *
 * @author Steve Ebersole
 */
public interface CustomEntityDirtinessStrategy {
	/**
	 * Is this strategy capable of telling whether the given entity is dirty?  A return of {@code true} means that
	 * {@link #isDirty} will be called next as the definitive means to determine whether the entity is dirty.
	 *
	 * @param entity The entity to be check.
	 * @param persister The persister corresponding to the given entity
	 * @param session The session from which this check originates.
	 *
	 * @return {@code true} indicates the dirty check can be done; {@code false} indicates it cannot.
	 */
	public boolean canDirtyCheck(Object entity, EntityPersister persister, Session session);

	/**
	 * The callback used by Hibernate to determine if the given entity is dirty.  Only called if the previous
	 * {@link #canDirtyCheck} returned {@code true}
	 *
	 * @param entity The entity to check.
	 * @param persister The persister corresponding to the given entity
	 * @param session The session from which this check originates.
	 *
	 * @return {@code true} indicates the entity is dirty; {@link false} indicates the entity is not dirty.
	 */
	public boolean isDirty(Object entity, EntityPersister persister, Session session);

	/**
	 * Callback used by Hibernate to signal that the entity dirty flag should be cleared.  Generally this
	 * happens afterQuery previous dirty changes were written to the database.
	 *
	 * @param entity The entity to reset
	 * @param persister The persister corresponding to the given entity
	 * @param session The session from which this call originates.
	 */
	public void resetDirty(Object entity, EntityPersister persister, Session session);

	/**
	 * Callback used to hook into Hibernate algorithm for determination of which attributes have changed.  Applications
	 * wanting to hook in to this would call back into the given {@link DirtyCheckContext#doDirtyChecking}
	 * method passing along an appropriate {@link AttributeChecker} implementation.
	 *
	 * @param entity The entity being checked
	 * @param persister The persister corresponding to the given entity
	 * @param session The session from which this call originates.
	 * @param dirtyCheckContext The callback context
	 */
	public void findDirty(Object entity, EntityPersister persister, Session session, DirtyCheckContext dirtyCheckContext);

	/**
	 * A callback to drive dirty checking.  Handed to the {@link CustomEntityDirtinessStrategy#findDirty} method
	 * so that it can callback on to it if it wants to handle dirty checking rather than using Hibernate's default
	 * checking
	 *
	 * @see CustomEntityDirtinessStrategy#findDirty
	 */
	public static interface DirtyCheckContext {
		/**
		 * The callback to indicate that dirty checking (the dirty attribute determination phase) should be handled
		 * by the calling {@link CustomEntityDirtinessStrategy} using the given {@link AttributeChecker}.
		 *
		 * @param attributeChecker The delegate usable by the context for determining which attributes are dirty.
		 */
		public void doDirtyChecking(AttributeChecker attributeChecker);
	}

	/**
	 * Responsible for identifying when attributes are dirty.
	 */
	public static interface AttributeChecker {
		/**
		 * Do the attribute dirty check.
		 *
		 * @param attributeInformation Information about the attribute which is useful to help determine if it is
		 * dirty.
		 *
		 * @return {@code true} indicates the attribute value has changed; {@code false} indicates it has not.
		 */
		public boolean isDirty(AttributeInformation attributeInformation);
	}

	/**
	 * Provides {@link AttributeChecker} with meta information about the attributes being checked.
	 */
	@SuppressWarnings( {"UnusedDeclaration"})
	public static interface AttributeInformation {
		/**
		 * Get a reference to the persister for the entity containing this attribute.
		 *
		 * @return The entity persister.
		 */
		public EntityPersister getContainingPersister();

		/**
		 * Many of Hibernate internals use arrays to define information about attributes.  This value
		 * provides this index into those arrays for this particular attribute.
		 * <p/>
		 * It can be useful if needing to leverage those Hibernate internals.
		 *
		 * @return The attribute index.
		 */
		public int getAttributeIndex();

		/**
		 * Get the name of this attribute.
		 *
		 * @return The attribute name
		 */
		public String getName();

		/**
		 * Get the mapping type of this attribute.
		 *
		 * @return The mapping type.
		 */
		public Type getType();

		/**
		 * Get the current value of this attribute.
		 *
		 * @return The attributes current value
		 */
		public Object getCurrentValue();

		/**
		 * Get the loaded value of this attribute.
		 * <p/>
		 * <b>NOTE : A call to this method may require hitting the database in cases where the loaded state is
		 * not known.  In those cases the db hit is incurred only once per entity, not for each attribute.</b>
		 *
		 * @return The attributes loaded value
		 */
		public Object getLoadedValue();
	}


}

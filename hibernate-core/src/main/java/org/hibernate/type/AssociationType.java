/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type;

import org.hibernate.MappingException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.persister.entity.Joinable;

/**
 * A type that represents some kind of association between entities.
 * @see org.hibernate.engine.internal.Cascade
 * @author Gavin King
 */
public interface AssociationType extends Type {

	/**
	 * Get the foreign key directionality of this association
	 */
	ForeignKeyDirection getForeignKeyDirection();

	//TODO: move these to a new JoinableType abstract class,
	//extended by EntityType and PersistentCollectionType:

	/**
	 * Is the primary key of the owning entity table
	 * to be used in the join?
	 */
	boolean useLHSPrimaryKey();
	/**
	 * Get the name of a property in the owning entity
	 * that provides the join key (null if the identifier)
	 */
	String getLHSPropertyName();

	/**
	 * The name of a unique property of the associated entity
	 * that provides the join key (null if the identifier of
	 * an entity, or key of a collection)
	 */
	String getRHSUniqueKeyPropertyName();

	/**
	 * Get the "persister" for this association - a class or
	 * collection persister
	 */
	Joinable getAssociatedJoinable(SessionFactoryImplementor factory) throws MappingException;

	/**
	 * Get the entity name of the associated entity
	 */
	String getAssociatedEntityName(SessionFactoryImplementor factory) throws MappingException;

	/**
	 * Do we dirty check this association, even when there are
	 * no columns to be updated?
	 */
	boolean isAlwaysDirtyChecked();
}

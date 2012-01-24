/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2012, Red Hat Inc. or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Inc.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate;

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
	 * @param session The session from which this check originates.
	 *
	 * @return {@code true} indicates the dirty check can be done; {@code false} indicates it cannot.
	 */
	public boolean canDirtyCheck(Object entity, Session session);

	/**
	 * The callback used by Hibernate to determine if the given entity is dirty.  Only called if the previous
	 * {@link #canDirtyCheck} returned {@code true}
	 *
	 * @param entity The entity to check.
	 * @param session The session from which this check originates.
	 *
	 * @return {@code true} indicates the entity is dirty; {@link false} indicates the entity is not dirty.
	 */
	public boolean isDirty(Object entity, Session session);

	/**
	 * Callback used by Hibernate to signal that the entity dirty flag should be cleared.  Generally this
	 * happens after previous dirty changes were written to the database.
	 *
	 * @param entity The entity to reset
	 * @param session The session from which this call originates.
	 */
	public void resetDirty(Object entity, Session session);
}

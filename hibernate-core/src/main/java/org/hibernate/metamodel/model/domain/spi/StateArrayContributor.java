/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.util.Map;

import org.hibernate.NotYetImplementedFor6Exception;
import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.type.ForeignKeyDirection;
import org.hibernate.type.descriptor.java.MutabilityPlan;

/**
 * Optional contract for a Navigable that can be part of it's container's
 * state array.
 *
 * @author Steve Ebersole
 */
public interface StateArrayContributor<J> extends Navigable<J>, ExpressableType<J> {
	/**
	 * Defines this contributor's position within the state array.
	 *
	 * @apiNote Much like {@link PersistentAttribute#getAttributePosition()}, this
	 * position follows a pre-defined algorithm based on alphabetical order, super
	 * types first.  Note however, that this ordering is only important internally
	 * as Hibernate builds the container's "state array" as part of EntityEntry,
	 * actions, cache entries, etc
	 */
	default int getStateArrayPosition() {
		throw new NotYetImplementedFor6Exception();
	}

	default void setStateArrayPosition(int position) {
		throw new NotYetImplementedFor6Exception();
	}

	/**
	 * Is this value nullable?
	 */
	boolean isNullable();

	/**
	 * Is this value used in SQL INSERT statements?
	 */
	boolean isInsertable();

	/**
	 * Is this value used in SQL UPDATE statements?
	 */
	boolean isUpdatable();

	/**
	 * Should this Navigable be dirty checked to determine whether its
	 * container is dirty?
	 */
	boolean isIncludedInDirtyChecking();

	/**
	 * For {@link OptimisticLockType#DIRTY} or {@link OptimisticLockType#ALL} style
	 * optimistic locking, should this Navigable be part of its container's optimistic
	 * locking?
	 */
	boolean isIncludedInOptimisticLocking();

	MutabilityPlan<J> getMutabilityPlan();

	@Override
	default Class<J> getJavaType() {
		return getJavaTypeDescriptor().getJavaType();
	}

	// todo (6.0) : "hydrate" relies on SqlSelection and jdbc value processing uses those to build the hydrated values itself.
	//		seems like this contract should define a `hydrateState` method, but implementing
	//		such a thing means passing in the SqlSelection(s) need to access the state.
	//
	//		however, such a solution is the only real way to account for collections, e.g., which
	//		are such a contributor but which return a "special" marker value on hydrate
	//
	// something like:

	/**
	 * @apiNote The incoming `jdbcValues` might be a single object or an array of objects
	 * depending on whether this navigable/contributor reported one or more SqlSelections.
	 * The return follows the same rules.  For a composite-value, an `Object[]` would be returned
	 * representing the composite's "simple state".  For entity-value, the return would
	 * be its id's "simple state" : again a single `Object` for simple ids, an array for
	 * composite ids.  All others return a single value.
	 *
	 * todo (6.0) : this may not be true for ANY mappings - verify
	 * 		- those may return the (id,discriminator) tuple
	 */
	default Object hydrate(Object jdbcValues, SharedSessionContractImplementor session) {
		throw new NotYetImplementedFor6Exception();
	}

	/**
	 * Given a hydrated representation of this navigable/contributor, resolve its
	 * domain representation.
	 * <p>
	 * E.g. for a composite, the hydrated form is an Object[] representing the
	 * "simple state" of the composite's attributes.  Resolution of those values
	 * returns the instance of the component with its resolved values injected.
	 *
	 * @apiNote
	 */
	default Object resolveHydratedState(
			Object hydratedForm,
			SharedSessionContractImplementor session,
			Object containerInstance) {
		throw new NotYetImplementedFor6Exception();
	}

	default Object replace(
			Object originalValue,
			Object targetValue,
			Object owner,
			Map copyCache,
			SessionImplementor session) {
		throw new NotYetImplementedFor6Exception();
	}

	default Object replace(
			Object originalValue,
			Object targetValue,
			Object owner,
			Map copyCache,
			ForeignKeyDirection foreignKeyDirection,
			SessionImplementor session) {
		throw new NotYetImplementedFor6Exception();
	}
}

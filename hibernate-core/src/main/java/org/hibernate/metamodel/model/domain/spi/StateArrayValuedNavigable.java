/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import org.hibernate.annotations.OptimisticLockType;
import org.hibernate.sql.ast.produce.metamodel.spi.ExpressableType;
import org.hibernate.type.descriptor.java.MutabilityPlan;

/**
 * Optional contract for a Navigable that can be part of it's container's
 * state array.
 *
 * @author Steve Ebersole
 */
public interface StateArrayValuedNavigable<J> extends Navigable<J>, ExpressableType<J> {
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
}

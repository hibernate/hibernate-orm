/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import org.hibernate.metamodel.model.domain.DomainType;

/**
 * Binding of the discriminator in a entity hierarchy
 *
 * @author Steve Ebersole
 * @author Hardy Ferentschik
 */
public interface DiscriminatorDescriptor<J>
		extends VirtualNavigable<J>, BasicValuedNavigable<J>, DomainTypeExposer<J> {

	String NAVIGABLE_NAME = "{type}";

	/**
	 * The mappings for `entity-name` <--> `discriminator-value` for this
	 * hierarchy's discriminator.
	 */
	DiscriminatorMappings getDiscriminatorMappings();

	@Override
	default PersistenceType getPersistenceType() {
		return PersistenceType.BASIC;
	}

	@Override
	default DomainType<J> getDomainType() {
		return this;
	}

	@Override
	default void visitNavigable(NavigableVisitationStrategy visitor) {
		visitor.visitDiscriminator( this );
	}

//	@Override
//	default boolean isNullable() {
//		// if there is a discriminator, {@code null} is not a valid value
//		return false;
//	}
//
//	@Override
//	default boolean isInsertable() {
//		// if there is a discriminator, it must be insertable
//		return true;
//	}
//
//	@Override
//	default boolean isUpdatable() {
//		// if there is a discriminator, it will not be updatable
//		return false;
//	}
//
//	@Override
//	default boolean isIncludedInDirtyChecking() {
//		return false;
//	}
//
//	@Override
//	default boolean isIncludedInOptimisticLocking() {
//		return false;
//	}
//
//	@Override
//	default MutabilityPlan<J> getMutabilityPlan() {
//		return ImmutableMutabilityPlan.INSTANCE;
//	}

	default boolean matchesNavigableName(J navigableName) {
		return NAVIGABLE_NAME.equals( navigableName );
	}
}


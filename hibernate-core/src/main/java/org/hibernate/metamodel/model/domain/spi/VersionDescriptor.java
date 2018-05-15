/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.spi;

import java.lang.reflect.Member;

import org.hibernate.type.descriptor.java.MutabilityPlan;

/**
 * Information about the entity (hierarchy wide) version
 *
 * @author Steve Ebersole
 */
public interface VersionDescriptor<O,J>
		extends SingularPersistentAttribute<O,J>, BasicValuedNavigable<J>, NonIdPersistentAttribute<O,J> {
	/**
	 * Access to the value that indicates an unsaved (transient) entity
	 *
	 * @return The unsaved value
	 */
	String getUnsavedValue();

	@Override
	default void visitNavigable(NavigableVisitationStrategy visitor) {
		visitor.visitVersion( this );
	}

	VersionSupport getVersionSupport();

	@Override
	default Disposition getDisposition() {
		return Disposition.VERSION;
	}

	@Override
	default boolean isOptional() {
		return false;
	}

	@Override
	default boolean isNullable() {
		return false;
	}

	@Override
	default boolean isInsertable() {
		// todo (6.0) : need to look at whether there is a value generator
		return true;
	}

	@Override
	default boolean isUpdatable() {
		// todo (6.0) : need to look at whether there is a value generator
		return false;
	}

	@Override
	default boolean isIncludedInDirtyChecking() {
		return false;
	}

	@Override
	default boolean isIncludedInOptimisticLocking() {
		return false;
	}

	@Override
	default MutabilityPlan<J> getMutabilityPlan() {
		return getJavaTypeDescriptor().getMutabilityPlan();
	}

	@Override
	default PersistenceType getPersistenceType() {
		return PersistenceType.BASIC;
	}

	@Override
	default SimpleTypeDescriptor<J> getType() {
		return getBasicType();
	}

	@Override
	default Class<J> getJavaType() {
		return getJavaTypeDescriptor().getJavaType();
	}

	@Override
	default Member getJavaMember() {
		return getPropertyAccess().getGetter().getMember();
	}


}

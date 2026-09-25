package org.hibernate.metamodel.mapping;

import jakarta.annotation.Nonnull;

import org.hibernate.cascade.spi.CascadeStyle;
import org.hibernate.cascade.spi.CascadeStyles;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.type.descriptor.java.MutabilityPlan;

/**
 * @author Steve Ebersole
 */
public interface AttributeMetadata {
	@Nonnull
	PropertyAccess getPropertyAccess();

	@Nonnull
	MutabilityPlan<?> getMutabilityPlan();

	boolean isNullable();

	boolean isInsertable();

	boolean isUpdatable();

	boolean isSelectable();

	boolean isIncludedInDirtyChecking();

	boolean isIncludedInOptimisticLocking();

	@Nonnull
	default CascadeStyle getCascadeStyle() {
		// todo (6.0) - implement in each subclass.
		//		For now return a default NONE value for all contributors since this isn't
		//		to be supported as a part of Alpha1.
		return CascadeStyles.NONE;
	}
}

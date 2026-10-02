package org.hibernate.metamodel.mapping;

/**
 * Marker interface for parts of the application domain model that do not actually
 * exist in the model classes.
 *
 * @see #isVirtual()
 *
 * @author Steve Ebersole
 */
public interface VirtualModelPart extends ModelPart {
	@Override
	default boolean isVirtual() {
		return true;
	}
}

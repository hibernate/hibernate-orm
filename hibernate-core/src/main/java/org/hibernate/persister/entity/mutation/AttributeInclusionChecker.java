package org.hibernate.persister.entity.mutation;

import jakarta.annotation.Nonnull;

import org.hibernate.metamodel.mapping.SingularAttributeMapping;

/**
 * @author Steve Ebersole
 */
@FunctionalInterface
@org.hibernate.SPI({ org.hibernate.SPI.Role.USE, org.hibernate.SPI.Role.IMPLEMENT })
public interface AttributeInclusionChecker {
	boolean include(int position, @Nonnull SingularAttributeMapping attribute);
}

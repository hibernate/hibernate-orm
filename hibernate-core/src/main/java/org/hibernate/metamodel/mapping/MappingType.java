package org.hibernate.metamodel.mapping;

import jakarta.annotation.Nonnull;

import org.hibernate.type.descriptor.java.JavaType;

/**
 * Common descriptor for types in the mapping model - entities, embeddables, String, Integer, etc
 *
 * @author Steve Ebersole
 */
@org.hibernate.SPI({ org.hibernate.SPI.Role.USE, org.hibernate.SPI.Role.IMPLEMENT })
public interface MappingType {
	/**
	 * The {@linkplain JavaType descriptor} descriptor for the mapped Java type
	 */
	@Nonnull
	JavaType<?> getMappedJavaType();
}

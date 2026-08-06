/*
 * SPDX-License-Identifier: Apache-2.0
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.type.descriptor.java;

import java.util.UUID;

import org.hibernate.Incubating;

/**
 * Specialization of {@link BasicJavaType} for types which model a
 * representation of a {@link UUID}.
 *
 * @param <T> the represented Java type
 *
 * @since 8.1
 * @author Steve Ebersole
 */
@Incubating
public interface UuidCapableJavaType<T> extends BasicJavaType<T> {
	/**
	 * The transformer used to convert generated UUID values to and from the
	 * represented Java type.
	 */
	ValueTransformer<T> getUuidValueTransformer();

	/**
	 * Whether {@link jakarta.persistence.GenerationType#AUTO AUTO} generation
	 * should infer UUID generation for this Java type.
	 */
	boolean prefersUuidGeneration();

	/**
	 * Converts values to and from Java's {@link UUID} representation.
	 */
	interface ValueTransformer<T> {
		T transform(UUID uuid);

		UUID parse(T value);
	}
}
